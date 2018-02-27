package com.lanredroidsmith.imagegalleryapp.util

import android.database.Cursor

/**
 * Created by Lanre on 1/15/18.
 */


fun Cursor.getInt(columnName: String) = getInt(getColumnIndexOrThrow(columnName))