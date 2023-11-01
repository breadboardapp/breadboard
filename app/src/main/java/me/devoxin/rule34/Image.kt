package me.devoxin.rule34

import android.os.Parcel
import android.os.Parcelable

class Image(
    val fileName: String,
    val fileFormat: String,
    val previewUrl: String,
    val fileUrl: String,
    val sampleUrl: String
) : Parcelable {
    val defaultUrl = sampleUrl.takeIf { it.isNotEmpty() } ?: fileUrl
    val hdAvailable = sampleUrl.isNotEmpty() && fileUrl.isNotEmpty() && sampleUrl != fileUrl
    val highestQualityFormatUrl = fileUrl.takeIf { it.isNotEmpty() } ?: sampleUrl

    constructor(parcel: Parcel) : this(parcel.nextString(), parcel.nextString(), parcel.nextString(), parcel.nextString(), parcel.nextString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(fileName)
        parcel.writeString(fileFormat)
        parcel.writeString(previewUrl)
        parcel.writeString(fileUrl)
        parcel.writeString(sampleUrl)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Image> {
        override fun createFromParcel(parcel: Parcel): Image {
            return Image(parcel)
        }

        override fun newArray(size: Int): Array<Image?> {
            return arrayOfNulls(size)
        }

        fun Parcel.nextString(): String = readString()!!
    }
}
