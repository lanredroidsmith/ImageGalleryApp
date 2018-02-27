package com.lanredroidsmith.imagegalleryapp.data.local.repo

import android.content.Context
import android.net.Uri
import android.support.v4.content.CursorLoader
import android.util.Log
import android.provider.MediaStore


/**
 * Created by Lanre on 1/15/18.
 */

class ImageRepository {

    private val _tag = ImageRepository::class.java.simpleName

    fun getAllImages(context: Context): CursorLoader? {
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.BUCKET_ID)
            val selection = "${MediaStore.MediaColumns.DATA} IS NOT NULL"
            val orderBy = "${MediaStore.Images.Media._ID} DESC" // so that the most recent comes 1st
            CursorLoader(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, null, orderBy)
        } catch (e: Exception) {
            Log.e(_tag, e.message)
            null
        }
    }

    fun getImagesInAlbum(context: Context, id: Int): CursorLoader? {
        return try {
            val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.WIDTH)
            val selection =
                    "${MediaStore.MediaColumns.DATA} IS NOT NULL AND ${MediaStore.Images.Media.BUCKET_ID}=?"
            val orderBy = "${MediaStore.Images.Media._ID} DESC" // so that the most recent comes 1st
            CursorLoader(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, arrayOf(id.toString()), orderBy)
        } catch (e: Exception) {
            Log.e(_tag, e.message)
            null
        }
    }

    fun getImageByUri(context: Context, uri: Uri): CursorLoader? {
        return try {
            val projection = arrayOf(MediaStore.Images.Media.BUCKET_ID)
            CursorLoader(context, uri, projection, null, null, null)
        } catch (e: Exception) {
            Log.e(_tag, e.message)
            null
        }
    }
}