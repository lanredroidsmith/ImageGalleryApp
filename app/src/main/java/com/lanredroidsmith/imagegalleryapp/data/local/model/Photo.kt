package com.lanredroidsmith.imagegalleryapp.data.local.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Lanre on 1/21/18.
 */

data class Photo(
        val id: Long,
        val filePath: String,
        val displayName: String,
        val dateAdded: Long,
        val dateModified: Long,
        val size: Long,
        val height: Int,
        val width: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readInt(),
            parcel.readInt()) {}

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(filePath)
        parcel.writeString(displayName)
        parcel.writeLong(dateAdded)
        parcel.writeLong(dateModified)
        parcel.writeLong(size)
        parcel.writeInt(height)
        parcel.writeInt(width)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Photo> {
        override fun createFromParcel(parcel: Parcel): Photo {
            return Photo(parcel)
        }

        override fun newArray(size: Int): Array<Photo?> {
            return arrayOfNulls(size)
        }
    }

}