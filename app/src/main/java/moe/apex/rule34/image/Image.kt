package moe.apex.rule34.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.tag.TagCategory
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
        replaceWith = ReplaceWith("artists",)
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
    val key: String
        get() = "${imageSource}_${id ?: fileName}"
    val highestQualityFormatUrl = fileUrl.takeIf { it.isNotEmpty() } ?: sampleUrl
    val isVideo = fileFormat == "mp4" || fileFormat == "webm"

    val hasGroupedTags: Boolean
        get() {
            if (id == null || metadata == null) return true

            /* Usually, we can tell that the existing grouped tags are grouped properly if they have more than one group.

               Otherwise, the least we could do is make sure that the only existing group is a non-general one.
               This does not usually happen, but we would at least know that it is already grouped in this case.

               The images that pass would usually have ungrouped tags, but it could also catch actual
               images that literally only have general tags. I'm not sure how to work around this. */
            if (
                metadata.groupedTags.size > 1 ||
                (metadata.groupedTags.size == 1 && metadata.groupedTags[0].category != TagCategory.GENERAL)
            )
                return true

            return false
        }
}