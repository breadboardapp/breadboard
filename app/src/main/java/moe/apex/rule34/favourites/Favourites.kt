package moe.apex.rule34.favourites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.rule34.detailview.ImageGrid
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.PreferenceKeys
import moe.apex.rule34.prefs
import moe.apex.rule34.util.AnimatedVisibilityLargeImageView
import moe.apex.rule34.util.HorizontallyScrollingChipsWithLabels
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.bottomAppBarAndNavBarHeight
import moe.apex.rule34.util.onScroll


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesPage(navController: NavController, bottomBarVisibleState: MutableState<Boolean>) {
    val prefs = LocalPreferences.current
    val preferencesRepository = LocalContext.current.prefs
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val staggeredGridState = rememberLazyStaggeredGridState()
    val uniformGridState = rememberLazyGridState()

    val images = prefs.favouriteImages.reversed().filter {
        it.imageSource in prefs.favouritesFilter
        &&
        if (it.metadata?.rating == null) ImageRating.UNKNOWN in prefs.favouritesRatingsFilter
        else it.metadata.rating in prefs.favouritesRatingsFilter
    }

    val chips = mutableListOf<List<@Composable () -> Unit>>()
    chips.add(ImageSource.entries.map { {
        FilterChip(
            selected = it in prefs.favouritesFilter,
            label = { Text(it.label) },
            onClick = {
                scope.launch {
                    if (it in prefs.favouritesFilter) {
                        preferencesRepository.removeFromSet(PreferenceKeys.FAVOURITES_FILTER, it)
                    } else {
                        preferencesRepository.addToSet(PreferenceKeys.FAVOURITES_FILTER, it)
                    }
                }
            }
        )
    } })
    chips.add(ImageRating.entries.map { {
        FilterChip(
            selected = it in prefs.favouritesRatingsFilter,
            label = { Text(it.label) },
            onClick = {
                scope.launch {
                    if (it in prefs.favouritesRatingsFilter) {
                        preferencesRepository.removeFromSet(PreferenceKeys.FAVOURITES_RATING_FILTER, it)
                    } else {
                        preferencesRepository.addToSet(PreferenceKeys.FAVOURITES_RATING_FILTER, it)
                    }
                }
            }
        )
    } })

    MainScreenScaffold("Favourite images", scrollBehavior, addBottomPadding = false) { padding ->
        ImageGrid(
            modifier = Modifier
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .onScroll(staggeredGridState) { bottomBarVisibleState.value = !it.lastScrolledForward }
                .onScroll(uniformGridState) { bottomBarVisibleState.value = !it.lastScrolledForward },
            staggeredGridState = staggeredGridState,
            uniformGridState = uniformGridState,
            images = images,
            onImageClick = { index, _ ->
                initialPage = index
                shouldShowLargeImage.value = true
            },
            contentPadding = PaddingValues(top = SMALL_LARGE_SPACER.dp, start = SMALL_LARGE_SPACER.dp, end = SMALL_LARGE_SPACER.dp, bottom = bottomAppBarAndNavBarHeight),
            filterComposable = {
                HorizontallyScrollingChipsWithLabels(
                    modifier = Modifier.padding(bottom = 4.dp),
                    labels = listOf("Sources", "Ratings"),
                    content = chips
                )
            }
        )
    }

    AnimatedVisibilityLargeImageView(navController, shouldShowLargeImage, initialPage, images, bottomBarVisibleState)
}
