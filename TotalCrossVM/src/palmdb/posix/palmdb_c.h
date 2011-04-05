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



#include <sys/param.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include <dirent.h>
#include "xtypes.h"

// wince is 4.5x faster when using the system io, due to the FILE_FLAG_RANDOM_ACCESS flag
// this is why we have a special version not using POSIX

typedef FILE* PDBFileRef;

Err inline PDBGetLastErr()
{
   return errno;
}

bool inline PDBCreateFile(TCHARP fullPath, bool createIt, bool readOnly, PDBFileRef* fileRef)
{
   if (readOnly)
   {
      // createIt and readOnly can't coexist: readOnly takes precedence
      // Open with binary mode
      *fileRef = fopen(fullPath, TEXT("rb"));
   }
   else
   if (createIt)
   {
      // Open for update binary mode, erasing previous data
      *fileRef = fopen(fullPath, TEXT("wb+"));
#if defined(darwin) // TODO@ handle app file permissions
      if (*fileRef)
         fchmod(fileno(*fileRef), S_IRWXU | S_IRWXG | S_IRWXO);
#endif
   }
   else
   {
      // Open for update binary mode, without erasing
      *fileRef = fopen(fullPath, TEXT("rb+"));
   }
   return *fileRef != NULL;
}

bool inline PDBCloseFile(PDBFileRef fileRef)
{
   return fclose(fileRef) == 0;
}

bool inline PDBRename(TCHARP oldName, TCHARP newName)
{
   return rename(oldName, newName) == 0;
}

bool inline PDBRemove(TCHARP fileName)
{
   return remove(fileName) == 0;
}

bool inline PDBRead(PDBFileRef fileRef, VoidP buf, int32 size, int32* read)
{
   return (*read = fread(buf, 1, size, fileRef)) > 0;
}

bool inline PDBReadAt(PDBFileRef fileRef, VoidP buf, int32 size, int32 offset, int32* read)
{
   return fseek(fileRef, offset, SEEK_SET) == 0 && (*read = fread(buf, 1, size, fileRef)) > 0; // guich@tc122_53: don't test for the amount of bytes read - no other platforms do this, and will make it impossible to delete an empty pdb
}

bool inline PDBWrite(PDBFileRef fileRef, VoidP buf, int32 size, int32* written)
{
   return (*written = fwrite(buf, 1, size, fileRef)) > 0;
}

bool inline PDBWriteAt(PDBFileRef fileRef, VoidP buf, int32 size, int32 offset, int32* written)
{
   return fseek(fileRef, offset, SEEK_SET) == 0 && (*written = fwrite(buf, 1, size, fileRef)) > 0; // guich@tc122_53
}

bool inline PDBGetFileSize (PDBFileRef fileRef, int32* size)
{
   // Record the current position
   uint32 pos = ftell(fileRef);
   if ((int)pos == -1) return false;

   // Seek to the end
   if (fseek(fileRef, 0, SEEK_END) != 0) return false;

   // Re-get position; it's the size
   *size = ftell(fileRef);

   // Restore the stream pointer
   fseek(fileRef, pos, SEEK_SET);

   return ((int32)size != -1);
}

bool inline PDBGrowFileSize(PDBFileRef fileRef, int32 oldSize, int32 growSize)
{
   // Exact behavior expected ???
   // Write operations extend the file automatically
   //>>>PGR: could seek?
   uint8 byteBuf[BYTE_BUF_LEN];

   if (growSize < 0)
   {
      #if !defined __ECOS && !defined __SYMBIAN32__
         return ftruncate(fileno(fileRef), growSize + oldSize) == 0;
      #endif
   }
   else
   {
      uint32 pos = ftell(fileRef);
      if (fseek(fileRef, 0, SEEK_END) == 0)
      {
         int done = 0;
         // write any bytes
         do
         {
            int32 written;
            uint32 actualSize = (growSize > BYTE_BUF_LEN)? BYTE_BUF_LEN : growSize;
            growSize -= actualSize;
            if (!PDBWrite(fileRef, byteBuf, actualSize, &written))
               return false;
         } while (growSize > 0);
         fseek(fileRef, pos, SEEK_SET);
         //fflush(fileRef);
      }
      else
         return false;
   }
   return true;
}

bool PDBListDatabasesIn(TCHARP path, bool recursive, HandlePDBSearchProcType proc, VoidP userVars)
{
   TCHAR fullPath[MAX_PATH];
   bool stopSearch = false;
   struct dirent * entry;
   DIR * targetDirectory = opendir(path);

   if (!targetDirectory) return false;

   strcpy(fullPath, path);
   int len = tcslen(path);
   if (fullPath[len-1] != '/')
   {
      fullPath[len] = '/';
      len++;
   }

   while (!stopSearch && (entry = readdir(targetDirectory)))
   {
      int isDir;
      TCHAR const * name = entry->d_name;
      strcpy(fullPath+len, entry->d_name);

      struct stat statData;
      if (stat(fullPath, &statData) != 0) continue;  /* error: skip this */
      #ifdef __ECOS
      isDir = (statData.st_mode & __stat_mode_DIR);
      #else
      isDir = (statData.st_mode & S_IFDIR);
      #endif

      if (isDir)
      {
         if (recursive && ((name[0] != '.') || ((name[1] != '\0') && ((name[1] != '.') || (name[2] != '\0'))))) /* warning: order matters! */
            stopSearch = PDBListDatabasesIn(fullPath, recursive, proc, userVars);
      }
      else
      if (endsWithPDB((TCHAR*)entry->d_name)) // not a dir
      {
         if (proc)
            stopSearch = (*proc)(fullPath, userVars);
      }
   }
   closedir(targetDirectory);
   return stopSearch; // guich@tc110_93: return stopSearch
}

#if 0 // NOT YET IMPLEMENTED

// date and time functions

int32 leapYear(int32 y)
{
   if (y % 400 == 0) return 1;
   if (y % 100 == 0) return 0;
   if (y % 4 == 0)   return 1;
   return 0;
}

int32 daysAccrued(int32 m, int32 y)
{
   static int32 daysAc[] = {0,31,59,90,120,151,181,212,243,273,304,334,365}; // days accrued in a year
   if (leapYear(y) && m >=2)
      return daysAc[m]+1;
   return daysAc[m];
}

int32 daysInYear(int32 y)
{
   return 365 + leapYear(y);
}

int32 date2days(int32 y, int32 m, int32 d)
{
   int32 total = d; // start the count with the current day
   int32 i;
   if (m > 1)
      total += daysAccrued(m-1,y); // it is not necessary count current month neither january
   // count since last year until base year how many days have gone
   for (i = y-1; i >= 1970; i--)
      total += daysInYear(i);
   return total-1;
}

#endif

uint32 PDBGetNow()
{
   struct timeval tv;
   gettimeofday(&tv, NULL);
   return (uint32)tv.tv_sec;
}
