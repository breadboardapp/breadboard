package moe.apex.rule34.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.apex.rule34.detailview.ImageGrid
import moe.apex.rule34.preferences.Experiment
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.util.AnimatedVisibilityLargeImageView
import moe.apex.rule34.util.OffsetBasedLargeImageView
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.RecommendationsProvider
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.SMALL_SPACER
import moe.apex.rule34.util.ScrollToTopArrow
import moe.apex.rule34.util.bottomAppBarAndNavBarHeight
import moe.apex.rule34.util.onScroll
import moe.apex.rule34.util.rememberPullToRefreshController
import moe.apex.rule34.viewmodel.BreadboardViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: BreadboardViewModel,
    bottomBarVisibleState: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val blockedTags by rememberUpdatedState(prefs.blockedTags)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }

    if (viewModel.recommendationsProvider == null) {
        viewModel.recommendationsProvider = RecommendationsProvider(
            seedImages = prefs.favouriteImages,
            imageSource = prefs.imageSource,
            auth = prefs.authFor(prefs.imageSource, context),
            showAllRatings = prefs.recommendAllRatings,
            filterRatingsLocally = prefs.filterRatingsLocally,
            initialBlockedTags = prefs.blockedTags
        )
        viewModel.recommendationsProvider!!.prepareRecommendedTags()
    }
    val recommendationsProvider = viewModel.recommendationsProvider!!
    val pullToRefreshController = rememberPullToRefreshController(initialValue = false) {
        recommendationsProvider.replaceBlockedTags(blockedTags)
        recommendationsProvider.prepareRecommendedTags()
        recommendationsProvider.recommendImages()
        recommendationsProvider.resetGridStates()
    }

    MainScreenScaffold(
        title = "Breadboard",
        largeTopBar = false,
        scrollBehavior = scrollBehavior,
        addBottomPadding = false,
        blur = shouldShowLargeImage.value,
        additionalActions = {
            ScrollToTopArrow(
                staggeredGridState = recommendationsProvider.staggeredGridState,
                uniformGridState = recommendationsProvider.uniformGridState
            ) {
                scrollBehavior.state.contentOffset = 0f
            }
        }
    ) { padding ->
        ImageGrid(
            modifier = Modifier
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .onScroll(recommendationsProvider.staggeredGridState) {
                    bottomBarVisibleState.value = !it.lastScrolledForward
                }
                .onScroll(recommendationsProvider.uniformGridState) {
                    bottomBarVisibleState.value = !it.lastScrolledForward
                },
            staggeredGridState = recommendationsProvider.staggeredGridState,
            uniformGridState = recommendationsProvider.uniformGridState,
            images = recommendationsProvider.recommendedImages,
            noImagesContent = {
                if (!recommendationsProvider.doneInitialLoad) {
                    return@ImageGrid
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No recommendations right now.")
                    TextButton(
                        onClick = {
                            pullToRefreshController.refresh(animate = true)
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(SMALL_SPACER.dp))
                        Text("Refresh")
                    }
                }
            },
            onImageClick = { index, _ ->
                initialPage = index
                shouldShowLargeImage.value = true
            },
            contentPadding = PaddingValues(
                start = SMALL_LARGE_SPACER.dp,
                end = SMALL_LARGE_SPACER.dp,
                top = SMALL_LARGE_SPACER.dp,
                bottom = bottomAppBarAndNavBarHeight
            ),
            pullToRefreshController = pullToRefreshController,
            doneInitialLoad = recommendationsProvider.doneInitialLoad,
            onEndReached = { recommendationsProvider.recommendImages() },
        )
    }

    if (Experiment.IMAGE_CAROUSEL_REWORK.isEnabled()) {
        OffsetBasedLargeImageView(navController, shouldShowLargeImage, initialPage, recommendationsProvider.recommendedImages, bottomBarVisibleState)
    } else {
        AnimatedVisibilityLargeImageView(navController, shouldShowLargeImage, initialPage, recommendationsProvider.recommendedImages, bottomBarVisibleState)
    }
}
