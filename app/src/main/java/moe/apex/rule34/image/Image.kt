package moe.apex.rule34.image

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.mutableStateOf

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
    val preferHd = mutableStateOf(true)

    constructor(parcel: Parcel) : this(
        parcel.nextString(),
        parcel.nextString(),
        parcel.nextString(),
        parcel.nextString(),
        parcel.nextString()
    )

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

        fun Parcel.nextString(): String = this.readString()!!
    }

    override fun hashCode(): Int {
        return defaultUrl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Image && other.defaultUrl == defaultUrl
    }

    fun toggleHd() {
        preferHd.value = !preferHd.value
    }
}