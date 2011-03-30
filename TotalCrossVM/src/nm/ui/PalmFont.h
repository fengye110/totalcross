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



#ifndef PALMFONT_H
#define PALMFONT_H

#pragma pack(2)
typedef struct
{
   uint16 antialiased;      // true if the font is antialiased
   uint16 firstChar;        // ASCII code of first character
   uint16 lastChar;         // ASCII code of last character
   uint16 spaceWidth;       // width of the space character
   uint16 maxWidth;         // width of font rectangle
   uint16 maxHeight;        // height of font rectangle
   uint16 owTLoc;           // offset to offset/width table
   uint16 ascent;           // ascent
   uint16 descent;          // descent
   uint16 rowWords;         // row width of bit image / 2
}  __attribute_packed__ *PalmFont, TPalmFont;
#pragma pack()

typedef struct
{
   TCZFile tcz;
   char name[32];
} *FontFile, TFontFile;

typedef struct
{
   uint8 *bitmapTable;
   TPalmFont fontP;
   uint16 rowWidthInBytes;
   uint16 *bitIndexTable;
} *UserFont, TUserFont;

int32 getJCharWidth(Context currentContext, Object fontObj, JChar ch);
int32 getJCharPWidth(Context currentContext, Object fontObj, JCharP s, int32 len); // len is NOT optional, it must be given
UserFont loadUserFontFromFontObj(Context currentContext, Object fontObj, JChar ch);
FontFile loadFontFile(char *fontName);
UserFont loadUserFont(FontFile ff, bool plain, int32 size, JChar c);  // use size=-1 to load the normal size
bool fontInit();
void fontDestroy();

#endif
