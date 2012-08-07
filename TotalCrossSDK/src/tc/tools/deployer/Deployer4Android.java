/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2012 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/

package tc.tools.deployer;

import totalcross.util.*;
import tc.tools.converter.bb.*;
import tc.tools.converter.bb.attribute.Code;
import tc.tools.converter.bb.attribute.LocalVariableTable;
import tc.tools.converter.bb.attribute.SourceFile;
import tc.tools.converter.bb.constant.Integer;
import tc.tools.converter.bb.constant.NameAndType;
import tc.tools.converter.bb.constant.UTF8;
import tc.tools.converter.bb.constant.Class;

import java.io.*;
import java.util.zip.*;

/*
    A launcher for Android is placed in a Android PacKage (APK), which is a 
    zip file, with the following contents:
    
       + META-INF
       |    MANIFEST.MF
       |    CERT.RSA
       |    CERT.SF
       + res
       |    drawable-hdpi
       |       icon.png
       | assets 
       |    tcfiles.zip
       | resources.arsc
       | classes.dex
       | AndroidManifest.xml
    
    The res + resources.arsc + AndroidManifest.xml are stored inside a file named resources.ap_
    (created with APPT) 
    
    Then it adds the classes.dex and signs the package, creating the APK file.
    
    The AndroidManifest.xml is a compiled version of the original AndroidManifest.xml file. 
    Its format is (all numbers in little endian):
       pos 4: file size
       pos 12: pos to � - 8
       pos 36: list of positions to the strings, starting from 0 
          each string is prefixed with its size and ends with 0 (chars in unicode)
     
     
    The resources.arsc has the package name located at pos 156, prefixed with 
    the total length of the name (127 bytes) - which is NOT the length of the package.
    There is a free space to place the package there, so there's no need to shift the file.
     
     
    Since Android requires that data stored in res/raw must be all lower-case, we have to create
    a zip and store the TCZ (and also files from android.pkg) inside that zip. When the program
    first runs, it unpacks that zip in the /data/data/class_package folder. Note that there's no
    way to delete the zip inside tha APK after the installation.
    
    
    Also: all TotalCross programs must be signed with the same key, otherwise the vm and litebase 
    will not be able to read data from the program's folder.
    
    
    To create the launcher for the application, we follow these steps:
    
    1. Edit the files: R.class  Stub.class  R$attr.class  R$drawable.class  R$raw.class  and replace
    the package and Stub names with totalcross.app.MainClass (where MainClass is the class that extends
    MainWindow)
    
    2. Run the dx (dalvik converter) to create a classes.dex file
    
    3. Zip the contents into tcfiles.zip
    
    4. Update the resources as explained above.
    
    5. Create the APK (jar file) with everything
    
    6. Sign the APK.
 */

public class Deployer4Android
{
   private static final boolean DEBUG = false;
   private static String targetDir, sourcePackage, targetPackage, targetTCZ, jarOut, fileName;
   private String tcFolder = null, lbFolder = null;
   private boolean isDemo,singleApk;

   byte[] buf = new byte[8192];
   
   public Deployer4Android() throws Exception
   {
      targetDir = DeploySettings.targetDir+"android/";
      fileName = DeploySettings.filePrefix;
      if (fileName.indexOf(' ') >= 0) // apk with spaces give errors, so we remove the spaces
         fileName = fileName.replace(" ","");
      // create the output folder
      File f = new File(targetDir);
      if (!f.exists())
         f.mkdirs();
      singleApk = DeploySettings.packageType != 0;
      if (!singleApk)
      {
         targetPackage = "totalcross/app/"+fileName.toLowerCase();
         if (!DeploySettings.quiet)
            System.out.println("Android application folder: /data/data/"+(targetPackage.replace('/','.')));
      }
      else
      {
         isDemo = (DeploySettings.packageType & DeploySettings.PACKAGE_DEMO) != 0;
         tcFolder = (isDemo ? DeploySettings.folderTotalCrossSDKDistVM : DeploySettings.folderTotalCrossVMSDistVM)+"android/";
         if ((DeploySettings.packageType & DeploySettings.PACKAGE_LITEBASE) != 0)
            lbFolder = (isDemo ? DeploySettings.folderLitebaseSDKDistLIB : DeploySettings.folderLitebaseVMSDistLIB) + "android/";
         // source and target packages must have the exact length
         sourcePackage = "totalcross/android";
         targetTCZ = "app"+DeploySettings.applicationId.toLowerCase();
         targetPackage = "totalcross/"+targetTCZ;
         System.out.println("Android application folder: /data/data/"+(targetPackage.replace('/','.')));
      }
      
      if (!singleApk)
      {
         createLauncher();  // 1
         jar2dex();         // 2
      }
      updateResources(); // 3+4+5
      Utils.jarSigner(fileName+".apk", targetDir);         // 6
      
      String extraMsg = "";
      if (DeploySettings.installPlatforms.indexOf("android,") >= 0)
         extraMsg = callADB();
      
      System.out.println("... Files written to folder "+targetDir+extraMsg);
      
   }

   private String callADB() throws Exception
   {
      String adb = Utils.findPath(DeploySettings.etcDir+"tools/android/adb.exe",false);
      if (adb == null)
         throw new DeployerException("File android/adb.exe not found!");
      String message = Utils.exec(adb+" install -r *.apk",targetDir);
      if (message.indexOf("INPUT:Success") >= 0)
         return " (installed)";
      System.out.println(message);
      return " (error on installl)";
   }
   
   private void createLauncher() throws Exception
   {
      String jarIn = Utils.findPath(DeploySettings.etcDir+"launchers/android/Launcher.jar",false);
      if (jarIn == null)
         throw new DeployerException("File android/Launcher.jar not found!");
      jarOut = targetDir+fileName+".jar";
      
      ZipInputStream zis = new ZipInputStream(new FileInputStream(jarIn));
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(jarOut));
      ZipEntry ze;

      while ((ze = zis.getNextEntry()) != null)
      {
         String name = convertName(ze.getName());
         if (DEBUG) System.out.println("=== Entry: "+name);
         
         zos.putNextEntry(new ZipEntry(name));
         if (name.endsWith(".class"))
            convertConstantPool(zis,zos);
         zos.closeEntry();
      }

      zis.close();
      zos.close();
   }
   
   private void jar2dex() throws Exception
   {
      // java -classpath P:\TotalCrossSDK\etc\tools\android\dx.jar com.android.dx.command.Main --dex --output=classes.dex UIGadgets.jar
      String dxjar = Utils.findPath(DeploySettings.etcDir+"tools/android/dx.jar",false);
      if (dxjar == null)
         throw new DeployerException("File android/dx.jar not found!");
      String javaExe = Utils.searchIn(DeploySettings.path, DeploySettings.appendDotExe("java"));
      String cmd = javaExe+" -classpath "+DeploySettings.pathAddQuotes(dxjar)+" com.android.dx.command.Main --dex --output=classes.dex "+DeploySettings.pathAddQuotes(new File(jarOut).getAbsolutePath()); // guich@tc124_3: use the absolute path for the file
      String out = Utils.exec(cmd, targetDir);
      if (!new File(targetDir+"classes.dex").exists())
         throw new DeployerException("An error occured when compiling the Java class with the Dalvik compiler. The command executed was: '"+cmd+"' at the folder '"+targetDir+"'\nThe output of the command is "+out);
      new File(jarOut).delete(); // delete the jar
   }
   
   private void updateResources() throws Exception
   {
      String ap = Utils.findPath(DeploySettings.etcDir+"launchers/android/resources.ap_",false);
      if (ap == null)
         throw new DeployerException("File android/resources.ap_ not found!");
      String apk = targetDir+fileName+".apk";
      ZipInputStream zis = new ZipInputStream(new FileInputStream(ap));
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(apk));
      ZipEntry ze,ze2;

      // search the input zip file, convert and write each entry to the output zip file
      while ((ze = zis.getNextEntry()) != null)
      {
         String name = ze.getName();
         zos.putNextEntry(ze2=new ZipEntry(name));
         if (name.indexOf("tcfiles.zip") >= 0)
            insertTCFiles_zip(ze2, zos);
         else
         if (name.indexOf("resources.arsc") >= 0)
            insertResources_arsc(zis, zos);
         else
         if (name.indexOf("icon.png") >= 0)
            insertIcon_png(zos);
         else
         if (name.indexOf("AndroidManifest.xml") >= 0)
            insertAndroidManifest_xml(zis,zos);
         
         zos.closeEntry();
      }
      if (singleApk)
      {
         processClassesDex(tcFolder+"TotalCross.apk", "classes.dex", zos);
         copyZipEntry(tcFolder+"TotalCross.apk", "res/layout/main.xml", zos);
      }
      else
      {
         // add classes.dex
         zos.putNextEntry(new ZipEntry("classes.dex"));
         totalcross.io.File f = new totalcross.io.File(targetDir+"classes.dex",totalcross.io.File.READ_WRITE);
         int n;
         while ((n=f.readBytes(buf,0,buf.length)) > 0)
            zos.write(buf, 0, n);
         zos.closeEntry();
         f.delete(); // delete original file
      }
      // include the vm and litebase
      if (tcFolder != null)
         copyZipEntry(tcFolder+"TotalCross.apk", "lib/armeabi/libtcvm.so", zos);
      if (lbFolder != null)
         copyZipEntry(lbFolder+"Litebase.apk", "lib/armeabi/liblitebase.so", zos);
      
      zis.close();
      zos.close();      
   }

   // http://strazzere.com/blog/?p=3
   private static void calcSignature(byte bytes[]) 
   { 
      java.security.MessageDigest md; 
      try 
      { 
         md = java.security.MessageDigest.getInstance("SHA-1"); 
      } 
      catch(java.security.NoSuchAlgorithmException ex) 
      { 
         throw new RuntimeException(ex); 
      } 
      md.update(bytes, 32, bytes.length - 32); 
      try 
      { 
         int amt = md.digest(bytes, 12, 20); 
         if (amt != 20) 
            throw new RuntimeException((new StringBuilder()).append("unexpected digest write:").append(amt).append("bytes").toString()); 
      } 
      catch(java.security.DigestException ex) 
      { 
         throw new RuntimeException(ex); 
      } 
   } 
    
   private static void calcChecksum(byte bytes[]) 
   { 
      Adler32 a32 = new Adler32(); 
      a32.update(bytes, 12, bytes.length - 12); 
      int sum = (int)a32.getValue(); 
      bytes[8] = (byte)sum; 
      bytes[9] = (byte)(sum >> 8); 
      bytes[10] = (byte)(sum >> 16); 
      bytes[11] = (byte)(sum >> 24); 
   }  
   
   private void processClassesDex(String srcZip, String fileName, ZipOutputStream dstZip) throws Exception
   {
      dstZip.putNextEntry(new ZipEntry(fileName));
      byte[] bytes = Utils.loadZipEntry(srcZip,fileName);

      replaceBytes(bytes, sourcePackage.getBytes(), targetPackage.getBytes());
      if (DeploySettings.autoStart)
         replaceBytes(bytes, new byte[]{(byte)0x71,(byte)0xC3,(byte)0x5B,(byte)0x07}, new byte[]{0,0,0,0});
      calcSignature(bytes);
      calcChecksum(bytes);
      dstZip.write(bytes,0,bytes.length);
      dstZip.closeEntry();
   }
   
   private void replaceBytes(byte[] bytes, byte[] fromBytes, byte[] toBytes)
   {
      int ofs=0;
      while ((ofs = Utils.indexOf(bytes, fromBytes, false, ofs)) != -1)
      {
         totalcross.sys.Vm.arrayCopy(toBytes, 0, bytes, ofs, toBytes.length);
         ofs += toBytes.length;
      }
   }

   private void copyZipEntry(String srcZip, String fileName, ZipOutputStream dstZip) throws Exception
   {
      dstZip.putNextEntry(new ZipEntry(fileName));
      byte[] bytes = Utils.loadZipEntry(srcZip,fileName);
      dstZip.write(bytes,0,bytes.length);
      dstZip.closeEntry();
   }
   
   private void insertIcon_png(ZipOutputStream zos) throws Exception
   {
      if (DeploySettings.bitmaps != null) DeploySettings.bitmaps.saveAndroidIcon(zos); // libraries don't have icons
   }

   private totalcross.io.ByteArrayStream readInputStream(java.io.InputStream is)
   {
      totalcross.io.ByteArrayStream bas = new totalcross.io.ByteArrayStream(2048);
      int len;
      while (true)
      {
         try
         {
            len = is.read(buf);
         }
         catch (java.io.IOException e) {break;}
         if (len > 0)
            bas.writeBytes(buf,0,len);
         else
            break;
      }
      return bas;
   }
   
   private void insertAndroidManifest_xml(InputStream zis, OutputStream zos) throws Exception
   {
      totalcross.io.ByteArrayStream bas;
      if (!singleApk)
         bas = readInputStream(zis);
      else
      {
         byte[] bytes = Utils.loadFile(DeploySettings.etcDir+"tools/android/AndroidManifest_singleapk.xml",true);
         bas = new totalcross.io.ByteArrayStream(bytes);
         bas.skipBytes(bytes.length);
      }
      bas.mark();
      totalcross.io.DataStreamLE ds = new totalcross.io.DataStreamLE(bas);
      
      String oldPackage  = singleApk ? sourcePackage.replace('/','.') : "totalcross.app.stub";
      String oldVersion  = "!1.0!";
      String oldTitle    = "Stub";
      String oldActivity = singleApk ? null : ".Stub";
      String oldSharedId = singleApk ? "totalcross.app.sharedid" : null;
      
      String newPackage  = targetPackage.replace('/','.');
      String newVersion  = DeploySettings.appVersion != null ? DeploySettings.appVersion : "1.0";
      String newTitle    = DeploySettings.appTitle;
      String newActivity = singleApk ? null : "."+fileName;
      String newSharedId = singleApk ? "totalcross.app.app"+DeploySettings.applicationId.toLowerCase() : null;
      
      int oldSize = bas.available();
      int difPackage  = (newPackage .length() - oldPackage .length()) * 2;
      int difVersion  = (newVersion .length() - oldVersion .length()) * 2;
      int difTitle    = (newTitle   .length() - oldTitle   .length()) * 2;
      int difActivity = singleApk ? 0 : (newActivity.length() - oldActivity.length()) * 2;
      int difSharedId = !singleApk ? 0 : (newSharedId.length() - oldSharedId.length()) * 2;
      int dif = difPackage + difVersion + difTitle + difActivity + difSharedId;
      
      // get the xml size
      bas.setPos(12); 
      int xmlsize = ds.readInt();
      xmlsize += dif;
      int gap = ((xmlsize + 3) & ~3) - xmlsize;

      int newSize = oldSize + dif + gap;

      // update new size and position of �
      bas.setPos(4);  ds.writeInt(newSize);
      bas.setPos(12); ds.writeInt(xmlsize + gap);
      int len = ds.readInt();
      
      bas.setPos(40);
      int[] positions = new int[len+1];
      for (int i =0,last=len-1; i <= last; i++)
      {
         if (i < last) positions[i+1] = ds.readInt();
         if (DEBUG) System.out.println(i+" "+positions[i]+" ("+(positions[i+1]-positions[i])+")");
      }
      
      String[] strings = new String[len];
      int pos0 = bas.getPos();
      for (int i =0; i < len; i++)
      {
         int pos = bas.getPos();
         String s = new String(ds.readChars());
         if (DEBUG) System.out.println(i+" #"+pos+" ("+(pos-pos0)+") "+s+" ("+s.length()+" - "+(s.length()*2+2+2)+")");
         strings[i] = s;         
         bas.skipBytes(2); // skip 0-terminated string
      }
      
      // read the rest of the resource (other kinds of data)
      int resSize = bas.available();
      byte[] res = new byte[resSize];
      bas.readBytes(res,0,resSize);
      int ofs;
      
      // now the "versionCode" is used to store some properties of the application
      // search and replace the value of versionCode="305419896" (0x12345678) with the application properties
      // note that currently there's no application properties!
      byte[] versionCodeMark = {(byte)0x78,(byte)0x56,(byte)0x34,(byte)0x12};
      ofs = Utils.indexOf(res, versionCodeMark, false);
      if (ofs == -1)
         throw new DeployerException("Error: could not find position for versionCode");
      totalcross.io.ByteArrayStream dtbas = new totalcross.io.ByteArrayStream(res);
      dtbas.setPos(ofs);
      totalcross.io.DataStreamLE dsbas = new totalcross.io.DataStreamLE(dtbas);
      int props = Utils.version2int(newVersion);
      dsbas.writeInt(props);
      
      // if is full screen, search and replace Theme.Black.NoTitleBar by Theme.Black.NoTitleBar.Fullscreen
      if (DeploySettings.isFullScreenPlatform(totalcross.sys.Settings.ANDROID)) // guich@tc120_59
      {
         byte[] themeMark = {(byte)0x09,(byte)0x00,(byte)0x03,(byte)0x01};
         ofs = Utils.indexOf(res, themeMark, false);
         if (ofs == -1)
            throw new DeployerException("Error: could not find position for theme");
         res[ofs] = (byte)0xA; // set Fullscreen attribute
      }
      
      // now, change the names accordingly
      for (int i = 0; i < len; i++)
      {
         String s = strings[i];
         if (oldPackage != null && s.equals(oldPackage))
            strings[i] = newPackage;
         else
         if (oldVersion != null && s.equals(oldVersion))
            strings[i] = newVersion;
         else
         if (oldTitle != null && s.equals(oldTitle))
            strings[i] = newTitle;
         else
         if (oldActivity != null && s.equals(oldActivity))
            strings[i] = newActivity;
         else
         if (oldSharedId != null && s.equals(oldSharedId))
            strings[i] = newSharedId;
      }
      // update the offsets table
      for (int i = 0; i < len; i++)
         positions[i+1] = positions[i] + (strings[i].length()*2+2+2); // 2 for the positions, and 2 for the 0 termination
      
      // now write everything again
      bas.setPos(36);
      for (int i =0; i < len; i++)
         ds.writeInt(positions[i]);
      for (int i =0; i < len; i++)
      {
         String s = strings[i];
         ds.writeChars(s, s.length());
         ds.writeShort(0);
      }
      if (gap > 0)
         for (int i =0; i < gap; i++)
            ds.writeByte(0);
      
      ds.writeBytes(res);
      int nn = bas.getPos();
      if (nn != newSize)
         throw new DeployerException("Something went wrong when parsing AndroidManifest.xml. Expected size is "+newSize+", but got "+nn); 
      
      zos.write(bas.getBuffer(), 0, newSize);
   }
   
   private void insertResources_arsc(InputStream zis, OutputStream zos) throws Exception
   {
      byte[] all;
      byte[] key;
      if (singleApk)
      {
         key = new byte[]{'t',(byte)0,'o',(byte)0,'t',(byte)0,'a',(byte)0,'l',(byte)0,'c',(byte)0,'r',(byte)0,'o',(byte)0,'s',(byte)0,'s',(byte)0,'.',(byte)0,'a',(byte)0,'n',(byte)0,'d',(byte)0,'r',(byte)0,'o',(byte)0,'i',(byte)0,'d',(byte)0};
         all = Utils.loadFile(DeploySettings.etcDir+"tools/android/resources_singleapk.arsc",true);
      }
      else
      {
         key = new byte[]{'t',(byte)0,'o',(byte)0,'t',(byte)0,'a',(byte)0,'l',(byte)0,'c',(byte)0,'r',(byte)0,'o',(byte)0,'s',(byte)0,'s',(byte)0,'.',(byte)0,'a',(byte)0,'p',(byte)0,'p',(byte)0,'.',(byte)0,'s',(byte)0,'t',(byte)0,'u',(byte)0,'b',(byte)0};
         all = readInputStream(zis).toByteArray();
      }
      int ofs = Utils.indexOf(all, key, false);
      if (ofs == -1)
         throw new DeployerException("Could not find position for totalcross.app.stub in arsc.");
      // write the name
      char[] chars = targetPackage.replace('/','.').toCharArray();
      if (chars.length > 0x7F)
         throw new DeployerException("The package name length can't be bigger than "+0x7F);
      int i =0,n = ofs + 0x7F * 2;
      for (; i < chars.length; i++, ofs += 2)
         all[ofs] = (byte)chars[i];
      while (ofs < n)
         all[ofs++] = (byte)0;
      zos.write(all);
   }

   private void copyStream(InputStream in, OutputStream out) throws Exception
   {
      int n;
      while ((n = in.read(buf)) > 0)
         out.write(buf, 0, n);
   }
   
   private void insertTCFiles_zip(ZipEntry ze, ZipOutputStream z) throws Exception
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
      ZipOutputStream zos = new ZipOutputStream(baos);
      
      // parse the android.pkg
      Hashtable ht = new Hashtable(13);
      Utils.processInstallFile("android.pkg", ht);

      Vector vLocals  = (Vector)ht.get("[L]"); if (vLocals == null) vLocals  = new Vector();
      Vector vGlobals = (Vector)ht.get("[G]"); if (vGlobals== null) vGlobals = new Vector();
      vLocals.addElement(DeploySettings.tczFileName);
      if (vGlobals.size() > 0)
         vLocals.addElements(vGlobals.toObjectArray());
      if (singleApk) // include the vm?
      {
         // tc is always included
         // include non-binary files
         vLocals.addElement(DeploySettings.folderTotalCrossSDKDistVM+"TCBase.tcz");
         vLocals.addElement(DeploySettings.folderTotalCrossSDKDistVM+DeploySettings.fontTCZ);
         if ((DeploySettings.packageType & DeploySettings.PACKAGE_LITEBASE) != 0)
            vLocals.addElement(DeploySettings.folderLitebaseSDKDistLIB+"LitebaseLib.tcz");
      }         

      Utils.preprocessPKG(vLocals,true);
      for (int i =0, n = vLocals.size(); i < n; i++)
      {
         String []pathnames = totalcross.sys.Convert.tokenizeString((String)vLocals.items[i],',');
         String pathname = pathnames[0];
         String name = Utils.getFileName(pathname);
         if (pathnames.length > 1)
         {
            name = totalcross.sys.Convert.appendPath(pathnames[1],name);
            if (name.startsWith("/"))
               name = name.substring(1);
         }
         // tcz's name must match the lowercase sharedid
         if (tcFolder != null && pathname.equals(DeploySettings.tczFileName)) 
            name = targetTCZ+".tcz";
         zos.putNextEntry(new ZipEntry(name));
         FileInputStream fis;
         try
         {
            fis = new FileInputStream(pathname);
         }
         catch (FileNotFoundException fnfe)
         {
            fis = new FileInputStream(totalcross.sys.Convert.appendPath(DeploySettings.currentDir, pathname));
         }
         copyStream(fis, zos);
         fis.close();
         zos.closeEntry();
      }
      zos.close();
      // add the file UNCOMPRESSED
      byte[] bytes = baos.toByteArray();
      CRC32 crc = new CRC32();
      crc.update(bytes); 
      ze.setCrc(crc.getValue());
      ze.setMethod(ZipEntry.STORED);
      ze.setCompressedSize(bytes.length);
      ze.setSize(bytes.length);
      z.write(bytes);
   }

   private void convertConstantPool(ZipInputStream is, ZipOutputStream os) throws Exception
   {
      totalcross.io.ByteArrayStream bas = new totalcross.io.ByteArrayStream(1024);
      totalcross.io.DataStream ds = new totalcross.io.DataStream(bas);
      int n;

      while ((n = is.read(buf)) > 0)
         bas.writeBytes(buf, 0, n);
      bas.setPos(0);
      JavaClass jclass = new JavaClass();
      jclass.load(ds);

      checkConstantPool(jclass);

      bas.reuse();
      jclass.save(ds);
      os.write(bas.getBuffer(), 0, bas.getPos());
   }

   private static void checkConstantPool(JavaClass jclass)
   {
      UTF8 descriptor;
      // Check all class and name/type constants
      if (DEBUG) System.out.println("Constant pool");
      int count = jclass.constantPool.size();
      for (int i = 1; i < count; i++)
      {
         JavaConstant constant = (JavaConstant)jclass.constantPool.items[i];
         i += constant.slots() - 1; // skip empty slots

         switch (constant.tag)
         {
            case JavaConstant.CONSTANT_INTEGER:
               String cla = jclass.getClassName();
               if (DeploySettings.autoStart && cla.endsWith("/StartupIntentReceiver") && ((Integer)constant.info).value == 123454321)
               {
                  Integer it = new Integer();
                  it.value = 0;
                  constant.info = it;
               }
               break;
            case JavaConstant.CONSTANT_CLASS:
               descriptor = ((Class)constant.info).getValueAsName();
               descriptor.value = convertName(descriptor.value);
               break;
            case JavaConstant.CONSTANT_NAME_AND_TYPE:
               descriptor = ((NameAndType)constant.info).getValue2AsDescriptor();
               descriptor.value = convertName(descriptor.value);
               break;
         }
      }

      // Check class fields
      if (DEBUG) System.out.println("Fields");
      count = jclass.fields.size();
      for (int i = 0; i < count; i ++)
      {
         JavaField field = (JavaField)jclass.fields.items[i];

         descriptor = (UTF8)field.descriptor.info;
         descriptor.value = convertName(descriptor.value);

         // Check field attributes
         int count2 = field.attributes.size();
         for (int j = 0; j < count2; j ++)
            checkAttribute((JavaAttribute)field.attributes.items[j]);
      }

      // Check class methods
      if (DEBUG) System.out.println("Methods");
      count = jclass.methods.size();
      for (int i = 0; i < count; i ++)
      {
         JavaMethod method = (JavaMethod)jclass.methods.items[i];

         descriptor = (UTF8)method.descriptor.info;
         descriptor.value = convertName(descriptor.value);

         // Check method attributes
         int count2 = method.attributes.size();
         for (int j = 0; j < count2; j ++)
            checkAttribute((JavaAttribute)method.attributes.items[j]);
      }

      // Check class attributes
      if (DEBUG) System.out.println("Atributes");
      count = jclass.attributes.size();
      for (int i = 0; i < count; i ++)
         checkAttribute((JavaAttribute)jclass.attributes.items[i]);

      if (DEBUG) System.out.println("FINISHED");
   }

   /**
    * @param attribute
    * @param classes
    */
   private static void checkAttribute(JavaAttribute attribute)
   {
      if (attribute.info instanceof SourceFile)
      {
         JavaConstant source = ((SourceFile)attribute.info).sourceFile;
         UTF8 descriptor = (UTF8)source.info;
         descriptor.value = convertName(descriptor.value);
      }
      else
      if (attribute.info instanceof LocalVariableTable)
      {
         Vector variables = ((LocalVariableTable)attribute.info).variables;
         int count = variables.size();
         for (int i = 0; i < count; i ++)
         {
            UTF8 descriptor = (UTF8)((LocalVariableTable.LocalVariable)variables.items[i]).descriptor.info;
            descriptor.value = convertName(descriptor.value);
         }
      }
      else if (attribute.info instanceof Code)
      {
         Code code = (Code)attribute.info;
         Vector attributes = code.attributes;
         int count = attributes.size();
         for (int i = 0; i < count; i ++)
            checkAttribute((JavaAttribute)attributes.items[i]);
      }
   }

   private static String convertName(String name)
   {
      String value = name.replace("totalcross/app/stub", targetPackage); // totalcross/app/stub/R -> totalcross/app/uigadgets/R
      value = value.replace("Stub",fileName); // totalcross/app/stub/Stub -> totalcross/app/uigadgets/Stub
      if (DEBUG) System.out.println(name+" -> "+value);
      return value;
   }
}