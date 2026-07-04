package moe.apex.breadboard.preferences

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import moe.apex.breadboard.navigation.BlockedTagsSettings
import moe.apex.breadboard.navigation.RecommendationsSettings
import moe.apex.breadboard.prefs
import moe.apex.breadboard.util.ChevronRight
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.LazyExpressiveGroup
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.TitleSummary
import moe.apex.breadboard.viewmodel.getGlobalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettingsScreen(navController: NavHostController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val viewModel = getGlobalViewModel()
    val scope = rememberCoroutineScope()
    val preferencesRepository = LocalContext.current.prefs
    val currentSettings = LocalPreferences.current

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "Content",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(MEDIUM_SPACER.dp),
        ) {
            LazyExpressiveGroup(desiredTopPadding = null) {
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Manage recommendations",
                        summary = "Fine-tune your recommendations by customising which tags can be used to recommend new content.",
                        trailingIcon = { ChevronRight() }
                    ) {
                        navController.navigate(RecommendationsSettings)
                    }
                }
                item {
                    EnumPref(
                        title = "Automatically play videos",
                        summary = currentSettings.autoplayVideos.label,
                        infoText = "Control whether videos will automatically play when " +
                                "opening them.\n\n" +
                                "When this is enabled, videos will start muted until you " +
                                "manually unmute by tapping the icon or your device's " +
                                "volume up button.",
                        enumItems = AutoplayVideosMode.entries,
                        selectedItem = currentSettings.autoplayVideos,
                        onSelection = {
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.AUTOPLAY_VIDEOS,
                                    it
                                )
                            }
                        }
                    )
                }
            }

            LazyExpressiveGroup("Filtering") {
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Manage blocked tags",
                        summary = "Add or remove tags to block from search results and recommendations.",
                        trailingIcon = { ChevronRight() }
                    ) {
                        navController.navigate(BlockedTagsSettings)
                    }
                }
                item {
                    SwitchPref(
                        checked = currentSettings.excludeAi,
                        title = "Hide AI-generated images",
                        summary = "Attempt to hide AI-generated images by automatically adding " +
                                "AI-related tags to your block list."
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.EXCLUDE_AI,
                                it
                            )
                        }
                        viewModel.resetProviders()
                    }
                }
            }

            LazyExpressiveGroup("Advanced") {
                item {
                    SwitchPref(
                        checked = currentSettings.filterRatingsLocally,
                        title = "Filter ratings locally",
                        summary = "Rather than appending the selected ratings to the search query, " +
                                  "filter the results by rating after searching.",
                        infoText = "Danbooru limits searches to 2 tags " +
                                   "(which includes ratings) without an API key.\n\n" +
                                   "Enabling this option will allow you to filter by rating " +
                                   "on all sources without an API key, and also let you filter " +
                                   "ratings mid-search, but may cause less results to be shown " +
                                   "at once, resulting in slightly higher data usage.\n\n" +
                                   "Yande.re always requires this option.\n\n" +
                                   "If you're unsure, keep this option enabled."
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.FILTER_RATINGS_LOCALLY,
                                it
                            )
                        }
                    }
                }
                item {
                    SwitchPref(
                        checked = currentSettings.profilesForAllTags,
                        title = "Allow profiles for all tags",
                        summary = "Replace the search option in the tag menu with an option to " +
                                  "view its profile.",
                        infoText = "Not all artists are categorised correctly by the image " +
                                   "boards, meaning the profile option would normally be " +
                                   "unavailable for their tag.\n\n" +
                                   "This option forces the profile option to appear for every " +
                                   "tag, even non-artist ones."
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.PROFILES_FOR_ALL_TAGS,
                                it
                            )
                        }
                    }
                }
            }
        }
    }
}
