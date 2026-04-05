package moe.apex.breadboard.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import moe.apex.breadboard.navigation.IgnoredTagsSettings
import moe.apex.breadboard.prefs
import moe.apex.breadboard.tag.IgnoredTagsHelper
import moe.apex.breadboard.util.AgeVerification
import moe.apex.breadboard.util.CHIP_SPACING
import moe.apex.breadboard.util.ChevronRight
import moe.apex.breadboard.util.ExpressiveGroup
import moe.apex.breadboard.util.LARGE_SPACER
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.MEDIUM_LARGE_SPACER
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.RecommendationsHelper
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.Summary
import moe.apex.breadboard.util.TitleSummary
import moe.apex.breadboard.util.bouncyAnimationSpec
import moe.apex.breadboard.util.differenceOlderThan
import moe.apex.breadboard.util.filterChipSolidColor
import moe.apex.breadboard.util.saveIgnoreListWithTimestamp
import moe.apex.breadboard.util.showToast
import moe.apex.breadboard.viewmodel.BreadboardViewModel
import moe.apex.breadboard.viewmodel.GlobalViewModelOwner
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days


private const val INFO_SECTION = -2

private const val PAGER_BUTTON_SIZE_DP = 64


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecommendationsSettingsScreen(navController: NavHostController) {
    val viewModel: BreadboardViewModel = viewModel(GlobalViewModelOwner)
    val context = LocalContext.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()
    val userPreferencesRepository = LocalContext.current.prefs
    val prefs = LocalPreferences.current
    val recommendationsProvider by viewModel.recommendationsProvider.collectAsState()

    var showAgeVerificationDialog by remember { mutableStateOf(false) }
    var showIgnored by rememberSaveable { mutableStateOf(false) }

    val favouriteImagesPerSource = remember {
        prefs.favouriteImages.groupBy { it.imageSource }
    }

    LaunchedEffect(Unit) {
        if (differenceOlderThan(7.days, prefs.internalIgnoreListTimestamp)) {
            scope.launch(Dispatchers.IO) {
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

    val topTags = remember(
        prefs.recommendAllRatings,
        prefs.recommendationsPoolSize,
        prefs.unfollowedTags,
        showIgnored,
        prefs.internalIgnoreList
    ) {
        mutableMapOf<ImageSource, List<String>>().apply {
            ImageSource.entries.forEach {
                val allTags = RecommendationsHelper.getAllTags(
                    images = favouriteImagesPerSource[it] ?: emptyList(),
                    allowAllRatings = prefs.recommendAllRatings,
                    excludedTags = prefs.blockedTags
                )
                this[it] = RecommendationsHelper.getMostCommonTags(
                    allTags = allTags,
                    followedTagsLimit = prefs.recommendationsPoolSize,
                    hiddenTags = prefs.internalIgnoreList,
                    unfollowedTags = prefs.unfollowedTags,
                    includeUnwantedTagsInResult = showIgnored
                ).map { it.first }
            }
        }
    }

    val pagerState = rememberPagerState(ImageSource.entries.indexOf(prefs.imageSource)) { topTags.size }

    fun resetRecommendations() {
        viewModel.setRecommendationsProvider(null)
    }

    fun showUnfollowedToast(tagName: String) {
        showToast(context, "Ignored $tagName")
    }

    fun showRefollowedToast(tagName: String) {
        showToast(context, "Unignored $tagName")
    }

    fun onChipClick(tagName: String) {
        scope.launch {
            if (tagName in prefs.unfollowedTags) {
                userPreferencesRepository.removeFromSet(
                    PreferenceKeys.UNFOLLOWED_TAGS,
                    tagName
                )
                showRefollowedToast(tagName)
            } else {
                userPreferencesRepository.addToSet(
                    PreferenceKeys.UNFOLLOWED_TAGS,
                    tagName
                )
                showUnfollowedToast(tagName)
            }
        }.invokeOnCompletion {
            resetRecommendations()
        }
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
                title = "Recommendations",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        }
    ) {
        val hasProvider = rememberSaveable { recommendationsProvider != null }
        val recentRecommendations = rememberSaveable { recommendationsProvider?.recommendedTags ?: emptyList() }
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(vertical = SMALL_LARGE_SPACER.dp),
            verticalArrangement = Arrangement.spacedBy(LARGE_SPACER.dp)
        ) {
            item {
                ExpressiveGroup("Recently recommended tags") {
                    item {
                        if (hasProvider) {
                            if (recentRecommendations.isEmpty()) {
                                Summary(
                                    modifier = Modifier.padding(SMALL_LARGE_SPACER.dp),
                                    text = "Add images from ${prefs.imageSource.label} to your " +
                                           "favourites to start getting personalised recommendations."
                                )
                                return@item
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = SMALL_LARGE_SPACER.dp,
                                        vertical = SMALL_SPACER.dp
                                    )
                            ) {
                                for (rec in recentRecommendations) {
                                    FilterChip(
                                        selected = rec !in prefs.unfollowedTags,
                                        onClick = { onChipClick(rec) },
                                        label = { Text(rec) },
                                        colors = filterChipSolidColor,
                                        border = null
                                    )
                                }
                            }
                        } else {
                            Summary(
                                modifier = Modifier.padding(SMALL_LARGE_SPACER.dp),
                                text = "Refresh your recommendations to see them here."
                            )
                        }
                    }
                }
            }

            item {
                ExpressiveGroup("Frequent tags") {
                    item {
                        ButtonGroup(
                            modifier = Modifier.padding(
                                horizontal = SMALL_LARGE_SPACER.dp,
                                vertical = SMALL_SPACER.dp
                            ),
                            overflowIndicator = { },
                            expandedRatio = 0.2f, // Default is 15% but I like this more
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            customItem(
                                buttonGroupContent = {
                                    PageChangeButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        },
                                        enabled = pagerState.currentPage > 0,
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = "Previous page"
                                        )
                                    }
                                },
                                menuContent = { }
                            )

                            customItem(
                                buttonGroupContent = {
                                    Text(
                                        text = topTags.keys.elementAt(pagerState.currentPage).label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                },
                                menuContent = { }
                            )

                            customItem(
                                buttonGroupContent = {
                                    PageChangeButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        },
                                        enabled = pagerState.currentPage != pagerState.pageCount - 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = "Next page"
                                        )
                                    }
                                },
                                menuContent = { }
                            )
                        }
                    }

                    item {
                        HorizontalPager(
                            modifier = Modifier.animateContentSize(bouncyAnimationSpec()),
                            state = pagerState,
                            verticalAlignment = Alignment.Top,
                            beyondViewportPageCount = 0
                        ) { page ->
                            val tags = topTags[ImageSource.entries[page]] ?: throw IllegalStateException("Recommendations settings pager out of bounds.")

                            if (tags.isEmpty()) {
                                Summary(
                                    modifier = Modifier.padding(SMALL_LARGE_SPACER.dp),
                                    text = "No frequent tags. Add some art from " +
                                           "${ImageSource.entries[page].label} to your " +
                                           "favourites to get personalised recommendations."
                                )
                                return@HorizontalPager
                            }

                            FlowRow(
                                modifier = Modifier.padding(
                                    horizontal = SMALL_LARGE_SPACER.dp,
                                    vertical = SMALL_SPACER.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp)
                            ) {
                                for (tag in tags) {
                                    FilterChip(
                                        selected = tag !in prefs.unfollowedTags,
                                        onClick = { onChipClick(tag) },
                                        label = { Text(tag) },
                                        colors = filterChipSolidColor,
                                        border = null
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SwitchPref(
                            checked = showIgnored,
                            title = "Show ignored tags"
                        ) {
                            showIgnored = it
                        }
                    }
                }
            }

            item {
                Summary(
                    modifier = Modifier.padding(horizontal = MEDIUM_LARGE_SPACER.dp),
                    text = "Your frequent tags consist of the most common tags from your " +
                            "favourite images. Breadboard will use these tags to recommend " +
                            "new content. You can tap a tag above to ignore it, preventing it " +
                            "from being used to recommend new content."
                )
            }

            item {
                ExpressiveGroup {
                    item {
                        SwitchPref(
                            checked = prefs.recommendAllRatings,
                            title = "Recommend all ratings",
                            summary = "Show images and tags from all ratings. If disabled, only " +
                                      "show images and tags rated General."
                        ) {
                            if (it && !AgeVerification.hasVerifiedAge(prefs)) {
                                showAgeVerificationDialog = true
                                return@SwitchPref
                            }
                            scope.launch {
                                userPreferencesRepository.updatePref(
                                    PreferenceKeys.RECOMMEND_ALL_RATINGS,
                                    it
                                )
                            }
                            resetRecommendations()
                        }
                    }
                    item {
                        SliderPref(
                            title = "Maximum frequent tags",
                            label = { it.roundToInt().toString() },
                            initialValue = remember { prefs.recommendationsPoolSize.toFloat() },
                            displayValueRange = 0f..20f,
                            allowedValueRange = 1f .. 20f
                        ) {
                            scope.launch {
                                userPreferencesRepository.updatePref(
                                    PreferenceKeys.RECOMMENDATIONS_POOL_SIZE,
                                    it.roundToInt()
                                )
                            }
                            resetRecommendations()
                        }
                    }
                    item {
                        Column {
                            SliderPref(
                                title = "Search size",
                                initialValue = remember { prefs.recommendationsTagCount.toFloat() },
                                displayValueRange = 0f .. 5f,
                                allowedValueRange = 1f..5f
                            ) {
                                scope.launch {
                                    userPreferencesRepository.updatePref(
                                        PreferenceKeys.RECOMMENDATIONS_TAG_COUNT,
                                        it.roundToInt()
                                    )
                                }
                                resetRecommendations()
                            }
                            AnimatedVisibility(
                                visible = prefs.recommendationsTagCount > prefs.recommendationsPoolSize,
                                enter = fadeIn() + expandVertically(bouncyAnimationSpec())
                            ) {
                                Summary(
                                    modifier = Modifier.padding(
                                        start = MEDIUM_LARGE_SPACER.dp,
                                        end = MEDIUM_LARGE_SPACER.dp,
                                        bottom = MEDIUM_LARGE_SPACER.dp
                                    ),
                                    text = "Your search size will be capped at the number of " +
                                           "frequent tags."
                                )
                            }
                        }
                    }
                    item {
                        SwitchPref(
                            checked = prefs.recommendationsWeightedSelection,
                            title = "Respect tag order",
                            summary = "Tags that appear earlier in your frequent tags list are " +
                                      "more likely to be used when recommending new content."
                        ) {
                            scope.launch {
                                userPreferencesRepository.updatePref(
                                    PreferenceKeys.RECOMMENDATIONS_WEIGHTED_SELECTION,
                                    it
                                )
                            }
                            resetRecommendations()
                        }
                    }
                }
            }

            item {
                ExpressiveGroup("Ignored tags") {
                    item {
                        TitleSummary(
                            title = "Manage ignored tags",
                            summary = "View and manage the tags that Breadboard will ignore when " +
                                      "recommending new content.",
                            trailingIcon = { ChevronRight() },
                            onClick = {
                                navController.navigate(IgnoredTagsSettings)
                            }
                        )
                    }
                }
            }


            item(key = INFO_SECTION) {
                Column(
                    modifier = Modifier.padding(top = LARGE_SPACER.dp),
                    verticalArrangement = Arrangement.spacedBy(LARGE_SPACER.dp)
                ) {
                    HorizontalDivider()
                    InfoSection(
                        text = "Your chosen search size will apply only when allowed by the " +
                               "selected image board. Danbooru limits searches to 2 tags without " +
                               "an API key."
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ButtonGroupScope.PageChangeButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    FilledIconButton(
        onClick = onClick,
        shapes = IconButtonShapes(
            shape = CircleShape,
            pressedShape = MaterialTheme.shapes.medium
        ),
        modifier = Modifier
            .width(PAGER_BUTTON_SIZE_DP.dp)
            .animateWidth(interactionSource),
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}
