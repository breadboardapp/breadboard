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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import moe.apex.breadboard.image.ImageBoardRequirement
import moe.apex.breadboard.navigation.ApiKeysSettings
import moe.apex.breadboard.prefs
import moe.apex.breadboard.util.AgeVerification
import moe.apex.breadboard.util.ChevronRight
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.LazyExpressiveGroup
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.TitleSummary
import moe.apex.breadboard.viewmodel.getGlobalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(navController: NavHostController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val viewModel = getGlobalViewModel()
    val scope = rememberCoroutineScope()

    var showAgeVerificationDialog by remember { mutableStateOf(false) }

    val preferencesRepository = LocalContext.current.prefs
    val currentSettings = LocalPreferences.current

    if (showAgeVerificationDialog) {
        AgeVerification.AgeVerifyDialog(
            onDismissRequest = { showAgeVerificationDialog = false },
            onAgeVerified = { showAgeVerificationDialog = false }
        )
    }

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "General",
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
            contentPadding = PaddingValues(MEDIUM_SPACER.dp)
        ) {
            LazyExpressiveGroup(desiredTopPadding = null) {
                item {
                    EnumPref(
                        title = "Image source",
                        summary = currentSettings.imageSource.label,
                        enumItems = ImageSource.entries,
                        selectedItem = currentSettings.imageSource,
                        onSelection = {
                            if (it == ImageSource.R34 && !AgeVerification.hasVerifiedAge(currentSettings)) {
                                showAgeVerificationDialog = true
                                return@EnumPref
                            }
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.IMAGE_SOURCE,
                                    it
                                )
                            }
                            viewModel.clearTagSuggestions()
                        }
                    )
                }
                item {
                    val authType = currentSettings.imageSource.imageBoard.apiKeyRequirement
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Manage API keys",
                        summary = if (authType == ImageBoardRequirement.NOT_NEEDED) {
                            "${currentSettings.imageSource.label} does not require an API key, " +
                            "but you can manage others here."
                        } else {"${currentSettings.imageSource.label} requires an API key${if (authType == ImageBoardRequirement.RECOMMENDED) " for the best experience." else "."} " +
                            "Set in API Key settings."
                        },
                        trailingIcon = { ChevronRight() }
                    ) {
                        navController.navigate(ApiKeysSettings)
                    }
                }
                item {
                    SwitchPref(
                        checked = currentSettings.saveSearchHistory,
                        title = "Save search history",
                        summary = "Save your 10 most recent searches. When this is disabled, " +
                                  "your search history will be cleared and Breadboard will not " +
                                  "save future searches."
                    ) {
                        scope.launch {
                            if (!it) preferencesRepository.clearSearchHistory()
                            preferencesRepository.updatePref(
                                key = PreferenceKeys.SAVE_SEARCH_HISTORY,
                                to = it
                            )
                        }
                    }
                }
                item {
                    SwitchPref(
                        checked = currentSettings.useFixedLinks,
                        title = "Share embeddable links",
                        summary = "When sharing an image, use a link that embeds better on external platforms when possible.",
                        infoText = "When embeddable links are enabled, sharing an image may " +
                                   "use an alternative link depending on the source.\n\n" +
                                   "Bilibili links are transformed into vxbilibili.com\n" +
                                   "Bluesky links are transformed into fxbsky.app\n" +
                                   "Pixiv links are transformed into phixiv.net\n" +
                                   "Twitter links are transformed into fxtwitter.com\n" +
                                   "Weibo links are transformed into fxweibo.com."
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.USE_FIXED_LINKS,
                                it
                            )
                        }
                    }
                }
            }
        }
    }
}
