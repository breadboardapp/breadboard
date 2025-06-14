package moe.apex.rule34.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.rule34.detailview.ImageGrid
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.util.AnimatedVisibilityLargeImageView
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.RecommendationsProvider
import moe.apex.rule34.util.bottomAppBarAndNavBarHeight
import moe.apex.rule34.util.onScroll
import moe.apex.rule34.viewmodel.BreadboardViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: BreadboardViewModel,
    bottomBarVisibleState: MutableState<Boolean>,
) {
    val prefs = LocalPreferences.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val staggeredGridState = rememberLazyStaggeredGridState()
    val staggeredFirstItem by remember { derivedStateOf { staggeredGridState.firstVisibleItemIndex } }
    val uniformGridState = rememberLazyGridState()
    val uniformFirstItem by remember { derivedStateOf { uniformGridState.firstVisibleItemIndex } }

    if (viewModel.recommendationsProvider == null) {
        viewModel.recommendationsProvider = RecommendationsProvider(prefs.favouriteImages, prefs.imageSource, prefs.filterRatingsLocally)
        viewModel.recommendationsProvider!!.prepareRecommendedTags()
    }
    val recommendationsProvider = viewModel.recommendationsProvider!!

    MainScreenScaffold(
        title = "Breadboard",
        largeTopBar = false,
        scrollBehavior = scrollBehavior,
        addBottomPadding = false,
        additionalActions = {
            AnimatedVisibility(
                enter = fadeIn(),
                exit = fadeOut(),
                visible = staggeredFirstItem != 0 || uniformFirstItem != 0
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            staggeredGridState.animateScrollToItem(0)
                        }
                        scope.launch {
                            uniformGridState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
        }
    ) { padding ->
        ImageGrid(
            modifier = Modifier
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .onScroll(staggeredGridState) {
                    bottomBarVisibleState.value = !it.lastScrolledForward
                }
                .onScroll(uniformGridState) {
                    bottomBarVisibleState.value = !it.lastScrolledForward
                },
            onPullToRefresh = {
                it.value = true
                recommendationsProvider.prepareRecommendedTags()
                scope.launch {
                    recommendationsProvider.recommendImages()
                    it.value = false
                }
            },
            staggeredGridState = staggeredGridState,
            uniformGridState = uniformGridState,
            images = recommendationsProvider.recommendedImages,
            noImagesContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No recommendations right now.")
                    TextButton(
                        onClick = {
                            recommendationsProvider.prepareRecommendedTags()
                            scope.launch { recommendationsProvider.prepareRecommendedTags() }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh")
                    }
                }
            },
            onImageClick = { index, _ ->
                initialPage = index
                shouldShowLargeImage.value = true
            },
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = bottomAppBarAndNavBarHeight
            ),
            initialLoad = { recommendationsProvider.recommendImages() },
            onEndReached = { recommendationsProvider.recommendImages() },
        )
    }

    AnimatedVisibilityLargeImageView(navController, shouldShowLargeImage, initialPage, recommendationsProvider.recommendedImages, bottomBarVisibleState)
}
