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



package totalcross.ui.event;

/**
 * An event that represents a pen drag.
 *
 * @author Keith Meehl
 */
public class DragEvent extends PenEvent
{
	/** The direction constant for a drag or flick right. */
	public static final int RIGHT = 1;
	/** The direction constant for a drag or flick left. */
	public static final int LEFT = 2;
	/** The direction constant for a drag or flick up. */
	public static final int UP = 3;
	/** The direction constant for a drag or flick down. */
	public static final int DOWN = 4;
	
	private final static String[] DIRECTIONS = {"","RIGHT","LEFT","UP","DOWN"}; 

   public int xDelt,yDelt,xTotal,yTotal;

   public int direction;

   /** Constructs an empty DragEvent. */
   public DragEvent()
   {
   }

   /**
    * Constructs a new DragEvent from a PenEvent, setting a new timestamp and setting consumed to false.
    */
   public DragEvent(PenEvent evt)
   {
      update(evt);
   }

   /**
    * Updates this DragEvent from a PenEvent, setting a new timestamp and setting consumed to false.
    */
   public DragEvent update(PenEvent evt)
   {
      this.x = evt.x;
      this.y = evt.y;
      this.type = evt.type;
      timeStamp = totalcross.sys.Vm.getTimeStamp();
      target = evt.target;
      this.modifiers = evt.modifiers;
      return this;
   }
   
   public String toString()
   {      
      return EVENT_NAME[type-200]+", direction: "+DIRECTIONS[direction]+", pos: "+x+","+y+" "+super.toString();
   }
}
