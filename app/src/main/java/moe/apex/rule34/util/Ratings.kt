package moe.apex.rule34.util

import androidx.compose.runtime.Composable
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences


val availableRatingsForCurrentSource: List<ImageRating>
    @Composable
    get() = availableRatingsForSource(LocalPreferences.current.imageSource)


fun availableRatingsForSource(source: ImageSource): List<ImageRating> {
    return ImageRating.entries.filter {
        it != ImageRating.UNKNOWN && if (source == ImageSource.YANDERE) it != ImageRating.SENSITIVE else true
    }
}
