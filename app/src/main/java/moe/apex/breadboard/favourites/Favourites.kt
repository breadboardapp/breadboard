package moe.apex.breadboard.favourites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.breadboard.detailview.ImageGrid
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.preferences.Experiment
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.preferences.PreferenceKeys
import moe.apex.breadboard.prefs
import moe.apex.breadboard.largeimageview.OffsetBasedLargeImageView
import moe.apex.breadboard.util.HorizontallyScrollingChipsWithLabels
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.ScrollToTopArrow
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.bottomAppBarAndNavBarHeight
import moe.apex.breadboard.util.filterChipSolidColor
import moe.apex.breadboard.util.onScroll
import moe.apex.breadboard.viewmodel.FavouritesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesPage(
    navController: NavController,
    viewModel: FavouritesViewModel = viewModel(),
    navBarVisibilityCallback: (Boolean) -> Unit = { }
) {
    val prefs = LocalPreferences.current
    val preferencesRepository = LocalContext.current.prefs
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    var isImageCarouselVisible by remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val blur = prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS)

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
            colors = filterChipSolidColor,
            border = null,
            onClick = {
                scope.launch {
                    if (it in prefs.favouritesFilter) {
                        preferencesRepository.removeFromSet(
                            key = PreferenceKeys.FAVOURITES_FILTER,
                            item = it,
                            default = prefs.favouritesFilter
                        )
                    } else {
                        preferencesRepository.addToSet(
                            key = PreferenceKeys.FAVOURITES_FILTER,
                            item = it,
                            default = prefs.favouritesFilter
                        )
                    }
                }
            }
        )
    } })
    chips.add(ImageRating.entries.map { {
        FilterChip(
            selected = it in prefs.favouritesRatingsFilter,
            label = { Text(it.label) },
            colors = filterChipSolidColor,
            border = null,
            onClick = {
                scope.launch {
                    if (it in prefs.favouritesRatingsFilter) {
                        preferencesRepository.removeFromSet(
                            key = PreferenceKeys.FAVOURITES_RATING_FILTER,
                            item = it,
                            default = prefs.favouritesRatingsFilter
                        )
                    } else {
                        preferencesRepository.addToSet(
                            key = PreferenceKeys.FAVOURITES_RATING_FILTER,
                            item = it,
                            default = prefs.favouritesRatingsFilter
                        )
                    }
                }
            }
        )
    } })

    MainScreenScaffold(
        title = "Favourite images",
        scrollBehavior = scrollBehavior,
        addBottomPadding = false,
        blur = isImageCarouselVisible && blur,
        additionalActions = {
            ScrollToTopArrow(
                staggeredGridState = viewModel.staggeredGridState,
                uniformGridState = viewModel.uniformGridState,
                animate = Experiment.ALWAYS_ANIMATE_SCROLL.isEnabled()
            ) {
                navBarVisibilityCallback(true)
            }
        }
    ) { padding ->
        ImageGrid(
            modifier = Modifier
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .onScroll(viewModel.staggeredGridState) {
                    navBarVisibilityCallback(!it.lastScrolledForward)
                }
                .onScroll(viewModel.uniformGridState) {
                    navBarVisibilityCallback(!it.lastScrolledForward)
                  },
            staggeredGridState = viewModel.staggeredGridState,
            uniformGridState = viewModel.uniformGridState,
            images = images,
            onImageClick = { index, _ ->
                Snapshot.withMutableSnapshot {
                    initialPage = index
                    isImageCarouselVisible = true
                }
            },
            contentPadding = PaddingValues(top = SMALL_LARGE_SPACER.dp, start = SMALL_LARGE_SPACER.dp, end = SMALL_LARGE_SPACER.dp, bottom = bottomAppBarAndNavBarHeight),
            filterComposable = {
                HorizontallyScrollingChipsWithLabels(
                    modifier = Modifier.padding(bottom = TINY_SPACER.dp),
                    labels = listOf("Sources", "Ratings"),
                    content = chips
                )
            }
        )
    }

    OffsetBasedLargeImageView(
        navController = navController,
        isActive = isImageCarouselVisible,
        initialPage = initialPage,
        allImages = images,
        onActiveStateChanged = {
            isImageCarouselVisible = it
            navBarVisibilityCallback(!it)
        }
    ) { oldImage, newImage ->
        preferencesRepository.updateFavouriteImage(oldImage, newImage)
    }
}
