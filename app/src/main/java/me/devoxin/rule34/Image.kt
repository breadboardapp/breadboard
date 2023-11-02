package me.devoxin.rule34

class Image(
    val fileName: String,
    val fileFormat: String,
    val previewUrl: String,
    fileUrl: String,
    sampleUrl: String
) {
    val defaultUrl = sampleUrl.takeIf { it.isNotEmpty() } ?: fileUrl
    val hdAvailable = sampleUrl.isNotEmpty() && fileUrl.isNotEmpty() && sampleUrl != fileUrl
    val highestQualityFormatUrl = fileUrl.takeIf { it.isNotEmpty() } ?: sampleUrl
}
