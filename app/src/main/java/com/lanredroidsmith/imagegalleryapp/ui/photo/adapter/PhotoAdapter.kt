package com.lanredroidsmith.imagegalleryapp.ui.photo.adapter

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.lanredroidsmith.imagegalleryapp.R
import com.lanredroidsmith.imagegalleryapp.data.local.model.Photo
import kotlinx.android.synthetic.main.photo_item.view.*
import java.io.File

/**
 * Created by Lanre on 1/5/18.
 */

class PhotoAdapter(private val mContext: Context,
                   private val mListener: OnPhotoSelectedListener)
    : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private val mInflater = LayoutInflater.from(mContext)
    private var mIdColumnIndex: Int = 0
    private var mFilePathColumnIndex: Int = 0
    private var mDisplayNameColumnIndex: Int = 0
    private var mDateAddedColumnIndex: Int = 0
    private var mDateModifiedColumnIndex: Int = 0
    private var mSizeColumnIndex: Int = 0
    private var mHeightColumnIndex: Int = 0
    private var mWidthColumnIndex: Int = 0
    private var mCursor: Cursor? = null

    interface OnPhotoSelectedListener {
        fun onPhotoSelected(photoView: View, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) =
        PhotoViewHolder(mInflater.inflate(R.layout.photo_item, parent, false))

    override fun getItemCount() = mCursor?.count ?: 0

    override fun onBindViewHolder(holder: PhotoViewHolder?, position: Int) {
        if (mCursor != null) {
            mCursor!!.moveToPosition(position) // get to the right location in the cursor

            val id = mCursor!!.getLong(mIdColumnIndex)
            val filePath = mCursor!!.getString(mFilePathColumnIndex)
            val displayName = mCursor!!.getString(mDisplayNameColumnIndex)
            val dateAdded = mCursor!!.getLong(mDateAddedColumnIndex)
            val dateModified = mCursor!!.getLong(mDateModifiedColumnIndex)
            val size = mCursor!!.getLong(mSizeColumnIndex)
            val height = mCursor!!.getInt(mHeightColumnIndex)
            val width = mCursor!!.getInt(mWidthColumnIndex)

            holder?.bindPhoto(Photo(id, filePath, displayName, dateAdded, dateModified, size, height, width))
        }
    }

    fun swapCursor(c: Cursor?): Cursor? {
        // check if this cursor is the same as the previous cursor (mCursor)
        if (mCursor === c) {
            return null // bc nothing has changed
        }
        val temp = mCursor
        mCursor = c // new cursor value assigned

        //check if this is a valid cursor, then update the data set
        if (c != null) {
            getColumnIndices()
            notifyDataSetChanged()
        }

        return temp
    }

    private fun getColumnIndices() {
        mIdColumnIndex = mCursor!!.getColumnIndex(MediaStore.Images.Media._ID)
        mFilePathColumnIndex = mCursor!!.getColumnIndex(MediaStore.Images.Media.DATA)
        mDisplayNameColumnIndex = mCursor!!.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        mDateAddedColumnIndex = mCursor!!.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        mDateModifiedColumnIndex = mCursor!!.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
        mSizeColumnIndex = mCursor!!.getColumnIndex(MediaStore.Images.Media.SIZE)
        mHeightColumnIndex = mCursor!!.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        mWidthColumnIndex = mCursor!!.getColumnIndex(MediaStore.Images.Media.WIDTH)
    }

    inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val mImageView: ImageView = view.findViewById(R.id.photo)
        fun bindPhoto(photo: Photo) {
            itemView.tag = photo.displayName
            ViewCompat.setTransitionName(mImageView, photo.displayName)
            Glide.with(mContext)
                    .load(File(photo.filePath))
                    .into(itemView.photo)
        }
        init {
            view.setOnClickListener { mListener.onPhotoSelected(mImageView, adapterPosition) }
        }
    }
}