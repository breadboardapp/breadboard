package moe.apex.breadboard.util

import androidx.compose.runtime.Composable
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences


val availableRatingsForCurrentSource: List<ImageRating>
    @Composable
    get() = availableRatingsForSource(LocalPreferences.current.imageSource)


fun availableRatingsForSource(source: ImageSource): List<ImageRating> {
    return ImageRating.entries.filter {
        it != ImageRating.UNKNOWN && if (source == ImageSource.YANDERE) it != ImageRating.SENSITIVE else true
    }
}
