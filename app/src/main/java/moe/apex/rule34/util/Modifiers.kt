package moe.apex.rule34.util

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier


@Composable
fun Modifier.onScroll(gridState: LazyStaggeredGridState, callback: (LazyStaggeredGridState) -> Unit): Modifier {
    LaunchedEffect(gridState.isScrollInProgress, gridState.lastScrolledForward) {
        if (gridState.isScrollInProgress) {
            callback(gridState)
        }
    }
    return this
}


@Composable
fun Modifier.onScroll(gridState: LazyGridState, callback: (LazyGridState) -> Unit): Modifier {
    LaunchedEffect(gridState.isScrollInProgress, gridState.lastScrolledForward) {
        if (gridState.isScrollInProgress) {
            callback(gridState)
        }
    }
    return this
}
