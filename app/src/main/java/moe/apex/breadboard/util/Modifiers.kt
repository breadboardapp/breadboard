package moe.apex.breadboard.util

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier


@Composable
fun Modifier.onScroll(scrollableState: ScrollableState, callback: (ScrollableState) -> Unit): Modifier {
    LaunchedEffect(scrollableState.isScrollInProgress, scrollableState.lastScrolledForward) {
        if (scrollableState.isScrollInProgress) {
            callback(scrollableState)
        }
    }
    return this
}
