package moe.apex.rule34.history

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.tag.TagSuggestion


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