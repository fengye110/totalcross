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



#ifndef FILE_H
#define FILE_H

#include "tcvm.h"

#define lastVolume -1
#define INVALID_FILEPTR_VALUE 0xffffffff

enum
{
   INVALID        = 0,
   DONT_OPEN      = 1,
   READ_WRITE     = 2,
   CREATE         = 4,
   CREATE_EMPTY   = 8,
   READ_ONLY      = 16, // just for reading DebugConsole
};

enum
{
   TIME_CREATED  = 0x1,
   TIME_MODIFIED = 0x2,
   TIME_ACCESSED = 0x4,
   TIME_ALL      = 0xF
};

enum
{
   ATTR_NORMAL        = 0,
   ATTR_ARCHIVE       = 1,
   ATTR_HIDDEN        = 2,
   ATTR_READ_ONLY     = 4,
   ATTR_SYSTEM        = 8,
   INVALID_ATTR_VALUE = 0xffffffff
};

#if defined (WINCE)
 typedef struct _STORAGE_IDENTIFICATION {
   DWORD dwSize;
   DWORD dwFlags;
   DWORD dwManufactureIDOffset;
   DWORD dwSerialNumOffset;
 } STORAGE_IDENTIFICATION, *PSTORAGE_IDENTIFICATION;

 #include <winioctl.h>

 #define IOCTL_DISK_BASE FILE_DEVICE_DISK
 #define IOCTL_DISK_GET_STORAGEID CTL_CODE(IOCTL_DISK_BASE, 0x709, METHOD_BUFFERED, FILE_ANY_ACCESS)
#endif // WINCE

static bool fileIsDir(TCHARP path, int32 slot);
TC_API bool validatePath(TCHARP path);
typedef bool (*validatePathFunc)(TCHARP path);
#endif
