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

package totalcross.ui;

import totalcross.ui.event.*;
import totalcross.ui.gfx.*;
import totalcross.ui.image.*;
import totalcross.util.*;

/** 
 * Bar is a class that provides a title area and a button area (at right).
 * The title and the button are optional, although it doesn't make sense to have a Bar
 * without title and buttons.
 * 
 * You can add or remove buttons, and change the title text.
 * 
 * Here's an example of how to use it, taken from the UIGadgets sample:
 * <pre>
 * final Bar h1,h2;
 * Font f = Font.getFont(true,Font.NORMAL_SIZE+2);
 * h1 = new Bar("fakeboot");
 * h1.canSelectTitle = true;
 * h1.setFont(f);
 * h1.setBackForeColors(0x0A246A,Color.WHITE);
 * h1.addButton(new Image("ic_dialog_alert.png"));
 * h1.addButton(new Image("ic_dialog_info.png"));
 * add(h1, LEFT,0,FILL,PREFERRED); // use 0 instead of TOP to overwrite the default menu area
 * </pre>
 * A ControlEvent.PRESSED is sent to the caller, and the button index can be retrieved using 
 * <code>getSelectedIndex</code> method.
 * 
 * By default, the background is shaded. You can change it to plain using
 * <code>h1.backgroundStyle = BACKGROUND_SOLID;</code> 
 */

public class Bar extends Container
{
   private BarButton title;
   private Vector icons = new Vector(2);
   private boolean initialized;
   private int selected=-1;
   private int c1,c2,c3,c4,tcolor,pcolor;
   
   /** Set to true to allow the title to be selected and send events. */
   public boolean canSelectTitle;
   
   /** The title horizontal alignment (LEFT, CENTER, RIGHT). Defaults to LEFT. */
   public int titleAlign = LEFT;
   
   private class BarButton extends Control
   {
      String title;
      Image icon0,icon;
      int gap,px,py;
      boolean pressed;
      
      BarButton(String title, Image icon) // title or icon
      {
         this.title = title;
         this.icon0 = icon;
      }
      
      public void onFontChanged()
      {
         if (title != null)
         {
            gap = fm.charWidth(' ');
         }
         else
         try
         {
            icon = null;
            icon = icon0.getSmoothScaledInstance(icon0.getWidth()*fmH/icon0.getHeight(),fmH,icon0.useAlpha ? -1 : backColor);
         } catch (ImageException e) {icon = icon0;}
      }
      
      public void onBoundsChanged(boolean b)
      {
         onFontChanged();
         if (title != null)
         {
            px = titleAlign == LEFT ? gap : titleAlign == CENTER ? (width-fm.stringWidth(title))/2 : (width-fm.stringWidth(title)-gap);
            py = (height-fmH)/2;
         }
         else
         {
            px = (width -icon.getWidth()) /2;
            py = (height-icon.getHeight())/2;
         }
      }
      
      public void onPaint(Graphics g)
      {
         int w = width;
         int h = height;
         
         if (pressed)
            g.fillShadedRect(0,0,w,h,true,false,foreColor,pcolor,30);
         
         // draw borders
         g.foreColor = c1; g.drawLine(0,0,w,0);
         g.foreColor = c3; g.drawLine(w-1,0,w-1,h);
         g.foreColor = c4; g.drawLine(0,h-1,w,h-1);
         g.foreColor = c2; 
         if (backgroundStyle == BACKGROUND_SHADED) 
            g.fillShadedRect(0,1,1,h-2,true,false,foreColor,c2,30); // drawLine causes an unexpected effect on shaded backgrounds
         else
            g.drawLine(0,0,0,h); 

         // draw contents
         if (title != null)
         {
            g.setClip(gap,0,w-gap-gap,h);
            g.foreColor = tcolor;
            g.drawText(title, px+1,py-1);
            g.foreColor = backColor;
            g.drawText(title, px-1,py+1);
            g.foreColor = foreColor;
            g.drawText(title, px,py);
         }
         else
            g.drawImage(icon, px,py);
      }
      
      public void onEvent(Event e)
      {
         if (!canSelectTitle && title != null)
            return;
         
         switch (e.type)
         {
            case PenEvent.PEN_DOWN:
               pressed = true;
               Window.needsPaint = true;
               break;
            case PenEvent.PEN_UP:
               if (pressed)
               {
                  selected = appId;
                  parent.postPressedEvent();
               }
               else selected = -1;
               pressed = false;
               Window.needsPaint = true;
               break;
            case PenEvent.PEN_DRAG:
            {
               PenEvent pe = (PenEvent)e;
               boolean armed = isInsideOrNear(pe.x,pe.y);
               if (armed != pressed)
               {
                  pressed = armed;
                  Window.needsPaint = true;
               }
               break;
            }
         }
      }
   }
   
   /** Constructs a Bar class without a title. Note that if 
    * you call the setTitle method, a RuntimeException will be thrown. 
    *
    * If you want to change the title later, use the other constructor and pass an empty String ("").
    */
   public Bar()
   {
      this(null);
   }
   
   /** Constructs a Bar class with the given title. */
   public Bar(String title)
   {
      this.title = title != null ? new BarButton(title,null) : null;
      this.backgroundStyle = BACKGROUND_SHADED;
      setFont(font.asBold());
   }
   
   /** Changes the title to the given one. */
   public void setTitle(String newTitle)
   {
      if (this.title == null)
         throw new RuntimeException("You can only set a title if you set one in the Bar's constructor.");
      title.title = newTitle;
      title.onBoundsChanged(false);
   }
   
   /** Adds an image Button */
   public void addButton(Image icon)
   {
      icons.addElement(new BarButton(null,icon));
      for (int i = icons.size(); --i >= 0;) ((Control)icons.items[i]).appId = i+1; // update appId used for selection
      if (initialized) initUI();
   }
   
   /** Removes a Button at the given index, starting at 1. */
   public void removeButton(int index)
   {
      icons.removeElementAt(index-1);
      for (int i = icons.size(); --i >= 0;) ((Control)icons.items[i]).appId = i+1;
      if (initialized) initUI();
   }
   
   /** Returns the selected button, or -1 if none was selected.
    * 
    * The title is always index 0 (even if there's no title), and the buttons start at index 1. 
    */
   public int getSelectedIndex()
   {
      return selected;
   }
   
   public void initUI()
   {
      removeAll();
      int n = icons.size();
      if (title == null) // if there's no title, make the icons take the whole size of the container
      {
         for (int i = n; --i > 0;)
            add((BarButton)icons.items[i], i==n-1 ? RIGHT : BEFORE, TOP, width/n, FILL);
         if (n > 0)
            add((BarButton)icons.items[0], LEFT, TOP, n == 1 ? FILL : FIT, FILL);
      }
      else
      {
         for (int i = n; --i >= 0;)
            add((BarButton)icons.items[i], i==n-1 ? RIGHT : BEFORE, TOP, height, FILL);
         add(title, LEFT, TOP, n == 0 ? FILL : FIT, FILL);
      }
      initialized = true;
   }
   
   public void onColorsChanged(boolean colorsChanged)
   {
      c1 = Color.brighter(backColor,30);
      c2 = Color.brighter(backColor,60);
      c3 = Color.darker(backColor,30);
      c4 = Color.darker(backColor,60);
      tcolor = Color.darker(backColor,32);
      pcolor = Color.interpolate(backColor,foreColor);
   }
   
   public int getPreferredWidth()
   {
      return parent == null ? FILL : parent.width;
   }
   
   public int getPreferredHeight()
   {
      return fmH*2;
   }
   
   public void reposition()
   {
      super.reposition();
      initUI();
   }
}
