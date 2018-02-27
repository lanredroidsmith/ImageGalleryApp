package com.lanredroidsmith.imagegalleryapp.ui.photo.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.lanredroidsmith.imagegalleryapp.R
import com.lanredroidsmith.imagegalleryapp.data.local.model.Album
import kotlinx.android.synthetic.main.album_item.view.*
import java.io.File

/**
 * Created by Lanre on 1/12/18.
 */
class AlbumAdapter(private val mContext: Context,
                   private var mAlbumsList: List<Album>?,
                   private val mListener: OnAlbumSelectedListener) :
        RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    interface OnAlbumSelectedListener {
        fun onAlbumSelected(album: Album)
    }

    private val mInflater = LayoutInflater.from(mContext)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) =
            AlbumViewHolder(mInflater.inflate(R.layout.album_item, parent, false))

    override fun getItemCount() = mAlbumsList?.size ?: 0

    override fun onBindViewHolder(holder: AlbumViewHolder?, position: Int) {
        if (mAlbumsList != null) {
            val album = mAlbumsList!![position]
            holder!!.itemView.albumName.text = album.name
            holder.itemView.photosCount.text = album.mediaCount.toString()
            Glide.with(mContext)
                 .load(File(album.latestMediaFile))
                 .into(holder.itemView.latestImage)
        }
    }

    inner class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                mListener.onAlbumSelected(mAlbumsList!![adapterPosition])
            }
        }
    }

    fun swapList(list: ArrayList<Album>) {
        mAlbumsList = list
        notifyDataSetChanged()
    }
}