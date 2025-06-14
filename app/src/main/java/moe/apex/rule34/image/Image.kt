package moe.apex.rule34.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.tag.TagGroup


@Serializable
data class ImageMetadata(
    val parentId: String? = null,
    val hasChildren: Boolean? = null,
    val artist: String? = null,
    val source: String? = null,
    @Deprecated(
        message = "Deprecated in version 251. Use `allTags` for identical functionality, or `groupedTags` to retrieve tags in categories.",
        replaceWith = ReplaceWith("allTags")
    )
    val tags: List<String>? = null,
    val groupedTags: List<TagGroup> = emptyList(),
    val rating: ImageRating,
    val pixivId: Int? = null,
) {
    val allTags: List<String>
        get() = groupedTags.fold(emptyList()) { acc, tagGroup -> acc + tagGroup.tags }
    val pixivUrl: String?
        get() = pixivId?.let { "https://www.pixiv.net/en/artworks/$it" }
}


@Serializable
data class Image(
    val id: String? = null,
    val fileName: String,
    val fileFormat: String,
    val previewUrl: String,
    val fileUrl: String,
    val sampleUrl: String,
    val imageSource: ImageSource = ImageSource.R34, // Backwards compatibility
    val aspectRatio: Float? = null, // Nullable for backwards compatibility with old favourites
    val metadata: ImageMetadata? = null
) {
    val highestQualityFormatUrl = fileUrl.takeIf { it.isNotEmpty() } ?: sampleUrl
    var preferHd by mutableStateOf(false)
    var hdQualityOverride: Boolean? by mutableStateOf(null)

    fun toggleHd(to: Boolean? = null) {
        preferHd = when (to) {
            null -> !preferHd
            else -> to
        }
        hdQualityOverride = preferHd
    }
}