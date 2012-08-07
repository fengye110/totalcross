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

package ras.ui;

import ras.*;

import totalcross.io.*;
import totalcross.net.*;
import totalcross.sys.*;
import totalcross.ui.*;
import totalcross.ui.dialog.*;
import totalcross.ui.event.*;
import totalcross.ui.font.*;
import totalcross.ui.gfx.*;
import totalcross.xml.soap.*;

public class ActivationWindow extends MainWindow
{
   static
   {
      Settings.useNewFont = true;
   }
   
   private ActivationClient client;
   private ActivationHtml html;

   public ActivationWindow() // used just to test on JavaSE
   {
      this(null);
   }

   public ActivationWindow(ActivationClient client)
   {
      super("", NO_BORDER);
      setUIStyle(Settings.Android);
      setBackColor(Color.WHITE);
      this.client = client;
   }
   
   public void initUI()
   {
      if (Settings.keyboardFocusTraversable)
         Settings.geographicalFocus = true;
      html = ActivationHtml.getInstance(ActivationHtml.ACTIVATION_START);
      if (html != null)
         swap(html);
      else
      {      
         int c1 = 0x0A246A;
         Font f = font.adjustedBy(2,true);
         Bar headerBar = new Bar("Activation");
         headerBar.createSpinner(Color.WHITE);
         headerBar.setFont(f);
         headerBar.setBackForeColors(c1,Color.WHITE);
         add(headerBar, LEFT,0,FILL,PREFERRED);
   
         Label l = new Label("The TotalCross virtual machine needs to be activated. This process requires your device's internet connection to be properly set up.");
         l.autoSplit = true;
         l.align = FILL;
         add(l,LEFT+5,AFTER+2,FILL-5,PREFERRED);
   
         headerBar.startSpinner();
         
         repaintNow();
   
         try
         {
            if (client == null)
               Vm.sleep(3000);
            else
               client.activate();
            headerBar.stopSpinner();
            success();
         }
         catch (ActivationException ex)
         {
            headerBar.stopSpinner();
            failure(ex);
         }
      }
   }

   private void failure(ActivationException ex)
   {
      Throwable cause = ex.getCause();
      String s = ex.getMessage() + " The activation process cannot continue. The application will be terminated.";

      if (cause instanceof SOAPException || cause instanceof IOException)
         s += " Try again 2 or 3 times if there's really an internet connection.";

      alert("Failure",s,-1);
      exit(1);
   }

   private void success()
   {
      alert("Success", "TotalCross is now activated!\nPlease restart your application.",0x008800);
      exit(0);
   }

   public void onEvent(Event e)
   {
      if (e.type == ControlEvent.PRESSED && html != null && e.target == html)
      {
         if (html.type != ActivationHtml.ACTIVATION_START)
            exit(html.type == ActivationHtml.ACTIVATION_SUCCESS ? 0 : 1);
         else
            try
            {
               if (!Socket.isInternetAccessible())
                  throw new ActivationException("", new IOException());
               if (client == null)
                  Vm.sleep(3000);
               else
                  client.activate();
               html = ActivationHtml.getInstance(ActivationHtml.ACTIVATION_SUCCESS);
               if (html != null)
                  swap(html);
               else
                  success();
            }
            catch (ActivationException ex)
            {
               Throwable cause = ex.getCause();
               cause.printStackTrace();
               html = ActivationHtml.getInstance(cause instanceof SOAPException || cause instanceof IOException ? ActivationHtml.ACTIVATION_NOINTERNET : ActivationHtml.ACTIVATION_ERROR);
               if (html != null)
                  swap(html);
               else
                  failure(ex);
            }
      }
   }
   
   private void alert(String tit, String s, int bg)
   {
      MessageBox mb = new MessageBox(tit, s.replace('\n', ' '), new String[] { "  Exit  " });
      mb.setTextAlignment(LEFT);
      mb.titleColor = Color.WHITE;
      mb.yPosition = BOTTOM;
      mb.popup();
   }
   
   public void screenResized()
   {
      if (html != null && Settings.isLandscape())
      {
         // make sure that the MessageBox takes the whole screen
         MessageBox mb = new MessageBox("Attention","This program must be run in portrait mode.\nPlease rotate back the device.",null)
         {
            public void setRect(int x, int y, int w, int h)
            {
               super.setRect(x,y,Settings.screenWidth,Settings.screenHeight);
            }
         };
         mb.transitionEffect = TRANSITION_NONE;
         mb.popupNonBlocking();
         while (Settings.isLandscape())
            pumpEvents();
         mb.unpop();
      }
      else super.screenResized();
   }
}