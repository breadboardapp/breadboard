package moe.apex.rule34.util

import androidx.compose.runtime.Composable
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences


val availableRatingsForCurrentSource: List<ImageRating>
    @Composable
    get() =
        ImageRating.entries.filter {
            it != ImageRating.UNKNOWN &&
            if (LocalPreferences.current.imageSource == ImageSource.YANDERE) it != ImageRating.SENSITIVE else true
        }
