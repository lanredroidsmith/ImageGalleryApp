package com.lanredroidsmith.imagegalleryapp.data.local.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Lanre on 1/3/18.
 */
data class Album(
        val name: String,
        val id: Int,
        var mediaCount: Int,
        val latestMediaFile: String) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(id)
        parcel.writeInt(mediaCount)
        parcel.writeString(latestMediaFile)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Album> {
        override fun createFromParcel(parcel: Parcel): Album {
            return Album(parcel)
        }

        override fun newArray(size: Int): Array<Album?> {
            return arrayOfNulls(size)
        }
    }
}