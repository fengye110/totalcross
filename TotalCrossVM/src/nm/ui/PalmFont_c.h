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



#include "PalmFont.h"

static char defaultFontName[16];

#define SIZE_LEN 4
UserFont baseFontN[SIZE_LEN],baseFontB[SIZE_LEN];
static int32 realSizes[] = {20,40,60,80};
bool useRealFont;

bool fontInit(Context currentContext)
{
   int32 *maxfs = null, *minfs = null, *normal = null;
   Object *defaultFontNameObj;
   TCClass c;
   c = loadClass(currentContext, "totalcross.ui.font.Font", false);
   if (c)
   {
      maxfs = getStaticFieldInt(c, "MAX_FONT_SIZE");
      minfs = getStaticFieldInt(c, "MIN_FONT_SIZE");
      normal = getStaticFieldInt(c, "NORMAL_SIZE");
      tabSizeField = getStaticFieldInt(c, "TAB_SIZE");
      defaultFontNameObj = getStaticFieldObject(c, "DEFAULT");
   }
   if (!maxfs || !minfs || !normal || !tabSizeField || !defaultFontNameObj)
      return false;

   JCharP2CharPBuf(String_charsStart(*defaultFontNameObj), String_charsLen(*defaultFontNameObj), defaultFontName);
   maxFontSize = *maxfs;
   minFontSize = *minfs;
   normalFontSize = *normal;
   fontsHeap = heapCreate();
   if (fontsHeap == null)
      return false;
   htUF = htNew(23, null);
   if (!htUF.items)
   {
      heapDestroy(fontsHeap);
      return false;
   }
   defaultFont = loadFontFile(defaultFontName);
   if (defaultFont == null)
   {
      alert("Font file is missing.\nPlease install TCFont.tcz");
      heapDestroy(fontsHeap);
      htFree(&htUF, null);
   }
   else
   {        
      int32 i;
      useRealFont = true;
      for (i = 0; i < SIZE_LEN; i++)
      {
         baseFontN[i] = loadUserFont(currentContext, defaultFont, false, realSizes[i], ' ');
         baseFontB[i] = loadUserFont(currentContext, defaultFont, true,  realSizes[i], ' ');
      }
      useRealFont = false;
   }
   return defaultFont != null;
}

void fontDestroy()
{
   VoidPs *list, *head;
   list = head = openFonts;
   if (head != null)
   do
   {
      FontFile ff = (FontFile)list->value;
      tczClose(ff->tcz);
      list = list->next;
   } while (list != head);
   openFonts = null;
   heapDestroy(fontsHeap);
   htFree(&htUF, null);
}

static FontFile findFontFile(char* fontName)
{
   VoidPs *list, *head;
   list = head = openFonts;
   if (head != null)
   {
      int32 len = xstrlen(fontName);
      do
      {
         FontFile ff = (FontFile)list->value;
         if (strCaseEqn(fontName, ff->name, len))
            return ff;
         list = list->next;
      } while (list != head);
   }
   return null;
}

#ifdef ANDROID
int32 callFindTCZ(CharP name);
#endif

FontFile loadFontFile(char *fontName)
{
   FontFile ff;
   TCZFile tcz;
   char fullName[150];

   IF_HEAP_ERROR(fontsHeap)
   {
      heapDestroy(fontsHeap);
      return null;
   }

   // first, check if its already loaded
   ff = findFontFile(fontName);
   if (ff == null)
   {
#ifdef ANDROID      
      int32 idx = callFindTCZ(fontName);
      if (idx >= 0)
         xstrcpy(fullName, fontName);
      else
      {
         xstrprintf(fullName, "%s.tcz", fontName); // append a tcz to the font name
         idx = callFindTCZ(fullName);
      }
      if (idx < 0 && 'a' <= fontName[0] && fontName[0] <= 'z')
      {
         fontName[0] = toUpper(fontName[0]); // the user may have created the font with uppercase, like Arial
         return loadFontFile(fontName);
      }
      if (idx >= 0)
      {
         tcz = tczOpen(fullName, true);
#else
      FILE* f = findFile(fontName, null);
      if (f == null)
      {
         xstrprintf(fullName, "%s.tcz", fontName); // append a tcz to the font name
         f = findFile(fullName, null);
      }
#ifndef WIN32 // win32 file system is not case sensitive
      if (f == null && 'a' <= fontName[0] && fontName[0] <= 'z')
      {
         fontName[0] = toUpper(fontName[0]); // the user may have created the font with uppercase, like Arial
         return loadFontFile(fontName);
      }
#endif
      if (f != null)
      {
         tcz = tczOpen(f, null);
#endif
         if (tcz != null)
         {
            ff = newXH(FontFile, fontsHeap);
            xstrncpy(ff->name, fontName, min32(xstrlen(fontName), 31));
            CharPToLower(ff->name); // fonts are stored in lowercase inside the tcz file
            ff->tcz = tcz;
            openFonts = VoidPsAdd(openFonts, ff, fontsHeap);
         }
      }
   }
   return ff;
}

#define BIAS_BITS 16
#define BIAS (1<<BIAS_BITS)

typedef uint8 alpha_t;
uint8* getResizedCharPixels(Context currentContext, UserFont uf, JChar ch, int32 newWidth, int32 newHeight) // access directly the font bits and return an array of alpha only
{
   bool fSuccess = false;
   // font bits
   int32 offset = uf->bitIndexTable[ch];
   int32 width = uf->bitIndexTable[ch + 1] - offset;
   int32 height = uf->fontP.maxHeight;
   alpha_t *ob, *ib, *ob0, pval;
   uint8* tempbuf;
   int32 i, j, n, s, iweight, a;
   double xScale, yScale;

   // Temporary values
   int32 * v_weight = null; // Weight contribution    [newHeight][maxContribs]
   int32 * v_pixel = null;  // Pixel that contributes [newHeight][maxContribs]
   int32 * v_count = null;  // How many contribution for the pixel [newHeight]
   int32 * v_wsum = null;   // Sum of weights [newHeight]

   uint8 * tb;        // Temporary (intermediate buffer)

   double center;         // Center of current sampling
   double weight;         // Current wight
   int32 left;           // Left of current sampling
   int32 right;          // Right of current sampling

   int32 * p_weight;     // Temporary pointer
   int32 * p_pixel;      // Temporary pointer

   int32 maxContribs, maxContribsXY;   // Almost-const: max number of contribution for current sampling
   double scaledRadius, scaledRadiusY;   // Almost-const: scaled radius for downsampling operations
   double filterFactor;   // Almost-const: filter factor for downsampling operations
   CharSizeCache csc;
   VoidPs *csclist;

   IF_HEAP_ERROR(fontsHeap)
   {
      goto Cleanup;
   }

   // check if its in the cache
   csclist = uf->charSizeCache;
   if (uf->charSizeCache != null)
   do
   {
      CharSizeCache csc = (CharSizeCache)csclist->value;
      if (csc->ch == ch && csc->size == newHeight)
         return csc->alpha;
      csclist = csclist->next;
   } while (csclist != uf->charSizeCache);

   xScale = ((double)newWidth / width);
   yScale = ((double)newHeight / height);

   ob0 = ob = (alpha_t*)heapAlloc(fontsHeap, newWidth * newHeight);
   ib = (alpha_t*)&uf->bitmapTable[offset];

   if (newWidth > width)
   {
      /* Horizontal upsampling */
      filterFactor = 1.0;
      scaledRadius = 2;
   }
   else
   {
      /* Horizontal downsampling */
      filterFactor = xScale;
      scaledRadius = 2 / xScale;
   }

   /* The maximum number of contributions for a target pixel */
   maxContribs = (int32)(2 * scaledRadius + 1);

   scaledRadiusY = yScale > 1.0 ? 2 : 2 / yScale;
   maxContribsXY = (int32)(2 * (scaledRadiusY > scaledRadius ? scaledRadiusY : scaledRadius) + 1);

   /* Pre-allocating all of the needed memory */
   s = max32(newWidth, newHeight);
   i = (uf->fontP.maxWidth * newHeight / uf->fontP.maxHeight + 1) * height + 2 * s * maxContribsXY * sizeof(int32)+2 * s * sizeof(int32);
   if (uf->tempbufssize >= i)
      xmemzero(uf->tempbufs, uf->tempbufssize);
   else
   {
      xfree(uf->tempbufs); uf->tempbufssize = 0;
      uf->tempbufs = xmalloc(i);
      if (!uf->tempbufs) goto Cleanup;
      uf->tempbufssize = i;
   }
   tempbuf = uf->tempbufs;
   tb = (alpha_t *)tempbuf; tempbuf += newWidth * height;
   v_weight = (int32 *)tempbuf; tempbuf += s * maxContribsXY * sizeof(int32); /* weights */
   v_pixel = (int32 *)tempbuf; tempbuf += s * maxContribsXY * sizeof(int32); /* the contributing pixels */
   v_count = (int32 *)tempbuf; tempbuf += s * sizeof(int32); /* how many contributions for the target pixel */
   v_wsum = (int32 *)tempbuf; tempbuf += s * sizeof(int32); /* sum of the weights for the target pixel */

   /* Pre-calculate weights contribution for a row */
   for (i = 0; i < newWidth; i++)
   {
      p_weight = v_weight + i * maxContribs;
      p_pixel = v_pixel + i * maxContribs;

      center = ((double)i) / xScale;
      left = (int32)((center + .5) - scaledRadius);
      right = (int32)(left + 2 * scaledRadius);

      for (j = left; j <= right; j++)
      {
         double cc;
         if (j < 0 || j >= width)
            continue;
         // Catmull-rom resampling
         cc = (center - j) * filterFactor;
         if (cc < 0.0) cc = -cc;
         if (cc <= 1.0) weight = 1.5 * cc * cc * cc - 2.5 * cc * cc + 1; else
         if (cc <= 2.0) weight = -0.5 * cc * cc * cc + 2.5 * cc * cc - 4 * cc + 2;
         else continue;
         if (weight == 0)
            continue;
         iweight = (int32)(weight * BIAS);

         n = v_count[i]; // Since v_count[i] is our current index
         p_pixel[n] = j;
         p_weight[n] = iweight;
         v_wsum[i] += iweight;
         v_count[i]++; // Increment contribution count
      }
   }

   /* Filter horizontally from input to temporary buffer */
   for (i = 0; i < newWidth; i++)
   {
      int32 wsum = v_wsum[i];
      int32 count = v_count[i];
      for (n = 0; n < height; n++)
      {
         p_weight = v_weight + i * maxContribs;
         p_pixel = v_pixel + i * maxContribs;

         a = 0;
         for (j = 0; j < count; j++)
         {
            int32 iweight = *p_weight++;
            pval = ib[*p_pixel++ + n * uf->rowWidthInBytes];
            // Acting on color components
            a += (int32)pval * iweight;
         }
         a /= wsum; if (a > 255) a = 255; else if (a < 0) a = 0;
         tb[i + n*newWidth] = (alpha_t)a;
      }
   }

   /* Going to vertical stuff */
   if (newHeight > height)
   {
      filterFactor = 1.0;
      scaledRadius = 2;
   }
   else
   {
      filterFactor = yScale;
      scaledRadius = 2 / yScale;
   }
   maxContribs = (int32)(2 * scaledRadius + 1);

   p_weight = v_weight;
   p_pixel = v_pixel;
   for (i = s*maxContribs; --i >= 0;)
      *p_weight++ = *p_pixel++ = 0;

   /* Pre-calculate filter contributions for a column */
   for (i = 0; i < newHeight; i++)
   {
      p_weight = v_weight + i * maxContribs;
      p_pixel = v_pixel + i * maxContribs;

      v_count[i] = 0;
      v_wsum[i] = 0;

      center = ((double)i) / yScale;
      left = (int32)(center + .5 - scaledRadius);
      right = (int32)(left + 2 * scaledRadius);

      for (j = left; j <= right; j++)
      {
         double cc;
         if (j < 0 || j >= height) continue;
         // Catmull-rom resampling
         cc = (center - j) * filterFactor;
         if (cc < 0.0) cc = -cc;
         if (cc <= 1.0) weight = 1.5 * cc * cc * cc - 2.5 * cc * cc + 1; else
         if (cc <= 2.0) weight = -0.5 * cc * cc * cc + 2.5 * cc * cc - 4 * cc + 2;
         else continue;
         if (weight == 0)
            continue;
         iweight = (int32)(weight * BIAS);

         n = v_count[i]; /* Our current index */
         p_pixel[n] = j;
         p_weight[n] = iweight;
         v_wsum[i] += iweight;
         v_count[i]++; /* Increment the contribution count */
      }
   }

   /* Filter vertically from work to output */
   for (i = 0; i < newHeight; i++)
   {
      int32 wsum = v_wsum[i];
      int32 count = v_count[i];
      for (n = 0; n < newWidth; n++)
      {
         p_weight = v_weight + i * maxContribs;
         p_pixel = v_pixel + i * maxContribs;

         a = 0;
         for (j = 0; j < count; j++)
         {
            int iweight = *p_weight++;
            pval = tb[n + newWidth * *p_pixel++]; // Using val as temporary storage 
            // Acting on color components 
            a += (int32)pval * iweight;
         }
         a /= wsum; if (a > 160) a = 255; else if (a < 0) a = 0; // changed 255 to 160 to make the font darker
         *ob++ = a;
      }
   }

   // add to the cache
   csc = newXH(CharSizeCache, fontsHeap);
   csc->ch = ch;
   csc->size = newHeight;
   csc->alpha = ob0;
   uf->charSizeCache = VoidPsAdd(uf->charSizeCache, csc, fontsHeap);
   fSuccess = true;

Cleanup: /* CLEANUP */
   if (!fSuccess) throwException(currentContext, OutOfMemoryError, "Cannot create font buffers");
   return fSuccess ? ob0 : null;
}

#ifdef __gl2_h_
int32 getCharTexture(Context currentContext, UserFont uf, JChar ch)
{
   int32 *id = &uf->textureIds[ch];
   if (*id == 0)
   {
      PixelConv* pixels = (PixelConv*)uf->charPixels, *p = pixels;
      int32 offset = uf->bitIndexTable[ch], y, x, idx;
      int32 width = uf->bitIndexTable[ch + 1] - offset, height = uf->fontP.maxHeight;
      p += width; // skip a line at top
      bool isLow = uf->fontP.maxHeight < 18;
      for (y = 0; y < height; y++)
      {
         uint8* alpha = &uf->bitmapTable[y * uf->rowWidthInBytes + offset];
         if (isLow) // for low res fonts, make it darker
            for (x = 0; x < width; x++, p++, alpha++)
               p->a = *alpha < 160 ? *alpha : 255;
         else
            for (x = 0; x < width; x++, p++, alpha++)
               p->a = *alpha;
         p->a = 0; p++; // skip a row at right
      }
      glLoadTexture(currentContext, null, id, pixels, width+1, height+1, false);
   }
   return *id;
}
#endif

#ifdef __gl2_h_
static void reset1Font(UserFont uf)
{
   int32 i;
   for (i = uf->fontP.lastChar - uf->fontP.firstChar + 1; --i >= 0;) 
      uf->textureIds[i] = 0;
}
#endif

void resetFontTexture()
{       
   #ifdef __gl2_h_
   int32 j;
   for (j = 0; j < SIZE_LEN; j++)
   {  
      reset1Font(baseFontN[j]);
      reset1Font(baseFontB[j]);
   }
   #endif
}

static UserFont getBaseFont(bool bold, int32 size)
{
   int32 i;
   for (i = 0; i < SIZE_LEN-1; i++)
      if (size <= realSizes[i])
         break;
   return bold ? baseFontB[i] : baseFontN[i];
}

UserFont loadUserFont(Context currentContext, FontFile ff, bool bold, int32 size, JChar c)
{
   char fullname[100];
   UserFont uf;
   uint32 bitmapTableSize, bitIndexTableSize, numberOfChars, uIndex, hash;
   int32 nlen, vsize, i;
   TCZFile uftcz;
   char faceType;

   IF_HEAP_ERROR(fontsHeap)
   {
      //heapDestroy(fontsHeap); - guich@tc114_63 - not a good idea; just return null
      return null;
   }

   nlen = 0;
   vsize = (size == -1) ? normalFontSize : max32(size, minFontSize); // guich@tc122_15: don't check for the maximum font size here

   faceType = c < 0x3000 && bold ? 'b' : 'p';
   uIndex = ((int32)c >> 8) << 8;
   xstrprintf(fullname, "%s$%c%du%d", ff->name, faceType, vsize, uIndex);

   // verify if its in the cache.
   hash = hashCode(fullname);
   uf = htGetPtr(&htUF, hash);
   if (uf != null)
      return uf;

   // in opengl, if using the default system font, create an alias of the default font size that will be resized in realtime
   if (!useRealFont && baseFontN[0] != null && c <= 255 && xstrlen(ff->name) == xstrlen(defaultFontName) && xstrncasecmp(ff->name, defaultFontName, xstrlen(defaultFontName)) == 0)
   {
      UserFont ubase = getBaseFont(bold, size);
      int32 ubaseH = ubase->fontP.maxHeight;
      uf = newXH(UserFont, fontsHeap);
      // take a copy of the font file
      xmemmove(uf, ubase, sizeof(TUserFont));
      // change some fields to match the target size
      uf->ubase = ubase;
      uf->fontP.maxHeight = size;
      uf->fontP.ascent = ubase->fontP.ascent     * size / ubaseH;
      uf->fontP.descent = size - uf->fontP.ascent;
      uf->fontP.maxWidth = ubase->fontP.maxWidth   * size / ubaseH;
      uf->fontP.spaceWidth = ubase->fontP.spaceWidth * size / ubaseH;
      // uf->rowWidthInBytes  = 0; - dont change this field since it will use the base texture
      htPutPtr(&htUF, hash, uf);
      return uf;
   }

   // first, try to load it
   uftcz = tczFindName(ff->tcz, fullname);
   if (uftcz == null)
   {
      nlen = xstrlen(ff->name);
      // try now as a plain font
      xstrprintf(&fullname[nlen], "$p%du%d", vsize, uIndex);
      uftcz = tczFindName(ff->tcz, fullname);
   }
   if (uftcz == null && vsize != normalFontSize) // guich@tc122_15: ... check only here
   {
      i = vsize;
      while (uftcz == null && --i >= 5) // try to find the nearest size
      {
         xstrprintf(&fullname[nlen], "$%c%du%d", faceType, i, uIndex);
         uftcz = tczFindName(ff->tcz, fullname);
      }
   }
   if (uftcz == null)
   {
      // try now as the default size original face
      xstrprintf(&fullname[nlen], "$%c%du%d", faceType, normalFontSize, uIndex);
      uftcz = tczFindName(ff->tcz, fullname);
   }
   if (uftcz == null && faceType != 'p')
   {
      // try now as the default size plain font
      xstrprintf(&fullname[nlen], "$p%du%d", normalFontSize, uIndex);
      uftcz = tczFindName(ff->tcz, fullname);
   }
   // at last, use the default font - guich@tc123_11: fixed these font checks
   if (uftcz == null && defaultFont != null)
   {
      xstrprintf(fullname, "tcfont$%c%du%d", faceType, vsize, uIndex);
      uftcz = tczFindName(defaultFont->tcz, fullname);
   }
   if (uftcz == null) // check if there's a font of any size - maybe the file has only one font?
   for (i = minFontSize; i <= maxFontSize; i++)
   {
      xstrprintf(fullname, "%s$p%du%d", ff->name, i, uIndex);
      if ((uftcz = tczFindName(ff->tcz, fullname)) != null)
         break;
   }
   if (uftcz == null) // check if there's a font of any size - at least with the default font
   for (i = minFontSize; i <= maxFontSize; i++)
   {
      xstrprintf(fullname, "tcfont$p%du%d", i, uIndex);
      if ((uftcz = tczFindName(defaultFont->tcz, fullname)) != null)
         break;
   }

   if (uftcz == null) // probably the index was outside the available ranges at this font
      return c == ' ' ? null : loadUserFont(currentContext, ff, bold, size, ' '); // guich@tc110_28: if space, just return null

   uf = newXH(UserFont, fontsHeap);
   uftcz->tempHeap = fontsHeap; // guich@tc114_63: use the fontsHeap
   tczRead(uftcz, &uf->fontP, 2 * 10);

   uf->rowWidthInBytes = 2 * (uint32)uf->fontP.rowWords * (uf->fontP.antialiased == AA_NO ? 1 : uf->fontP.antialiased == AA_4BPP ? 4 : 8);
   numberOfChars = uf->fontP.lastChar - uf->fontP.firstChar + 1;
   bitmapTableSize = ((uint32)uf->rowWidthInBytes) * uf->fontP.maxHeight;
   bitIndexTableSize = (numberOfChars + 1) * 2;
   uf->bitmapTable = newPtrArrayOf(UInt8, bitmapTableSize, fontsHeap);
   uf->bitIndexTable = newPtrArrayOf(UInt16, bitIndexTableSize >> 1, fontsHeap);
   tczRead(uftcz, uf->bitmapTable, bitmapTableSize);
   tczRead(uftcz, uf->bitIndexTable, bitIndexTableSize);
   uf->bitIndexTable -= uf->fontP.firstChar; // instead of doing "bitIndexTable[ch-firstChar]", this trick will allow use "bitIndexTable[ch]
   if (uf->fontP.antialiased == AA_8BPP) // glfont - create the texture
   {
#ifdef __gl2_h_
      uf->textureIds = newPtrArrayOf(Int32, numberOfChars, fontsHeap);
#endif
      uf->charPixels = newPtrArrayOf(Int32, (uf->fontP.maxWidth + 1) * (uf->fontP.maxHeight + 1), fontsHeap);
   }

   tczClose(uftcz);
   if (!useRealFont)
      htPutPtr(&htUF, hash, uf);
   return uf;
}

UserFont loadUserFontFromFontObj(Context currentContext, Object fontObj, JChar ch)
{
   if (fontObj == currentContext->lastFontObj && ch == ' ')
      return currentContext->lastUF;
   else
   {
      FontFile ff = null;
      int32 style = Font_style(fontObj);
      int32 size = Font_size(fontObj);
      UserFont uf;
      xmoveptr(&ff, ARRAYOBJ_START(Font_hvUserFont(fontObj)));
      uf = loadUserFont(currentContext, ff, (style & 1) == 1, size, ch);
      if (uf == null) // guich@tc123_11: use the last available font
         uf = currentContext->lastUF;
      if (uf != null && ch == ' ') // only cache ' ', usually used to get a pointer to the user font
      {
         currentContext->lastUF = uf;
         currentContext->lastFontObj = fontObj;
      }
      return uf;
   }
}

int32 getJCharWidth(Context currentContext, Object fontObj, JChar ch)
{
   UserFont uf = loadUserFontFromFontObj(currentContext, fontObj, ch);
   if (ch == 160) // guich@tc153: now the char 160 have the same width of a number
      ch = '0';
   if (ch < ' ') // guich@tc126_22: since enter can be inside the range of this font, we have to handle it before and make sure its width is 0.
      return (ch == '\t') ? uf->fontP.spaceWidth * *tabSizeField : 0; // guich@tc100: handle tabs
   if (uf == null || ch < uf->fontP.firstChar || ch > uf->fontP.lastChar) // invalid char - guich@tc122_23: must also check the font's range
      return ch == ' ' ? 0 : getJCharWidth(currentContext, fontObj, ' ');
   if (uf->fontP.firstChar <= ch && ch <= uf->fontP.lastChar)
   {
      int32 r = uf->bitIndexTable[ch + 1] - uf->bitIndexTable[ch];
      if (uf->ubase != null) // an inherited font?
         r = r * uf->fontP.maxHeight / uf->ubase->fontP.maxHeight;
      return r;
   }
   else
      return uf->fontP.spaceWidth;
}

int32 getJCharPWidth(Context currentContext, Object fontObj, JCharP s, int32 len)
{
   int sum = 0;
   while (len-- > 0)
      sum += getJCharWidth(currentContext, fontObj, *s++);
   return sum;
}
