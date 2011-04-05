/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *  This file is covered by the GNU LESSER GENERAL PUBLIC LICENSE VERSION 3.0    *
 *  A copy of this license is located in file license.txt at the root of this    *
 *  SDK or can be downloaded here:                                               *
 *  http://www.gnu.org/licenses/lgpl-3.0.txt                                     *
 *                                                                               *
 *********************************************************************************/



package tc.samples.lang.thread.socket;

import totalcross.sys.*;
import totalcross.ui.*;
import totalcross.ui.gfx.*;

public class TypingContainer extends Container implements Runnable
{
   String typingText = "The new virtual machine, called TotalCross, has better performance due to a new instruction set that eliminates limitations in the existing SuperWaba virtual machine, with enhancements such as unlimited object size, preemptive threads and a new high-performance garbage collector that is 20X faster than the SuperWaba's. Additionally, deployed files are now compacted, to acheive a 30% reduction in size over SuperWaba applications.";

   int index = 0;
   MultiEdit me;
   boolean fill;

   public TypingContainer(boolean fill)
   {
      this.fill = fill;
   }

   public void initUI()
   {
      super.initUI();
      setBackColor(Color.WHITE);
      setBorderStyle(BORDER_RAISED);

      me = new MultiEdit(0,0);
      add(me,0,0,width,height);
      if (fill)
         me.justify = fill;
      me.setEditable(false);
      me.hasCursorWhenNotEditable = false;

      Thread t = new Thread(this);
      t.start();
   }

   public void run()
   {
      int length = typingText.length();
      StringBuffer sb = new StringBuffer(length);
      while (true)
      {
         index = 0;
         while (index < length)
         {
            sb.append(typingText.charAt(index));
            me.setText(sb.toString());
            if (ThreadedSocket.paused || ThreadedSocket.paused0) me.repaintNow();
            index++;
            Vm.sleep(100);
         }
         Vm.sleep(1000);
         sb.setLength(0);
         me.setText("");
         index = 0;
      }
   }
}
