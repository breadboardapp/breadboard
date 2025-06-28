package moe.apex.rule34.preferences

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.image.ImageBoardLocalFilterType
import moe.apex.rule34.prefs
import moe.apex.rule34.util.BaseHeading
import moe.apex.rule34.util.ExportDirectoryHandler
import moe.apex.rule34.util.VerticalSpacer
import moe.apex.rule34.util.LargeVerticalSpacer
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.NavBarHeightVerticalSpacer
import moe.apex.rule34.util.StorageLocationSelection
import moe.apex.rule34.util.ImportException
import moe.apex.rule34.util.ImportHandler
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MediumLargeVerticalSpacer
import moe.apex.rule34.util.PromptType
import moe.apex.rule34.util.SmallVerticalSpacer
import moe.apex.rule34.util.ExpressiveTagEntryContainer
import moe.apex.rule34.util.TitledModalBottomSheet
import moe.apex.rule34.util.exportData
import moe.apex.rule34.util.importData
import moe.apex.rule34.util.largerShapeCornerSize
import moe.apex.rule34.util.navBarHeight
import moe.apex.rule34.util.preImportChecks
import moe.apex.rule34.util.saveUriToPref
import moe.apex.rule34.util.showToast
import moe.apex.rule34.viewmodel.BreadboardViewModel
import org.json.JSONException
import org.json.JSONObject
import java.io.FileInputStream


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(viewModel: BreadboardViewModel) {
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
    var showBlockedTagsSheet by rememberSaveable { mutableStateOf(false) }

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

            PreferencesGroup("Data saver") {
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
            }

            LargeVerticalSpacer()

            PreferencesGroup("Downloads") {
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

            PreferencesGroup(title = "Searching") {
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
                        modifier = Modifier.fillMaxWidth(),
                        title = "Set API key",
                        summary = if (!noAuthNeeded) {
                            "${currentSettings.imageSource.label} requires an API key to work properly. " +
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
                        viewModel.tagSuggestions.removeIf { tag ->
                            tag.value == currentSettings.imageSource.imageBoard.aiTagName && tag.isExcluded
                        }
                    }
                }
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Manage blocked tags",
                        summary = "Add or remove tags to block from search results."
                    ) {
                        showBlockedTagsSheet = true
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

            if (showBlockedTagsSheet) {
                BlockedTagsBottomSheet(onDismissRequest = { showBlockedTagsSheet = false })
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
            }

            LargeVerticalSpacer()

            PreferencesGroup(title = "Sharing") {
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
                               "an API key on Danbooru, but may cause less results to be shown at" +
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockedTagsBottomSheet(
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val userPreferencesRepository = LocalContext.current.prefs
    val prefs = LocalPreferences.current
    val blockedTags = prefs.manuallyBlockedTags.reversed()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    if (showAddDialog) {
        var content by remember { mutableStateOf("") }
        AlertDialog(
            title = { Text("Add blocked tag") },
            text = {
                PreferenceTextBox(
                    value = content,
                    label = "Tags",
                ) {
                    content = it
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBlocks = content.trim().split(" ")
                        scope.launch {
                            for (tag in newBlocks) {
                                userPreferencesRepository.addToSet(
                                    PreferenceKeys.MANUALLY_BLOCKED_TAGS,
                                    tag
                                )
                            }
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            onDismissRequest = { showAddDialog = false },
        )
    }

    TitledModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = "Blocked tags"
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(largerShapeCornerSize, largerShapeCornerSize, 0.dp, 0.dp))
        ) {
            LazyColumn(
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = navBarHeight + 88.dp), // FAB height + 16dp vertical padding
            ) {
                item {
                    Summary(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = "Images with any of these tags will not appear in search results or recommendations. However, they will still show in your Favourites.",
                    )
                    LargeVerticalSpacer()
                }
                if (prefs.excludeAi) {
                    val aiTags = ImageSource.entries.mapTo(mutableSetOf()) { it.imageBoard.aiTagName }
                    item {
                        BaseHeading(
                            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                            text = "Automatically blocked"
                        )
                    }
                    items(aiTags.size) { index ->
                        ExpressiveTagEntryContainer(
                            modifier = Modifier.animateItem(),
                            label = aiTags.elementAt(index),
                            position = when (index) {
                                0 -> ListItemPosition.TOP
                                aiTags.size - 1 -> ListItemPosition.BOTTOM
                                else -> ListItemPosition.MIDDLE // Not used at the time of writing but if more AI tags appear in the future when it would be useful
                            }
                        )
                    }
                    if (prefs.manuallyBlockedTags.isNotEmpty()) {
                        item { MediumLargeVerticalSpacer() }
                        item {
                            BaseHeading(
                                modifier = Modifier
                                    .padding(start = 8.dp, bottom = 6.dp)
                                    .animateItem(),
                                text = "Blocked by you"
                            )
                        }
                    }
                }
                items(blockedTags.size, key = { index -> blockedTags[index] }) { index ->
                    val tag = blockedTags[index]
                    ExpressiveTagEntryContainer(
                        modifier = Modifier.animateItem(),
                        label = tag,
                        position = when {
                            prefs.manuallyBlockedTags.size == 1 -> ListItemPosition.SINGLE_ELEMENT
                            index == 0 -> ListItemPosition.TOP
                            index == prefs.manuallyBlockedTags.size - 1 -> ListItemPosition.BOTTOM
                            else -> ListItemPosition.MIDDLE
                        },
                        trailingContent = {
                            IconButton(
                                onClick = { scope.launch {
                                    userPreferencesRepository.removeFromSet(PreferenceKeys.MANUALLY_BLOCKED_TAGS, tag)
                                } }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Unblock tag",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = navBarHeight + 16.dp, end = 4.dp)
            ) {
                Icon(Icons.Rounded.Add, "Add blocked tag")
            }
        }
    }
}


private operator fun JSONObject.contains(key: String): Boolean {
    return try {
        get(key)
        true
    } catch (_: JSONException) {
        false
    }
}
