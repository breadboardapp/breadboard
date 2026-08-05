package moe.apex.breadboard.preferences

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import moe.apex.breadboard.image.ImageBoardRequirement
import moe.apex.breadboard.prefs
import moe.apex.breadboard.util.AgeVerification
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.LazyExpressiveGroup
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.TitleSummary
import moe.apex.breadboard.viewmodel.getGlobalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(navController: NavHostController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val viewModel = getGlobalViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showAgeVerificationDialog by remember { mutableStateOf(false) }
    var showSauceNaoKeyDialog by remember { mutableStateOf(false) }

    val preferencesRepository = LocalContext.current.prefs
    val currentSettings = LocalPreferences.current

    if (showAuthDialog) {
        AuthDialog(
            selectedBoard = currentSettings.imageSource.imageBoard,
            default = currentSettings.authFor(currentSettings.imageSource, context),
            onDismissRequest = { showAuthDialog = false }
        ) { username, apiKey ->
            scope.launch {
                preferencesRepository.setAuth(
                    currentSettings.imageSource,
                    username.takeUnless { it.isBlank() },
                    apiKey.takeUnless { it.isBlank() }
                )
            }
            showAuthDialog = false
            viewModel.resetProviders()
        }
    }

    if (showAgeVerificationDialog) {
        AgeVerification.AgeVerifyDialog(
            onDismissRequest = { showAgeVerificationDialog = false },
            onAgeVerified = { showAgeVerificationDialog = false }
        )
    }

    if (showSauceNaoKeyDialog) {
        SauceNaoApiKeyDialog(
            currentKey = currentSettings.saucenaoApiKey,
            onDismissRequest = { showSauceNaoKeyDialog = false },
            onSave = { key ->
                scope.launch {
                    preferencesRepository.updatePref(PreferenceKeys.SAUCENAO_API_KEY, key)
                }
                showSauceNaoKeyDialog = false
            }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        title = "Set API key",
                        summary = if (authType != ImageBoardRequirement.NOT_NEEDED) {
                            "${currentSettings.imageSource.label} requires an API key${if (authType == ImageBoardRequirement.RECOMMENDED) " for the best experience." else "."} " +
                            "Tap to set."
                        } else {
                            "${currentSettings.imageSource.label} does not require an API key."
                        },
                        enabled = authType != ImageBoardRequirement.NOT_NEEDED
                    ) {
                        showAuthDialog = true
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

            LazyExpressiveGroup("SauceNAO") {
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Set SauceNAO API key",
                        summary = if (currentSettings.saucenaoApiKey.isNotEmpty()) {
                            "API key is set."
                        } else {
                            "An API key is required to use reverse image search. Tap to set."
                        }
                    ) {
                        showSauceNaoKeyDialog = true
                    }
                }
            }
        }
    }
}


@Composable
private fun SauceNaoApiKeyDialog(
    currentKey: String,
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("SauceNAO API key") },
        text = {
            Column {
                Text(
                    text = "Enter your SauceNAO API key. You can get one from saucenao.com/user.php after creating an account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = SMALL_SPACER.dp)
                )
                PreferenceTextBox(
                    value = apiKey,
                    label = "API key",
                    obscured = true,
                    keyboardType = KeyboardType.Password
                ) {
                    apiKey = it.trim()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(apiKey) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

