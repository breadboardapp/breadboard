package moe.apex.rule34.preferences

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.image.ImageBoardLocalFilterType
import moe.apex.rule34.navigation.BlockedTagsSettings
import moe.apex.rule34.prefs
import moe.apex.rule34.util.ExportDirectoryHandler
import moe.apex.rule34.util.VerticalSpacer
import moe.apex.rule34.util.LargeVerticalSpacer
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.NavBarHeightVerticalSpacer
import moe.apex.rule34.util.StorageLocationSelection
import moe.apex.rule34.util.ImportException
import moe.apex.rule34.util.ImportHandler
import moe.apex.rule34.util.PromptType
import moe.apex.rule34.util.SmallVerticalSpacer
import moe.apex.rule34.util.exportData
import moe.apex.rule34.util.importData
import moe.apex.rule34.util.preImportChecks
import moe.apex.rule34.util.saveUriToPref
import moe.apex.rule34.util.showToast
import moe.apex.rule34.viewmodel.BreadboardViewModel
import org.json.JSONException
import org.json.JSONObject
import java.io.FileInputStream


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavHostController, viewModel: BreadboardViewModel) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var storageLocationPromptLaunched by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedData: JSONObject? by remember { mutableStateOf(null) }
    var importedData: JSONObject? by remember { mutableStateOf(null) }
    var importingStarted by rememberSaveable { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    val preferencesRepository = LocalContext.current.prefs
    val currentSettings = LocalPreferences.current

    MainScreenScaffold("Settings", scrollBehavior) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            VerticalSpacer()

            PreferencesGroup("General") {
                item {
                    EnumPref(
                        title = "Image source",
                        summary = currentSettings.imageSource.label,
                        enumItems = ImageSource.entries.toTypedArray(),
                        selectedItem = currentSettings.imageSource,
                        onSelection = {
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.IMAGE_SOURCE,
                                    it
                                )
                            }
                            viewModel.tagSuggestions.clear()
                        }
                    )
                }
                item {
                    val noAuthNeeded = currentSettings.imageSource.imageBoard.canLoadUnauthenticated
                    TitleSummary(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        title = "Set API key",
                        summary = if (!noAuthNeeded) {
                            "${currentSettings.imageSource.label} requires an API key for the best experience. " +
                                    "Tap to set."
                        } else {
                            "${currentSettings.imageSource.label} does not require an API key."
                        },
                        enabled = !noAuthNeeded
                    ) {
                        showAuthDialog = true
                    }
                }
                item {
                    SwitchPref(
                        checked = currentSettings.saveSearchHistory,
                        title = "Save search history",
                        summary = "Save your 10 most recent searches. When this is disabled, your " +
                                "search history will be cleared and Breadboard will not save future " +
                                "searches."
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
                        title = "Share fixed links",
                        summary = "When sharing an image, use a 'fixed' link where possible."
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

            if (showAuthDialog) {
                AuthDialog(
                    default = currentSettings.authFor(currentSettings.imageSource),
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
                }
            }

            LargeVerticalSpacer()

            PreferencesGroup(title = "Content filtering") {
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Manage blocked tags",
                        summary = "Add or remove tags to block from search results and recommendations."
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
                    }
                }
                item {
                    SwitchPref(
                        checked = currentSettings.filterRatingsLocally,
                        title = "Filter ratings locally",
                        summary = "Rather than appending the selected ratings to the search query, " +
                                "filter the results by rating after searching."
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.FILTER_RATINGS_LOCALLY,
                                it
                            )
                        }
                    }
                }
            }

            LargeVerticalSpacer()

            PreferencesGroup(title = "Layout") {
                item {
                    SwitchPref(
                        checked = currentSettings.useStaggeredGrid,
                        title = "Staggered grid",
                        summary = "Use a staggered grid for images rather than a uniform grid."
                    ) {
                        scope.launch {
                            preferencesRepository.updatePref(
                                PreferenceKeys.USE_STAGGERED_GRID,
                                it
                            )
                        }
                    }
                }
                item {
                    ReorderablePref(
                        title = "Reorder image actions",
                        dialogTitle = "Actions",
                        summary = "Customise the order of actions in the image viewer. The top " +
                                  "action will be displayed separately in its own dedicated button.",
                        items = currentSettings.imageViewerActions
                    ) {
                        scope.launch {
                            preferencesRepository.updateEnumList(
                                PreferenceKeys.IMAGE_VIEWER_ACTION_ORDER,
                                it
                            )
                        }
                    }
                }
            }

            LargeVerticalSpacer()

            PreferencesGroup("Data and storage") {
                item {
                    EnumPref(
                        title = "Data saver",
                        summary = currentSettings.dataSaver.label,
                        enumItems = DataSaver.entries.toTypedArray(),
                        selectedItem = currentSettings.dataSaver,
                        onSelection = {
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.DATA_SAVER,
                                    it
                                )
                            }
                        }
                    )
                }
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Save downloads to",
                        summary = if (currentSettings.storageLocation == Uri.EMPTY) "Tap to set"
                        else currentSettings.storageLocation.toString()
                    ) {
                        storageLocationPromptLaunched = true
                    }
                }
            }

            if (storageLocationPromptLaunched) {
                StorageLocationSelection(
                    promptType = PromptType.DIRECTORY_PERMISSION,
                    onFailure = { storageLocationPromptLaunched = false }
                ) { uri ->
                    saveUriToPref(context, scope, uri)
                    storageLocationPromptLaunched = false
                }
            }

            LargeVerticalSpacer()

            if (showExportDialog) {
                val categories = PrefCategory.entries.toMutableStateList()
                ExportDialog(
                    enabledCategories = categories,
                    onDismissRequest = { showExportDialog = false }
                ) {
                    scope.launch {
                        exportedData = exportData(context, categories)
                        showExportDialog = false
                    }
                }
            }

            if (exportedData != null) {
                ExportDirectoryHandler(exportedData!!) { exportedData = null }
            }

            if (importingStarted) {
                ImportHandler({ importingStarted = false }) { uri ->
                    if (!uri.toString().endsWith(".bread")) {
                        showToast(context, "Invalid file type.")
                        importingStarted = false
                        return@ImportHandler
                    }

                    scope.launch(Dispatchers.IO) {
                        context.contentResolver.openFileDescriptor(uri, "r").use { fd ->
                            FileInputStream(fd!!.fileDescriptor).use { fis ->
                                try {
                                    val json = JSONObject(fis.readBytes().decodeToString())
                                    val checkResult = preImportChecks(currentSettings, json)
                                    if (!checkResult.isSuccess) {
                                        throw checkResult.exceptionOrNull()!!
                                    }
                                    importedData = json
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        showToast(
                                            context = context,
                                            text = when (e) {
                                                is JSONException -> "Invalid backup provided."
                                                is ImportException -> e.message!!
                                                else -> "Unknown error."
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        importingStarted = false
                    }
                }
            }

            if (importedData != null) {
                ImportDialog(
                    allowedCategories = PrefCategory.entries.filter { pc -> pc == PrefCategory.BUILD || pc.name in importedData!! },
                    onDismissRequest = { importedData = null}
                ) { categories ->
                    scope.launch {
                        val result = importData(context, importedData!!, categories)
                        withContext(Dispatchers.Main) {
                            if (result.isFailure) {
                                showToast(context, result.exceptionOrNull()!!.message!!)
                            } else {
                                showToast(context, "Imported successfully.")
                            }
                        }
                        importedData = null
                    }
                }
            }

            PreferencesGroup(title = "Import/export") {
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Export data",
                        summary = "Export a backup file containing your current settings, favourite images, " +
                                "and search history."
                    ) {
                        showExportDialog = true
                    }
                }
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Import data",
                        summary = "Import a Breadboard backup file."
                    ) {
                        importingStarted = true
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 48.dp))

            InfoSection(text = "When data saver is enabled, images will load in a lower resolution " +
                               "by default. Downloads will always be in the maximum resolution.")
            AnimatedVisibility(currentSettings.imageSource.imageBoard.localFilterType != ImageBoardLocalFilterType.NOT_NEEDED) {
                Column {
                    LargeVerticalSpacer()
                    InfoSection(
                        text = "Danbooru limits searches to 2 tags (which includes ratings) " +
                                "without an API key. If you are using Danbooru without an API key, " +
                                "you should enable 'Filter ratings locally' to filter by rating. " +
                                "Yande.re always requires this option."
                    )
                }
            }
            LargeVerticalSpacer()
            InfoSection(text = "Filtering ratings locally has the benefit of being able to " +
                               "adjust the filter after searching and allows filtering without " +
                               "an API key on Danbooru, but may cause less results to be shown at " +
                               "once and result in higher data usage for the same number of " +
                               "visible images.")
            LargeVerticalSpacer()
            InfoSection(text = "When fixed links are enabled, sharing an image may use an " +
                               "alternative link depending on the source. Bluesky links are " +
                               "transformed into fxbsky.app, Pixiv links are transformed into " +
                               "phixiv.net, and Twitter links are transformed into fxtwitter.com.")
            NavBarHeightVerticalSpacer()
        }
    }
}


@Composable
private fun AuthDialog(
    default: ImageBoardAuth?,
    onDismissRequest: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var userId by remember { mutableStateOf(default?.user ?: "") }
    var apiKey by remember { mutableStateOf(default?.apiKey ?: "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Set API key") },
        text = {
            Column {
                PreferenceTextBox(
                    value = userId,
                    label = "User ID/name",
                    keyboardType = KeyboardType.Password,
                    obscured = false
                ) {
                    userId = it.trim()
                }
                SmallVerticalSpacer()
                PreferenceTextBox(
                    value = apiKey,
                    label = "API key",
                    keyboardType = KeyboardType.Password,
                    obscured = true
                ) {
                    apiKey = it.trim()
                }
            }
        },
        confirmButton = {
            Button(
                enabled = (userId.isNotBlank() && apiKey.isNotBlank()) || (userId.isBlank() && apiKey.isBlank()),
                onClick = { onSave(userId, apiKey) }) {
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


private operator fun JSONObject.contains(key: String): Boolean {
    return try {
        get(key)
        true
    } catch (_: JSONException) {
        false
    }
}
