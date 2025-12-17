package moe.apex.rule34.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import moe.apex.rule34.navigation.IgnoredTagsSettings
import moe.apex.rule34.prefs
import moe.apex.rule34.tag.IgnoredTagsHelper
import moe.apex.rule34.util.AgeVerification
import moe.apex.rule34.util.CHIP_SPACING
import moe.apex.rule34.util.ChevronRight
import moe.apex.rule34.util.ExpressiveGroup
import moe.apex.rule34.util.LARGE_SPACER
import moe.apex.rule34.util.LargeTitleBar
import moe.apex.rule34.util.MEDIUM_LARGE_SPACER
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.SMALL_SPACER
import moe.apex.rule34.util.RecommendationsHelper
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.Summary
import moe.apex.rule34.util.TitleSummary
import moe.apex.rule34.util.bouncyAnimationSpec
import moe.apex.rule34.util.differenceOlderThan
import moe.apex.rule34.util.filterChipSolidColor
import moe.apex.rule34.util.saveIgnoreListWithTimestamp
import moe.apex.rule34.util.showToast
import moe.apex.rule34.viewmodel.BreadboardViewModel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days


private const val INFO_SECTION = -2

private const val PAGER_BUTTON_SIZE_DP = 64


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsSettingsScreen(navController: NavHostController, viewModel: BreadboardViewModel) {
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
                )
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
                                        onClick = {
                                            if (!prefs.unfollowedTags.contains(rec)) {
                                                scope.launch {
                                                    if (rec in prefs.unfollowedTags) {
                                                        userPreferencesRepository.removeFromSet(
                                                            PreferenceKeys.UNFOLLOWED_TAGS,
                                                            rec
                                                        )
                                                        showRefollowedToast(rec)
                                                    } else {
                                                        userPreferencesRepository.addToSet(
                                                            PreferenceKeys.UNFOLLOWED_TAGS,
                                                            rec
                                                        )
                                                        showUnfollowedToast(rec)
                                                    }
                                                }
                                                resetRecommendations()
                                            }
                                        },
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = SMALL_LARGE_SPACER.dp,
                                    vertical = SMALL_SPACER.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledIconButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                enabled = pagerState.currentPage > 0,
                                modifier = Modifier.width(PAGER_BUTTON_SIZE_DP.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Previous page"
                                )
                            }
                            Text(
                                text = topTags.keys.elementAt(pagerState.currentPage).label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FilledIconButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                enabled = pagerState.currentPage != pagerState.pageCount - 1,
                                modifier = Modifier.width(PAGER_BUTTON_SIZE_DP.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Next page"
                                )
                            }
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
                                TitleSummary(title = "No frequent tags" )
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
                                        onClick = {
                                            scope.launch {
                                                if (tag in prefs.unfollowedTags) {
                                                    userPreferencesRepository.removeFromSet(
                                                        PreferenceKeys.UNFOLLOWED_TAGS,
                                                        tag
                                                    )
                                                    showRefollowedToast(tag)
                                                } else {
                                                    userPreferencesRepository.addToSet(
                                                        PreferenceKeys.UNFOLLOWED_TAGS,
                                                        tag
                                                    )
                                                    showUnfollowedToast(tag)
                                                }
                                            }
                                            resetRecommendations()
                                        },
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
                                      "show images and tags rated Safe."
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
                                    text = "Your search size will be capped at the number of frequent tags."
                                )
                            }
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