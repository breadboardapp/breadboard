package moe.apex.rule34.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import moe.apex.rule34.detailview.ImageGrid
import moe.apex.rule34.preferences.Experiment
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.largeimageview.OffsetBasedLargeImageView
import moe.apex.rule34.tag.IgnoredTagsHelper
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.RecommendationsProvider
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.SMALL_SPACER
import moe.apex.rule34.util.ScrollToTopArrow
import moe.apex.rule34.util.bottomAppBarAndNavBarHeight
import moe.apex.rule34.util.differenceOlderThan
import moe.apex.rule34.util.onScroll
import moe.apex.rule34.util.rememberPullToRefreshController
import moe.apex.rule34.util.saveIgnoreListWithTimestamp
import moe.apex.rule34.viewmodel.BreadboardViewModel
import kotlin.time.Duration.Companion.days


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: BreadboardViewModel,
    bottomBarVisibleState: MutableState<Boolean>,
) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val recommendationsProvider by viewModel.recommendationsProvider.collectAsState()
    val blockedTags by rememberUpdatedState(prefs.blockedTags)
    val unfollowedTags by rememberUpdatedState(prefs.unfollowedTags)
    val builtInIgnoredTags by rememberUpdatedState(prefs.internalIgnoreList)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    val shouldShowLargeImage = remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }

    val blur = prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS)

    val _onRefresh: suspend () -> Unit = {
        recommendationsProvider?.let {
            it.replaceBlockedTags(blockedTags)
            it.replaceUnfollowedTags(unfollowedTags + builtInIgnoredTags)
            it.prepareRecommendedTags()
            it.recommendImages()
            it.resetGridStates()
        }
    }
    val onRefresh by rememberUpdatedState(_onRefresh)

    val pullToRefreshController = rememberPullToRefreshController(onRefresh = onRefresh)

    MainScreenScaffold(
        title = "Breadboard",
        largeTopBar = false,
        scrollBehavior = scrollBehavior,
        addBottomPadding = false,
        blur = shouldShowLargeImage.value && blur,
        additionalActions = {
            recommendationsProvider?.let {
                ScrollToTopArrow(
                    staggeredGridState = it.staggeredGridState,
                    uniformGridState = it.uniformGridState
                ) {
                    scrollBehavior.state.contentOffset = 0f
                }
            }
        }
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
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(SMALL_LARGE_SPACER.dp)
            )
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
                        poolSize = prefs.recommendationsPoolSize,
                        useWeightedSelection = prefs.recommendationsWeightedSelection
                    )
                    newProvider.prepareRecommendedTags()
                    viewModel.setRecommendationsProvider(newProvider)
                }
            }
        }

        recommendationsProvider?.let { provider ->
            ImageGrid(
                modifier = Modifier
                    .padding(padding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .onScroll(provider.staggeredGridState) {
                        bottomBarVisibleState.value = !it.lastScrolledForward
                    }
                    .onScroll(provider.uniformGridState) {
                        bottomBarVisibleState.value = !it.lastScrolledForward
                    },
                staggeredGridState = provider.staggeredGridState,
                uniformGridState = provider.uniformGridState,
                images = provider.recommendedImages,
                noImagesContent = {
                    if (!provider.doneInitialLoad) {
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
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(SMALL_SPACER.dp))
                            Text("Refresh")
                        }
                    }
                },
                onImageClick = { index, _ ->
                    Snapshot.withMutableSnapshot {
                        initialPage = index
                        shouldShowLargeImage.value = true
                    }
                },
                contentPadding = PaddingValues(
                    start = SMALL_LARGE_SPACER.dp,
                    end = SMALL_LARGE_SPACER.dp,
                    top = SMALL_LARGE_SPACER.dp,
                    bottom = bottomAppBarAndNavBarHeight
                ),
                pullToRefreshController = pullToRefreshController,
                doneInitialLoad = provider.doneInitialLoad,
                onEndReached = { provider.recommendImages() },
            )
        }
    }

    OffsetBasedLargeImageView(
        navController,
        shouldShowLargeImage,
        initialPage,
        recommendationsProvider?.recommendedImages ?: emptyList(),
        bottomBarVisibleState
    )
}
