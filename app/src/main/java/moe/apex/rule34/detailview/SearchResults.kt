package moe.apex.rule34.detailview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.image.ImageBoardRequirement
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.navigation.Settings
import moe.apex.rule34.preferences.Experiment
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.PreferenceKeys
import moe.apex.rule34.prefs
import moe.apex.rule34.util.AgeVerification
import moe.apex.rule34.util.ExpressiveContainer
import moe.apex.rule34.util.HorizontallyScrollingChipsWithLabels
import moe.apex.rule34.util.LargeTitleBar
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.largeimageview.OffsetBasedLargeImageView
import moe.apex.rule34.util.PullToRefreshControllerDefaults
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.ScrollToTopArrow
import moe.apex.rule34.util.TINY_SPACER
import moe.apex.rule34.util.TitleSummary
import moe.apex.rule34.util.availableRatingsForCurrentSource
import moe.apex.rule34.util.filterChipSolidColor
import moe.apex.rule34.util.rememberPullToRefreshController
import moe.apex.rule34.util.withoutVertical
import moe.apex.rule34.viewmodel.SearchResultsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResults(navController: NavController, source: ImageSource, tagList: List<String>, viewModel: SearchResultsViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = LocalPreferences.current

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    val isImageCarouselVisible = remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }
    var showAgeVerificationDialog by remember { mutableStateOf(false) }

    val preferencesRepository = LocalContext.current.prefs
    val filterLocally = prefs.filterRatingsLocally
    val blockedTags by rememberUpdatedState(prefs.blockedTags)
    val blur = prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS)

    val actuallyBlockedTags = rememberSaveable { mutableStateSetOf<String>() }
    val actuallySelectedRatings = rememberSaveable {
        mutableStateSetOf<ImageRating>().apply {
            addAll(prefs.ratingsFilter)
        }
    }

    fun setUpViewModel(auth: ImageBoardAuth? = null) {
        if (!viewModel.isReady) {
            viewModel.setup(
                imageSource = source,
                auth = auth ?: prefs.authFor(source, context),
                tags = tagList
            )
        }
    }

    // Populate the internal list of blocked tags
    fun updateBlockedTags() {
        Snapshot.withMutableSnapshot {
            actuallyBlockedTags.clear()
            actuallyBlockedTags.addAll(blockedTags.filter { it !in tagList })
        }
    }

    LaunchedEffect(Unit) {
        val auth = prefs.authFor(source, context)
        if (auth != viewModel.auth) {
            viewModel.prepareReset()
        }
        setUpViewModel(auth)
        // Don't automatically update on config change like screen rotation if the list is already populated
        if (actuallyBlockedTags.isEmpty()) {
            updateBlockedTags()
        }
    }

    val pullToRefreshController = rememberPullToRefreshController(
        indicator = {
            PullToRefreshControllerDefaults.Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(
                        if (prefs.filterRatingsLocally) {
                            Modifier.offset(y = 80.dp) // Height of the ratings box
                        } else Modifier
                    ),
                controller = it
            )
        }
    ) {
        updateBlockedTags()
        viewModel.prepareReset()
        setUpViewModel()
        viewModel.loadMore()
    }

    val ratingRows: List<@Composable () -> Unit> = availableRatingsForCurrentSource.map { {
        FilterChip(
            selected = it in actuallySelectedRatings,
            label = { Text(it.label) },
            colors = filterChipSolidColor,
            border = null,
            onClick = {
                if (it in actuallySelectedRatings) {
                    actuallySelectedRatings.remove(it)
                } else {
                    if (it != ImageRating.SAFE && !AgeVerification.hasVerifiedAge(prefs)) {
                        showAgeVerificationDialog = true
                        return@FilterChip
                    } else {
                        actuallySelectedRatings.add(it)
                    }
                }
                scope.launch {
                    preferencesRepository.updateSet(
                        PreferenceKeys.RATINGS_FILTER,
                        actuallySelectedRatings.map { it.name })
                }
            }
        )
    } }

    val imagesToDisplay = viewModel.images.filter {
        it.metadata!!.tags.none { tag -> actuallyBlockedTags.contains(tag.lowercase()) } &&
        if (prefs.filterRatingsLocally) it.metadata.rating in prefs.ratingsFilter else true
    }

    if (showAgeVerificationDialog) {
        AgeVerification.AgeVerifyDialog(
            onDismissRequest = { showAgeVerificationDialog = false },
            onAgeVerified = { showAgeVerificationDialog = false }
        )
    }

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "Search results",
                scrollBehavior = scrollBehavior,
                navController = navController,
                additionalActions = {
                    if (viewModel.isReady) {
                        ScrollToTopArrow(
                            staggeredGridState = viewModel.staggeredGridState,
                            uniformGridState = viewModel.uniformGridState,
                            animate = !filterLocally || Experiment.ALWAYS_ANIMATE_SCROLL.isEnabled(),
                        )
                    }
                }
            )
        },
        addBottomPadding = false,
        blur = isImageCarouselVisible.value && blur,
    ) { padding ->
        if (!viewModel.isReady) {
            return@MainScreenScaffold
        }

        val needsAuth = remember {
            source.imageBoard.apiKeyRequirement == ImageBoardRequirement.REQUIRED &&
            prefs.authFor(source, context) == null
        }

        if (needsAuth) {
            return@MainScreenScaffold Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(top = SMALL_LARGE_SPACER.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MEDIUM_SPACER.dp)
            ) {
                ExpressiveContainer(position = ListItemPosition.SINGLE_ELEMENT) {
                    TitleSummary(
                        title = "API Key required",
                        summary = "${source.label} requires an API key to search.\n" +
                                  "Add an API key in Settings.\n" +
                                  "Alternatively, use a different image source.",
                    )
                }
                Button(
                    onClick = {
                        navController.navigate(Settings)
                    },
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("Go to Settings")
                }
            }
        }

        ImageGrid(
            modifier = Modifier
                .padding(padding.withoutVertical(top = false))
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            staggeredGridState = viewModel.staggeredGridState,
            uniformGridState = viewModel.uniformGridState,
            images = imagesToDisplay,
            onImageClick = { index, _ ->
                Snapshot.withMutableSnapshot {
                    initialPage = index
                    isImageCarouselVisible.value = true
                }
            },
            contentPadding = PaddingValues(top = SMALL_LARGE_SPACER.dp, start = SMALL_LARGE_SPACER.dp, end = SMALL_LARGE_SPACER.dp),
            filterComposable = if (filterLocally) { {
                HorizontallyScrollingChipsWithLabels(
                    modifier = Modifier.padding(bottom = TINY_SPACER.dp),
                    labels = listOf("Ratings"),
                    content = listOf(ratingRows)
                )
            } } else null,
            pullToRefreshController = pullToRefreshController,
            doneInitialLoad = viewModel.doneInitialLoad,
            onEndReached = viewModel::loadMore,
            noImagesContent = { if (viewModel.doneInitialLoad) { NoImages() } }
        )
    }

    OffsetBasedLargeImageView(navController, isImageCarouselVisible, initialPage, imagesToDisplay)
}

