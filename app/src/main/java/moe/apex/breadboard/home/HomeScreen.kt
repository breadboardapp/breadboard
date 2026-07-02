package moe.apex.breadboard.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.apex.breadboard.detailview.FlexibleImageGrid
import moe.apex.breadboard.detailview.FlexibleImageGridDefaults
import moe.apex.breadboard.preferences.Experiment
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.largeimageview.OffsetBasedLargeImageView
import moe.apex.breadboard.tag.IgnoredTagsHelper
import moe.apex.breadboard.ui.theme.BreadboardTheme
import moe.apex.breadboard.util.FollowingProvider
import moe.apex.breadboard.util.FullscreenLoadingSpinner
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.RecommendationsProvider
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.ScrollToTopArrow
import moe.apex.breadboard.util.SmallTitleBar
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.bottomAppBarAndNavBarHeight
import moe.apex.breadboard.util.differenceOlderThan
import moe.apex.breadboard.util.onScroll
import moe.apex.breadboard.util.rememberPullToRefreshController
import moe.apex.breadboard.util.saveIgnoreListWithTimestamp
import moe.apex.breadboard.viewmodel.getGlobalViewModel
import kotlin.time.Duration.Companion.days


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    navBarVisibilityCallback: (Boolean) -> Unit = { }
) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val viewModel = getGlobalViewModel()
    val recommendationsProvider by viewModel.recommendationsProvider.collectAsState()
    val followingProvider by viewModel.followingProvider.collectAsState()
    val blockedTags by rememberUpdatedState(prefs.blockedTags)
    val unfollowedTags by rememberUpdatedState(prefs.unfollowedTags)
    val builtInIgnoredTags by rememberUpdatedState(prefs.internalIgnoreList)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    var shouldShowLargeImage by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }

    val pagerState = rememberPagerState { 2 }

    val blur = prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS)

    MainScreenScaffold(
        topAppBar = {
            Column {
                SmallTitleBar(
                    title = "Breadboard",
                    additionalActions = {
                        val currentProvider =
                            if (pagerState.currentPage == 0) recommendationsProvider else followingProvider
                        currentProvider?.let {
                            ScrollToTopArrow(
                                scrollableState = if (prefs.useStaggeredGrid) it.staggeredGridState else it.uniformGridState,
                                animate = true
                            ) {
                                navBarVisibilityCallback(true)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BreadboardTheme.colors.titleBar,
                        scrolledContainerColor = BreadboardTheme.colors.titleBar
                    )
                )
                Row(
                    modifier = Modifier
                        .background(color = BreadboardTheme.colors.titleBar)
                        .padding(horizontal = SMALL_LARGE_SPACER.dp, vertical = TINY_SPACER.dp),
                    horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp)
                ) {
                    TagPageIndicator(
                        label = "For You",
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                    )
                    TagPageIndicator(
                        label = "Following",
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                    )
                }
            }
        },
        addBottomPadding = false,
        blur = shouldShowLargeImage && blur,
    ) { padding ->
        LaunchedEffect(Unit) {
            if (differenceOlderThan(7.days, prefs.internalIgnoreListTimestamp)) {
                scope.launch {
                    IgnoredTagsHelper.fetchTagListOnline(
                        context = context,
                        onSuccess = { saveIgnoreListWithTimestamp(context, it) }
                    ) { failureResult ->
                        saveIgnoreListWithTimestamp(
                            context = context,
                            data = prefs.internalIgnoreList.takeIf { it.isNotEmpty() } ?: failureResult
                        )
                    }
                }
            }
        }

        if (builtInIgnoredTags.isEmpty()) {
            FullscreenLoadingSpinner()
        } else {
            if (recommendationsProvider == null) {
                LaunchedEffect(Unit) {
                    val newProvider = RecommendationsProvider(
                        seedImages = prefs.favouriteImages,
                        imageSource = prefs.imageSource,
                        auth = prefs.authFor(prefs.imageSource, context),
                        showAllRatings = prefs.recommendAllRatings,
                        filterRatingsLocally = prefs.filterRatingsLocally,
                        initialBlockedTags = prefs.blockedTags,
                        initialUnfollowedTags = prefs.unfollowedTags + builtInIgnoredTags,
                        selectionSize = prefs.recommendationsTagCount,
                        poolSize = prefs.recommendationsPoolSize
                    )
                    newProvider.prepareRecommendedTags()
                    viewModel.setRecommendationsProvider(newProvider)
                }
            }
            if (followingProvider == null) {
                LaunchedEffect(Unit) {
                    val newProvider = FollowingProvider(
                        followedArtists = prefs.followedTags,
                        auth = prefs.authFor(ImageSource.DANBOORU, context),
                        showAllRatings = prefs.recommendAllRatings
                    )
                    viewModel.setFollowingProvider(newProvider)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
            userScrollEnabled = !shouldShowLargeImage
        ) { page ->
            if (page == 0) {
                recommendationsProvider?.let { provider ->
                    val state = if (prefs.useStaggeredGrid) {
                        provider.staggeredGridState
                    } else {
                        provider.uniformGridState
                    }

                    val onRefresh: suspend () -> Unit by rememberUpdatedState {
                        provider.let {
                            it.replaceBlockedTags(blockedTags)
                            it.replaceUnfollowedTags(unfollowedTags + builtInIgnoredTags)
                            it.prepareRecommendedTags()
                            it.recommendImages()
                            it.resetGridStates()
                        }
                    }

                    val ptrController = rememberPullToRefreshController(
                        enabled = provider.doneInitialLoad,
                        onRefresh = onRefresh
                    )

                    FlexibleImageGrid(
                        gridState = state,
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .onScroll(state) {
                                navBarVisibilityCallback(!it.lastScrolledForward)
                            },
                        userScrollEnabled = !shouldShowLargeImage,
                        images = provider.recommendedImages,
                        onImageClick = { index, _ ->
                            Snapshot.withMutableSnapshot {
                                selectedImageIndex = index
                                shouldShowLargeImage = true
                            }
                        },
                        noImagesContent = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No recommendations right now.")
                                TextButton(
                                    onClick = {
                                        ptrController.refresh(animate = true)
                                    }
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(SMALL_SPACER.dp))
                                    Text("Refresh")
                                }
                            }
                        },
                        contentPadding = PaddingValues(
                            start = SMALL_LARGE_SPACER.dp,
                            end = SMALL_LARGE_SPACER.dp,
                            top = SMALL_LARGE_SPACER.dp,
                            bottom = bottomAppBarAndNavBarHeight
                        ),
                        pullToRefreshController = ptrController,
                        doneInitialLoad = provider.doneInitialLoad,
                        loadingIndicator = FlexibleImageGridDefaults::RoundLoadingIndicator,
                        onEndReached = { provider.recommendImages() },
                    )
                }
            } else {
                followingProvider?.let { provider ->
                    val state = if (prefs.useStaggeredGrid) {
                        provider.staggeredGridState
                    } else {
                        provider.uniformGridState
                    }

                    val onRefresh: suspend () -> Unit by rememberUpdatedState {
                        provider.let {
                            it.replaceFollowedArtists(prefs.followedTags)
                            it.reset()
                            it.loadMore()
                            it.resetGridStates()
                        }
                    }

                    val ptrController = rememberPullToRefreshController(
                        enabled = provider.doneInitialLoad,
                        onRefresh = onRefresh
                    )

                    FlexibleImageGrid(
                        gridState = state,
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .onScroll(state) {
                                navBarVisibilityCallback(!it.lastScrolledForward)
                            },
                        userScrollEnabled = !shouldShowLargeImage,
                        images = provider.images,
                        onImageClick = { index, _ ->
                            Snapshot.withMutableSnapshot {
                                selectedImageIndex = index
                                shouldShowLargeImage = true
                            }
                        },
                        noImagesContent = {
                            if (!provider.doneInitialLoad) {
                                return@FlexibleImageGrid
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (prefs.followedTags.isEmpty()){
                                        "You aren't following anyone yet."
                                    } else {
                                        "No new posts from artists you follow."
                                    }
                                )
                                TextButton(
                                    onClick = {
                                        ptrController.refresh(animate = true)
                                    }
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(SMALL_SPACER.dp))
                                    Text("Refresh")
                                }
                            }
                        },
                        contentPadding = PaddingValues(
                            start = SMALL_LARGE_SPACER.dp,
                            end = SMALL_LARGE_SPACER.dp,
                            top = SMALL_LARGE_SPACER.dp,
                            bottom = bottomAppBarAndNavBarHeight
                        ),
                        pullToRefreshController = ptrController,
                        doneInitialLoad = provider.doneInitialLoad,
                        loadingIndicator = FlexibleImageGridDefaults::RoundLoadingIndicator,
                        onEndReached = { provider.loadMore() },
                    )
                }
            }
        }
    }

    val recommendedImages = if (pagerState.currentPage == 0) recommendationsProvider?.recommendedImages else followingProvider?.images

    OffsetBasedLargeImageView(
        navController = navController,
        isActive = shouldShowLargeImage,
        initialSelectedImageIndex = selectedImageIndex,
        allImages = recommendedImages ?: emptyList(),
        onActiveStateChanged = {
            shouldShowLargeImage = it
            navBarVisibilityCallback(!it)
        }
    ) { oldImage, newImage ->
        if (recommendedImages != null) {
            val index = recommendedImages.indexOf(oldImage)
            if (index != -1) recommendedImages[index] = newImage
        }
    }
}


@Composable
private fun RowScope.TagPageIndicator(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ToggleButton(
        checked = selected,
        modifier = Modifier.weight(1f),
        onCheckedChange = { onClick() },
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = ToggleButtonDefaults.toggleButtonColors().disabledContainerColor
        )
    ) {
        Text(label)
    }
}
