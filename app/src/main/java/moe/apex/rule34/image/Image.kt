package moe.apex.rule34.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.tag.TagGroup
import moe.apex.rule34.util.MigrationOnlyField


@Serializable
data class ImageMetadata(
    val parentId: String? = null,
    val hasChildren: Boolean? = null,
    @MigrationOnlyField
    @SerialName("tags")
    @Deprecated(
        message = "Do not use this outside of migrations.",
        replaceWith = ReplaceWith("tags")
    )
    val uncategorisedTags: List<String>? = null,
    val groupedTags: List<TagGroup> = emptyList(),
    val rating: ImageRating,
    val pixivId: Int? = null,
    @MigrationOnlyField
    @SerialName("artist")
    @Deprecated(
        message = "Do not use this outside of migrations.",
        replaceWith = ReplaceWith("artist")
    )
    val artistsString: String? = null,
    val artists: List<String> = emptyList(),
    val source: String? = null,
) {
    /** A non-categorised list of tags. [groupedTags] should be preferred in most cases. */
    val tags: List<String>
        get() = groupedTags.fold(emptyList()) { acc, tagGroup -> acc + tagGroup.tags }

    val pixivUrl: String?
        get() = pixivId?.let { "https://www.pixiv.net/en/artworks/$it" }

    @Deprecated(
        message = "Deprecated in version 300. Use `artists` to retrieve artists as a list.",
        replaceWith = ReplaceWith("artists")
    )
    val artist: String?
        get() = artists.takeIf { it.isNotEmpty() }?.joinToString(" ")
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