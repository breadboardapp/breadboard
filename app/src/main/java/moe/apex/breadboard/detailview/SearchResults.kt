package moe.apex.breadboard.detailview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.breadboard.image.ImageBoardAuth
import moe.apex.breadboard.image.ImageBoardRequirement
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.navigation.Settings
import moe.apex.breadboard.preferences.Experiment
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.preferences.PreferenceKeys
import moe.apex.breadboard.prefs
import moe.apex.breadboard.util.AgeVerification
import moe.apex.breadboard.util.ExpressiveContainer
import moe.apex.breadboard.util.HorizontallyScrollingChipsWithLabels
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.ListItemPosition
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.largeimageview.OffsetBasedLargeImageView
import moe.apex.breadboard.util.PullToRefreshControllerDefaults
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.ScrollToTopArrow
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.TitleSummary
import moe.apex.breadboard.util.availableRatingsForCurrentSource
import moe.apex.breadboard.util.filterChipSolidColor
import moe.apex.breadboard.util.rememberPullToRefreshController
import moe.apex.breadboard.util.withoutVertical
import moe.apex.breadboard.viewmodel.SearchResultsViewModel


@Composable
fun SearchResults(navController: NavController, source: ImageSource, tagList: List<String>, viewModel: SearchResultsViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = LocalPreferences.current

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    var isImageCarouselVisible by remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(0) }
    var showAgeVerificationDialog by remember { mutableStateOf(false) }

    val preferencesRepository = LocalContext.current.prefs
    val filterLocally = prefs.filterRatingsLocally
    val manuallyBlockedTags by rememberUpdatedState(prefs.manuallyBlockedTags)
    val blur = prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS)

    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    val viewModelAuth by viewModel.auth.collectAsStateWithLifecycle()
    val doneInitialLoad by viewModel.doneInitialLoad.collectAsStateWithLifecycle()
    val viewModelImages by viewModel.images.collectAsStateWithLifecycle()
    val blockedTags by viewModel.blockedTags.collectAsStateWithLifecycle()

    val actuallySelectedRatings = rememberSaveable {
        mutableStateSetOf<ImageRating>().apply {
            addAll(prefs.ratingsFilter)
        }
    }

    fun setUpViewModel(auth: ImageBoardAuth? = null) {
        viewModel.setup(
            imageSource = source,
            auth = auth ?: prefs.authFor(source, context),
            tags = tagList
        )
    }

    fun updateBlockedTags() = viewModel.updateBlockedTags(manuallyBlockedTags, prefs.excludeAi)

    LaunchedEffect(Unit) {
        val auth = prefs.authFor(source, context)
        if (auth != viewModelAuth) {
            viewModel.updateAuth(auth)
        }

        if (!isReady) {
            updateBlockedTags() // Subsequent calls are done in the pull to refresh callback.
            setUpViewModel(auth)
        }
    }

    val pullToRefreshController = rememberPullToRefreshController(
        indicator = {
            PullToRefreshControllerDefaults.Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(
                        if (filterLocally) {
                            Modifier.offset(y = 80.dp) // Height of the ratings box
                        } else Modifier
                    ),
                controller = it
            )
        }
    ) {
        setUpViewModel()
        updateBlockedTags()
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

    val imagesToDisplay = remember(viewModelImages, blockedTags, actuallySelectedRatings.size) {
        viewModel.filterImages(if (filterLocally) actuallySelectedRatings else null)
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
                    if (doneInitialLoad) {
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
        blur = isImageCarouselVisible && blur,
    ) { padding ->
        val needsAuth = remember {
            source.imageBoard.apiKeyRequirement == ImageBoardRequirement.REQUIRED &&
            prefs.authFor(source, context) == null
        }

        if (needsAuth) {
            return@MainScreenScaffold ApiKeyRequiredColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(top = SMALL_LARGE_SPACER.dp)
                    .fillMaxWidth(),
                source = source
            ) {
                navController.navigate(Settings)
            }
        }

        if (!isReady) {
            return@MainScreenScaffold
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
                    isImageCarouselVisible = true
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
            doneInitialLoad = doneInitialLoad,
            onEndReached = viewModel::loadMore,
            noImagesContent = { if (doneInitialLoad) { NoImages() } }
        )
    }

    OffsetBasedLargeImageView(
        navController = navController,
        isActive = isImageCarouselVisible,
        initialPage = initialPage,
        allImages = imagesToDisplay,
        onActiveStateChanged = { isImageCarouselVisible = it }
    ) { oldImage, newImage ->
        viewModel.updateImage(oldImage, newImage)
    }
}



@Composable
fun ApiKeyRequiredColumn(
    modifier: Modifier = Modifier,
    source: ImageSource,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
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
            onClick = onClick,
            colors = ButtonDefaults.buttonColors()
        ) {
            Text("Go to Settings")
        }
    }
}