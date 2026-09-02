package moe.apex.breadboard.detailview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.breadboard.image.ImageBoardAuth
import moe.apex.breadboard.image.ImageBoardRequirement
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.preferences.Experiment
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.preferences.PreferenceKeys
import moe.apex.breadboard.prefs
import moe.apex.breadboard.util.AgeVerification
import moe.apex.breadboard.util.HorizontallyScrollingChipsWithLabels
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.largeimageview.OffsetBasedLargeImageView
import moe.apex.breadboard.util.ApiKeyRequiredPrompt
import moe.apex.breadboard.util.PullToRefreshControllerDefaults
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.ScrollToTopArrow
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.availableRatingsForCurrentSource
import moe.apex.breadboard.util.filterChipSolidColor
import moe.apex.breadboard.util.refreshImageMetadata
import moe.apex.breadboard.util.rememberPullToRefreshController
import moe.apex.breadboard.viewmodel.SearchResultsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResults(navController: NavController, source: ImageSource, tagList: List<String>, viewModel: SearchResultsViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = LocalPreferences.current

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    var isImageCarouselVisible by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
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
    val selectedRatings by viewModel.selectedRatings.collectAsStateWithLifecycle()
    val state = if (prefs.useStaggeredGrid) {
        viewModel.staggeredGridState
    } else {
        viewModel.uniformGridState
    }

    fun setUpViewModel(auth: ImageBoardAuth? = null) {
        viewModel.setup(
            imageSource = source,
            auth = auth ?: prefs.authFor(source, context),
            tags = tagList
        )
    }

    fun updateBlockedTags() = viewModel.updateBlockedTags(manuallyBlockedTags, prefs.excludeAi)

    SideEffect(Unit) {
        val auth = prefs.authFor(source, context)
        if (auth != viewModelAuth) {
            viewModel.updateAuth(auth)
        }

        if (!isReady) {
            viewModel.updateSelectedRatings(prefs.ratingsFilter)
            setUpViewModel(auth)
            updateBlockedTags() // Subsequent calls are done in the pull to refresh callback.
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

    val ratingRows: List<@Composable () -> Unit> = availableRatingsForCurrentSource.map { rating -> {
        FilterChip(
            selected = rating in selectedRatings,
            label = { Text(rating.label) },
            colors = filterChipSolidColor,
            border = null,
            onClick = {
                if (rating in selectedRatings) {
                    viewModel.removeRating(rating)
                } else {
                    if (rating != ImageRating.SAFE && !AgeVerification.hasVerifiedAge(prefs)) {
                        showAgeVerificationDialog = true
                        return@FilterChip
                    } else {
                        viewModel.addRating(rating)
                    }
                }
                scope.launch {
                    preferencesRepository.updateSet(
                        PreferenceKeys.RATINGS_FILTER,
                        viewModel.selectedRatings.value.map { it.name })
                }
            }
        )
    } }

    val imagesToDisplay = remember(viewModelImages, blockedTags, selectedRatings) {
        viewModel.filterImages(if (filterLocally) selectedRatings else null)
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
                            scrollableState = state,
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
            return@MainScreenScaffold ApiKeyRequiredPrompt(
                modifier = Modifier
                    .padding(padding)
                    .padding(top = SMALL_LARGE_SPACER.dp),
                source = source,
                navController = navController
            )
        }

        if (!isReady) {
            return@MainScreenScaffold
        }

        FlexibleImageGrid(
            gridState = state,
            modifier = Modifier
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            images = imagesToDisplay,
            onImageClick = { index, _ ->
                Snapshot.withMutableSnapshot {
                    selectedImageIndex = index
                    isImageCarouselVisible = true
                }
            },
            noImagesContent = {
                if (selectedRatings.isEmpty()) {
                    FlexibleImageGridDefaults.NoImages("No ratings selected.")
                } else {
                    FlexibleImageGridDefaults.NoImages()
                }
            },
            contentPadding = PaddingValues(top = SMALL_LARGE_SPACER.dp, start = SMALL_LARGE_SPACER.dp, end = SMALL_LARGE_SPACER.dp),
            headerItems = {
                if (filterLocally) {
                    item {
                        HorizontallyScrollingChipsWithLabels(
                            modifier = Modifier.padding(bottom = TINY_SPACER.dp),
                            labels = listOf("Ratings"),
                            content = listOf(ratingRows)
                        )
                    }
                }
            },
            pullToRefreshController = pullToRefreshController,
            doneInitialLoad = doneInitialLoad,
            onEndReached = viewModel::loadMore
        )
    }

    OffsetBasedLargeImageView(
        navController = navController,
        isActive = isImageCarouselVisible,
        initialSelectedImageIndex = selectedImageIndex,
        allImages = imagesToDisplay,
        onActiveStateChanged = { isImageCarouselVisible = it }
    ) { image ->
        if (!image.hasGroupedTags) {
            refreshImageMetadata(image, prefs.authFor(image.imageSource, context)) {
                viewModel.updateImage(image, it)
            }
        }
    }
}
