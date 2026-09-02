package moe.apex.breadboard.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.apex.breadboard.detailview.FlexibleImageGrid
import moe.apex.breadboard.detailview.FlexibleImageGridDefaults
import moe.apex.breadboard.image.ImageBoardRequirement
import moe.apex.breadboard.preferences.Experiment
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.largeimageview.OffsetBasedLargeImageView
import moe.apex.breadboard.navigation.FollowedArtists
import moe.apex.breadboard.navigation.GeneralSettings
import moe.apex.breadboard.navigation.RecommendationsSettings
import moe.apex.breadboard.preferences.BrowseTab
import moe.apex.breadboard.preferences.PreferenceKeys
import moe.apex.breadboard.prefs
import moe.apex.breadboard.tag.IgnoredTagsHelper
import moe.apex.breadboard.ui.theme.BreadboardTheme
import moe.apex.breadboard.ui.theme.prefTitle
import moe.apex.breadboard.util.ApiKeyRequiredPrompt
import moe.apex.breadboard.util.BasicExpressiveContainer
import moe.apex.breadboard.util.CHIP_SPACING
import moe.apex.breadboard.util.ExpressivePromptWithActions
import moe.apex.breadboard.util.FollowingProvider
import moe.apex.breadboard.util.ListItemPosition
import moe.apex.breadboard.util.MEDIUM_LARGE_SPACER
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.RecommendationsHelper
import moe.apex.breadboard.util.RecommendationsProvider
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.ScrollToTopArrow
import moe.apex.breadboard.util.SmallTitleBar
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.bottomAppBarAndNavBarHeight
import moe.apex.breadboard.util.differenceOlderThan
import moe.apex.breadboard.util.filterChipSolidColor
import moe.apex.breadboard.util.refreshImageMetadata
import moe.apex.breadboard.util.onScroll
import moe.apex.breadboard.util.rememberPullToRefreshController
import moe.apex.breadboard.util.saveIgnoreListWithTimestamp
import moe.apex.breadboard.viewmodel.getGlobalViewModel
import kotlin.collections.emptyList
import kotlin.time.Duration.Companion.days


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    navBarVisibilityCallback: (Boolean) -> Unit = { }
) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val viewModel = getGlobalViewModel()
    val defaultTab by viewModel.defaultBrowseTab.collectAsState()

    val recommendationsProvider by viewModel.recommendationsProvider.collectAsState()
    val recommendedImages = recommendationsProvider?.recommendedImages?.collectAsState()?.value ?: emptyList()

    val followingProvider by viewModel.followingProvider.collectAsState()
    val followingImages = followingProvider?.images?.collectAsState()?.value ?: emptyList()

    val blockedTags by rememberUpdatedState(prefs.blockedTags)
    val unfollowedTags by rememberUpdatedState(prefs.unfollowedTags)
    val builtInIgnoredTags by rememberUpdatedState(prefs.internalIgnoreList)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    var shouldShowLargeImage by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }

    val pagerState = rememberPagerState(
        initialPage = defaultTab?.let { BrowseTab.entries.indexOf(it) } ?: run {
            val t = prefs.defaultBrowseTab
            viewModel.setDefaultBrowseTab(t)
            BrowseTab.entries.indexOf(t)
        },
    ) {
        BrowseTab.entries.size
    }

    SideEffect(pagerState.currentPage) {
        viewModel.setDefaultBrowseTab(BrowseTab.entries[pagerState.currentPage])
    }

    val blur = prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS)

    MainScreenScaffold(
        topAppBar = {
            Column {
                SmallTitleBar(
                    title = "Breadboard",
                    additionalActions = {
                        val currentProvider =
                            if (pagerState.currentPage == BrowseTab.entries.indexOf(BrowseTab.FOR_YOU)) {
                                recommendationsProvider
                            } else {
                                followingProvider
                            }
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
                        .padding(
                            horizontal = SMALL_LARGE_SPACER.dp,
                            vertical = TINY_SPACER.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp)
                    ) {
                        BrowseTab.entries.forEachIndexed { index, tab ->
                            TagPageIndicator(
                                label = tab.label,
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = pagerState.currentPage == BrowseTab.entries.indexOf(BrowseTab.FOLLOWING),
                        enter = expandHorizontally(
                            clip = false,
                            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                        )+ fadeIn() + scaleIn(
                            animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
                        ),
                        exit = shrinkHorizontally() + fadeOut() + scaleOut()
                    ) {
                        FilledIconButton(
                            modifier = Modifier.padding(start = TINY_SPACER.dp),
                            onClick = {
                                navController.navigate(FollowedArtists)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Edit followed artists",
                            )
                        }
                    }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .offset(y = SMALL_LARGE_SPACER.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                LoadingIndicator()
            }
        } else {
            if (recommendationsProvider == null) {
                SideEffect(Unit) {
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
                SideEffect(Unit) {
                    val newProvider = FollowingProvider(
                        initialFollowedArtists = prefs.followedTags,
                        initialBlockedTags = prefs.blockedTags,
                        showAllRatings = prefs.recommendAllRatings
                    )
                    viewModel.setFollowingProvider(newProvider)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            key = { BrowseTab.entries[it].name },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
            userScrollEnabled = !shouldShowLargeImage
        ) { page ->
            if (page == BrowseTab.entries.indexOf(BrowseTab.FOR_YOU)) {
                val needsAuth = remember {
                    prefs.imageSource.imageBoard.apiKeyRequirement == ImageBoardRequirement.REQUIRED &&
                            prefs.authFor(prefs.imageSource, context) == null
                }

                if (needsAuth) {
                    return@HorizontalPager ApiKeyRequiredPrompt(
                        modifier = Modifier.padding(vertical = SMALL_LARGE_SPACER.dp),
                        source = prefs.imageSource,
                        navController = navController
                    ) {
                        OutlinedButton(
                            onClick = {
                                navController.navigate(GeneralSettings)
                            },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text("Change source")
                        }
                    }
                }

                /* R34 is overwhelmingly QUESTIONABLE or EXPLICIT rated posts. We'll just
                   enforce the option to prevent any confusion as to why there would be no (good)
                   results when disabled. */
                if (prefs.imageSource == ImageSource.R34 && !prefs.recommendAllRatings) {
                    return@HorizontalPager ExpressivePromptWithActions(
                        modifier = Modifier.padding(vertical = SMALL_LARGE_SPACER.dp),
                        title = "Settings adjustment needed",
                        summary = "Your selected source is Rule34, but you don't have " +
                                  "all ratings enabled. Enable the 'Recommend all ratings' " +
                                  "option to start seeing your recommendations. "
                    ) {
                        OutlinedButton(
                            onClick = {
                                navController.navigate(GeneralSettings)
                            },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text("Change source")
                        }

                        Button(
                            onClick = {
                                navController.navigate(RecommendationsSettings)
                            },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text("Go to settings")
                        }
                    }
                }

                recommendationsProvider?.let { provider ->
                    val state = if (prefs.useStaggeredGrid) {
                        provider.staggeredGridState
                    } else {
                        provider.uniformGridState
                    }

                    val doneInitialLoad by provider.doneInitialLoad.collectAsState()

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
                        enabled = doneInitialLoad,
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
                        images = recommendedImages,
                        onImageClick = { index, _ ->
                            Snapshot.withMutableSnapshot {
                                selectedImageIndex = index
                                shouldShowLargeImage = true
                            }
                        },
                        noImagesContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FlexibleImageGridDefaults.NoImages("No recommendations right now.")
                                Button(
                                    onClick = {
                                        ptrController.refresh(animate = true)
                                    },
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
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
                        doneInitialLoad = doneInitialLoad,
                        loadingIndicator = FlexibleImageGridDefaults::RoundLoadingIndicator,
                        onEndReached = { provider.recommendImages() },
                    )
                }
            } else if (page == BrowseTab.entries.indexOf(BrowseTab.FOLLOWING)) {
                followingProvider?.let { provider ->
                    val state = if (prefs.useStaggeredGrid) {
                        provider.staggeredGridState
                    } else {
                        provider.uniformGridState
                    }

                    val doneInitialLoad by provider.doneInitialLoad.collectAsState()

                    val onRefresh: suspend () -> Unit by rememberUpdatedState {
                        provider.let {
                            it.replaceBlockedTags(blockedTags)
                            it.replaceFollowedArtists(prefs.followedTags)
                            it.reset()
                            it.loadMore()
                            it.resetGridStates()
                        }
                    }

                    val ptrController = rememberPullToRefreshController(
                        enabled = followingImages.isNotEmpty() ||
                                (doneInitialLoad && prefs.followedTags.isNotEmpty()),
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
                        images = followingImages,
                        onImageClick = { index, _ ->
                            Snapshot.withMutableSnapshot {
                                selectedImageIndex = index
                                shouldShowLargeImage = true
                            }
                        },
                        noImagesContent = {
                            if (doneInitialLoad) {
                                return@FlexibleImageGrid
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (prefs.followedTags.isEmpty()) {
                                    val suggestedArtists = remember(prefs.favouriteImages, prefs.followedTags) {
                                        RecommendationsHelper.getRecommendedArtists(
                                            images = prefs.favouriteImages,
                                            followedTags = prefs.followedTags
                                        )
                                    }

                                    if (suggestedArtists.isNotEmpty()) {
                                        BasicExpressiveContainer(position = ListItemPosition.SINGLE_ELEMENT) {
                                            ArtistSuggestions(
                                                suggestedArtists = suggestedArtists,
                                                onFollow = { artists ->
                                                    /* I'm just doing these manually because for
                                                       some reason calling ptrController.refresh()
                                                       still treats the following list like it's
                                                       empty? */
                                                    provider.reset()
                                                    provider.replaceFollowedArtists(artists.toSet())
                                                    provider.replaceBlockedTags(blockedTags)
                                                    scope.launch {
                                                        context.prefs.updateSet(
                                                            key = PreferenceKeys.FOLLOWED_TAGS,
                                                            to = artists,
                                                        )
                                                        provider.updateDoneInitialLoad(true)
                                                        provider.loadMore()
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        FlexibleImageGridDefaults.NoImages(
                                            text = "You aren't following anyone yet.\n" +
                                                   "Add some posts to your Favourites and Breadboard will suggest artists for you to follow."
                                        )
                                    }
                                } else {
                                    FlexibleImageGridDefaults.NoImages("No new posts from your followed artists.")
                                    Button(
                                        onClick = {
                                            ptrController.refresh(animate = true)
                                        },
                                        shapes = ButtonDefaults.shapes()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                        Text("Refresh")
                                    }
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
                        doneInitialLoad = doneInitialLoad,
                        loadingIndicator = FlexibleImageGridDefaults::RoundLoadingIndicator,
                        onEndReached = { provider.loadMore() },
                    )
                }
            }
        }
    }

    val displayImages = if (pagerState.currentPage == 0) recommendedImages else followingImages

    OffsetBasedLargeImageView(
        navController = navController,
        isActive = shouldShowLargeImage,
        initialSelectedImageIndex = selectedImageIndex,
        allImages = displayImages,
        onActiveStateChanged = {
            shouldShowLargeImage = it
            navBarVisibilityCallback(!it)
        }
    ) { image ->
        // Following feed is Danbooru which always has grouped tags so not needed for that.
        if (pagerState.currentPage == 0 && !image.hasGroupedTags) {
            refreshImageMetadata(image, prefs.authFor(image.imageSource, context)) { newImage ->
                val index = recommendedImages.indexOf(image)
                recommendationsProvider?.updateImage(index, newImage)
            }
        }
    }
}


@Composable
private fun ArtistSuggestions(
    suggestedArtists: List<String>,
    onFollow: (List<String>) -> Unit
) {
    val selectedForFollow = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MEDIUM_LARGE_SPACER.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TINY_SPACER.dp)
    ) {
        Text(
            text = "Welcome to your Following feed!",
            style = MaterialTheme.typography.prefTitle,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Let's get started by following some of your favourite artists.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        FlowRow(
            modifier = Modifier.padding(vertical = SMALL_LARGE_SPACER.dp),
            horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp, Alignment.CenterHorizontally),
        ) {
            for (artist in suggestedArtists) {
                FilterChip(
                    selected = artist in selectedForFollow,
                    onClick = {
                        if (artist in selectedForFollow) {
                            selectedForFollow.remove(artist)
                        } else {
                            selectedForFollow.add(artist)
                        }
                    },
                    label = { Text(artist) },
                    colors = filterChipSolidColor,
                    border = null
                )
            }
        }

        Button(
            onClick = {
                onFollow(selectedForFollow.takeIf { it.isNotEmpty() } ?: suggestedArtists)
            }
        ) {
            Text(
                text = if (selectedForFollow.isEmpty()) {
                    "Follow all"
                } else {
                    "Follow selected"
                }
            )
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
