package moe.apex.rule34.favourites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import moe.apex.rule34.detailview.ImageGrid
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.util.AnimatedVisibilityLargeImageView
import moe.apex.rule34.util.MainScreenScaffold


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesPage(bottomBarVisibleState: MutableState<Boolean>) {
    val prefs = LocalPreferences.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }

    val images = prefs.favouriteImages.reversed().filter {
        it.imageSource in prefs.favouritesFilter
        &&
        if (it.metadata?.rating == null) ImageRating.UNKNOWN in prefs.favouritesRatingsFilter
        else it.metadata.rating in prefs.favouritesRatingsFilter
    }

    MainScreenScaffold("Favourite images", scrollBehavior) { padding ->
        ImageGrid(
            modifier = Modifier
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            images = images,
            onImageClick = { index, image ->
                bottomBarVisibleState.value = false
                initialPage = index
                shouldShowLargeImage.value = true
            },
            contentPadding = PaddingValues(top = 8.dp, start = 16.dp, end = 16.dp),
            showSourceFilter = true,
            showRatingFilter = true
        )
    }
    AnimatedVisibilityLargeImageView(shouldShowLargeImage, initialPage, images, bottomBarVisibleState)
}
