/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/



#ifndef GFX_EX_H
#define GFX_EX_H

#define SETPIXEL32(r,g,b) (((r) << 16) | ((g) << 8) | (b))           // 00RRGGBB
#define SETPIXEL565(r,g,b) ((((r) >> 3) << 11) | (((g) >> 2) << 5) | (((b) >> 3))) // bits RRRRRGGGGGGBBBBB
#define SETPIXEL565_(dest,p) do {uint32 temp = p & 0xF800F800; *dest++ = ( (temp>>16) | ((p>>13)&0x7E0) | (temp>>11) );} while(0);

typedef struct
{
   jfieldID mNativeBitmapID;
   jobject mBitmap;
} TScreenSurfaceEx, *ScreenSurfaceEx;

#endif
