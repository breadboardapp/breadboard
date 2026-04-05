package moe.apex.breadboard.history

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.tag.TagSuggestion


@Serializable
data class SearchHistoryEntry(
    val timestamp: Long,
    val source: ImageSource,
    val tags: Set<TagSuggestion>,
    val ratings: Set<ImageRating>
)


@OptIn(ExperimentalSerializationApi::class)
fun List<SearchHistoryEntry>.encodeToByteArray(): ByteArray {
    return Cbor.encodeToByteArray(this)
}