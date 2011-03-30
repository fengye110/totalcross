/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2003 Gilbert Fridgen                                           *
 *  Copyright (C) 2003-2011 SuperWaba Ltda.                                      *
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



package totalcross.pim.memobook;
import totalcross.pim.*;
import totalcross.util.*;
/**
 * An interface describing the functionality an implementation of an <code>MemoRecord</code> (e.g. one memo) must provide
 * @author fridgegi
 */
public interface MemoRecord extends VCalRecord
{
   /**
    * Reads <code>MemoFields</code> from the device, maps them and stores them in <code>MemoField<code>s and returns them.
    * @return a Vector containing the contact information of this record in <code>MemoField</code>s
    */
   public Vector getFields();
   /**
    * Reads information from the passed fields, maps them and writes them to the device. Passes not supported fields to a <code>NotSupportedHandler</code>
    * @param fields the fields to write to the device
    */
   public void setFields(Vector fields);
   /**
    * Reads this <code>MemoRecord</code>'s note directly.
    * @return the note's text
    */
   public String rawReadNote();
   /**
    * Writes this <code>MemoRecord</code>'s note directly
    * @param note the text to write to the note
    */
   public void rawWriteNote(String note);
   /**
    * Registeres a NotSupportedHandler that handle's <code>MemoField</code>s this device cannot store
    * @param nsh the NotSupportedHandler to register
    */
   public void registerNotSupportedhandler(MemoNotSupportedHandler nsh);
}
