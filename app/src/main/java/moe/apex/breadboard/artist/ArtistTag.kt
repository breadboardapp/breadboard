package moe.apex.breadboard.artist

import androidx.compose.runtime.Immutable


@Immutable
data class ArtistTag(
    val name: String,
    val postCount: Int
)
