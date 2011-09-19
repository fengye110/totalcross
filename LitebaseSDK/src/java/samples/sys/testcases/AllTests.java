/*********************************************************************************
 *  TotalCross Software Development Kit - Litebase                               *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/

package samples.sys.testcases;

// Created on 29/04/2004
import litebase.*;
import totalcross.io.*;
import totalcross.sys.*;
import totalcross.unit.*;
import totalcross.ui.*;
import totalcross.ui.event.*;

/** 
 * The class that executes all test cases.
 */
public class AllTests extends TestSuite
{
   /**
    * Indicates if the logger is to be used or not during AllTests.
    */
   static boolean useLogger;
   
   /**
    * Indicates if the connections of the tests except for <code>TestAsciiTables</code> and <code>TestSourcePath</code> use ascii connections.
    */
   private static boolean isAscii;
   
   static
   {
      Settings.useNewFont = true;
   }
   
   /** 
    * Constructs all the test cases. Needs to be used with TotalCross. 
    */
   public AllTests()
   {
      super("Litebase Test Suite");
      if (Settings.platform.equals(Settings.ANDROID))
         Vm.debug(Vm.ALTERNATIVE_DEBUG);
      addTestCase(TestAsciiTables.class); // juliana@210_2: now Litebase supports tables with ascii strings.
      addTestCase(TestBigJoins.class);
      addTestCase(TestBlob.class);
      addTestCase(TestCachedRows.class);
      addTestCase(TestClosedLitebaseAndProcessLogs.class);
      addTestCase(TestComposedIndexAndPK.class);
      addTestCase(TestDate_DateTime.class);
      addTestCase(TestDeleteAndMetaData.class);
      addTestCase(TestDeleteAndPurge.class);
      addTestCase(TestDrop.class);
      addTestCase(TestDuplicateEntry.class);
      addTestCase(TestEndianess.class);
      addTestCase(TestIndexIneqAndLike.class);
      addTestCase(TestIndexRebalance.class);
      addTestCase(TestInvalidArguments.class);
      addTestCase(TestJoin.class);
      addTestCase(TestLogger.class);
      addTestCase(TestMaxMin.class);
      addTestCase(TestMultipleConnection.class);
      addTestCase(TestNullAndDefaultValues.class);
      addTestCase(TestOrderBy.class);
      addTestCase(TestOrderByIndices.class);
      addTestCase(TestPalmMemoryCard.class);
      addTestCase(TestPreparedStatement.class);
      addTestCase(TestPrimaryKey.class);
      addTestCase(TestRename.class);
      addTestCase(TestResultSet.class); // juliana@211_4: solved bugs with result set dealing.
      addTestCase(TestReIndex2rowId.class);
      addTestCase(TestRowIdAndPurge.class);
      addTestCase(TestRowIterator.class);
      addTestCase(TestSelectClause_AggFunctions.class);
      addTestCase(TestSourcePath.class);
      addTestCase(TestSQLFunctions.class);
      addTestCase(TestTableRecovering.class);
      addTestCase(TestThread.class);
      addTestCase(TestVirtualRecords.class);
      addTestCase(TestWhereClause_Basic.class);
      addTestCase(TestWhereClause_Caseless.class);
      addTestCase(TestWhereClause_Indexes.class);
      Vm.tweak(Vm.TWEAK_DUMP_MEM_STATS, true);
   }

	/** 
	 * Initializes the user interface.
	 */
   public void initUI() // rnovais@570_77
   {
      String appSecretKey = Settings.appSecretKey;
      useLogger = appSecretKey != null && appSecretKey.startsWith("y");
      isAscii = appSecretKey != null && appSecretKey.endsWith("y"); 
      if (useLogger)
         LitebaseConnection.logger = LitebaseConnection.getDefaultLogger();
      super.initUI();
      setMenuBar(mbar = new MenuBar(new MenuItem[][]{mbar.getMenuItems()[0], 
                                   {new MenuItem("Config"), new MenuItem("isAscii", isAscii), new MenuItem("Drop all tables")}, 
                                   {new MenuItem("Logs"), new MenuItem("Use Logger", useLogger), new MenuItem("Erase All Loggers")}}));
   }

   /** 
    * Detects the menu items events other than File.
    * 
    * @param event The event being handled.
    */
   public void onEvent(Event event)
   {
      if (event.type == ControlEvent.PRESSED && event.target == mbar) 
      {   
         switch (mbar.getSelectedIndex())
         {
            case 101:
               isAscii = !isAscii;
               Settings.appSecretKey = (useLogger? "y" : "n") + (isAscii? "y" : "n");
               break;
            case 102: // Drops All Tables. 
               dropAllTables();
               break;
            case 201: // Sets or unsets the logger usage.
               useLogger = !useLogger;
               if (useLogger)
                  LitebaseConnection.logger = LitebaseConnection.getDefaultLogger();
               else
               {
                  if (LitebaseConnection.logger != null)
                  {
                     LitebaseConnection.logger.dispose(true);
                     LitebaseConnection.logger = null;
                  }
               }
               Settings.appSecretKey = (useLogger? "y" : "n") + (isAscii? "y" : "n");
               break;
            case 202: // Deletes all the logger files.
               LitebaseConnection.deleteLogFiles();
         }
      } 
         
      super.onEvent(event);
   }

   /**
    * Drops all tables used for the testcases. Notice that all the tables in the folders used by the tests and with the creation id "Test"
    * will be erased even if they are from another application.
    */
   private void dropAllTables()
   {
      int i = 9;
      String temporario;
      String tempPath = Convert.appendPath(Settings.appPath, "temp/");
      
      try
      {
         temporario = Convert.appendPath(File.getCardVolume().getPath(), "temporário/");
      }
      catch (IOException exception)
      {
         temporario = Convert.appendPath(Settings.appPath, "temporário/");
      }
      catch (NullPointerException exception)
      {
         temporario = Convert.appendPath(Settings.appPath, "temporário/");
      }

      // The paths used by AllTests.
      String[] paths =
      {
         Settings.platform.equals(Settings.PALMOS)? "/Litebase_DBs/" : Settings.appPath, Settings.dataPath, 
         tempPath, tempPath + "a/", tempPath + "b/", temporario, "/", "/dba/", "/dbb/"
      };

      int[] slots = {1, 1, 1, 1, 1, 1, -1, -1, -1};

      // The subfolders used by AllTests.
      File[] folders = new File[9];
         
      while (--i >= 0) // Erases the table files from this application.
      {
         try
         {
            
            if (paths[i] != null)
            {
               folders[i] = new File(paths[i], File.DONT_OPEN, slots[i]);
               LitebaseConnection.dropDatabase("Test", paths[i], slots[i]);
            }
         }
         catch (DriverException exception) {}
         catch (IOException exception) {}
      }

      // Erases the palm memory card forlders.
      try
      {
         folders[8].delete();
      } 
      catch (IOException exception) {}
      try
      {
         folders[7].delete();
      }
      catch (IOException exception) {}
      
      i = 6;
      while (--i >= 2) // Erases the folders, being careful to erase the empty folders first.
         try
         {
            folders[i].delete();
         } 
         catch (IOException exception) {}
   }
   
   /**
    * Does the tests using the kind of string selected. 
    * 
    * @return A driver connection using ascii or unicode characters depending on which option was made and the application id as the application id
    * used by Litebase.
    */
   static LitebaseConnection getInstance()
   {
      return LitebaseConnection.getInstance(Settings.applicationId, 
                                                     "chars_type =" + (isAscii? "ascii" : "unicode") + "; path = " + Settings.appPath);
   }
   
   /**
    * Does the tests using the kind of string selected. 
    * 
    * @param appCrid The application id given by the user.
    * @return A driver connection using ascii or unicode characters depending on which option was made.
    */
   static LitebaseConnection getInstance(String appCrid)
   {
      return LitebaseConnection.getInstance(appCrid, "chars_type =" + (isAscii? "ascii" : "unicode") + "; path = " + Settings.appPath);
   }
   
   /**
    * Does the tests using the kind of string selected. 
    * 
    * @param appCrid The application id given by the user.
    * @param path The path where the tables are to be created.
    * @return A driver connection using ascii or unicode characters depending on which option was made and the path given by the user.
    */
   static LitebaseConnection getInstance(String appCrid, String path)
   {
      return LitebaseConnection.getInstance(appCrid, "chars_type =" + (isAscii? "ascii" : "unicode") + "; path = " + path);
   }
}

