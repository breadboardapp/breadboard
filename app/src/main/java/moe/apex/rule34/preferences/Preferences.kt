package moe.apex.rule34.preferences

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.ImageBoard
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.image.ImageBoardRequirement
import moe.apex.rule34.navigation.AboutSettings
import moe.apex.rule34.navigation.BlockedTagsSettings
import moe.apex.rule34.navigation.ExperimentalSettings
import moe.apex.rule34.prefs
import moe.apex.rule34.util.AgeVerification
import moe.apex.rule34.util.ChevronRight
import moe.apex.rule34.util.ExportDirectoryHandler
import moe.apex.rule34.util.ExpressiveGroup
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.StorageLocationSelection
import moe.apex.rule34.util.ImportException
import moe.apex.rule34.util.ImportHandler
import moe.apex.rule34.util.LARGE_SPACER
import moe.apex.rule34.util.PromptType
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.TitleSummary
import moe.apex.rule34.util.exportData
import moe.apex.rule34.util.importData
import moe.apex.rule34.util.launchInDefaultBrowser
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
    var showAgeVerificationDialog by remember { mutableStateOf(false) }

    val preferencesRepository = LocalContext.current.prefs
    val currentSettings = LocalPreferences.current

    if (showAuthDialog) {
        AuthDialog(
            selectedBoard = currentSettings.imageSource.imageBoard,
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

    if (showAgeVerificationDialog) {
        AgeVerification.AgeVerifyDialog(
            onDismissRequest = { showAgeVerificationDialog = false },
            onAgeVerified = {
                scope.launch {
                    preferencesRepository.updatePref(
                        PreferenceKeys.HAS_VERIFIED_AGE,
                        true
                    )
                }
                showAgeVerificationDialog = false
            }
        )
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
            var displayName = uri.lastPathSegment.toString()
            if (!displayName.endsWith(".bread")) {
                /* Android's content provider also sometimes returns URIs that hide the real file
                   path and name so we need to query the content resolver to get the correct one. */
                context.contentResolver.query(uri, null, null, null).use { cursor ->
                    if (cursor?.moveToFirst() == true) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            displayName = cursor.getString(displayNameIndex)
                        }
                    }
                }
            }
            if (!displayName.endsWith(".bread")) {
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

    MainScreenScaffold(
        title = "Settings",
        scrollBehavior = scrollBehavior,
        additionalActions = {
            var isDropdownVisible by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { isDropdownVisible = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More"
                    )
                }
                DropdownMenu(
                    expanded = isDropdownVisible,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                    onDismissRequest = { isDropdownVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("About") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "About"
                            )
                        },
                        onClick = {
                            isDropdownVisible = false
                            navController.navigate(AboutSettings)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Experimental features") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = "Experimental features"
                            )
                        },
                        onClick = {
                            isDropdownVisible = false
                            navController.navigate(ExperimentalSettings)
                        }
                    )
                }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(vertical = MEDIUM_SPACER.dp),
            verticalArrangement = Arrangement.spacedBy(LARGE_SPACER.dp)
        ) {
            item {
                ExpressiveGroup("General") {
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
                                viewModel.tagSuggestions.clear()
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
            }

            item {
                ExpressiveGroup(title = "Content filtering") {
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
                        }
                    }
                    item {
                        SwitchPref(
                            checked = currentSettings.recommendAllRatings,
                            title = "Recommend all ratings",
                            summary = "On the browse page, show images with all ratings. If disabled, " +
                                      "only show images rated Safe."
                        ) {
                            if (it && !AgeVerification.hasVerifiedAge(currentSettings)) {
                                showAgeVerificationDialog = true
                                return@SwitchPref
                            }
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.RECOMMEND_ALL_RATINGS,
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
            }

            item {
                ExpressiveGroup(title = "Interaction and layout") {
                    item {
                        EnumPref(
                            title = "Start page",
                            summary = currentSettings.defaultStartDestination.label,
                            enumItems = StartDestination.entries,
                            selectedItem = currentSettings.defaultStartDestination,
                        ) {
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.DEFAULT_START_DESTINATION,
                                    it
                                )
                            }
                        }
                    }
                    item {
                        EnumPref(
                            title = "Hide app content",
                            summary = currentSettings.flagSecureMode.label,
                            enumItems = FlagSecureMode.entries,
                            selectedItem = currentSettings.flagSecureMode
                        ) {
                            scope.launch {
                                preferencesRepository.updatePref(
                                    PreferenceKeys.FLAG_SECURE_MODE,
                                    it
                                )
                            }
                        }
                    }
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
                            dialogTitle = "Image actions",
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
            }

            item {
                ExpressiveGroup("Data and storage") {
                    item {
                        EnumPref(
                            title = "Data saver",
                            summary = currentSettings.dataSaver.label,
                            enumItems = DataSaver.entries,
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
            }

            item {
                ExpressiveGroup(title = "Import/export") {
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
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = LARGE_SPACER.dp))
            }

            item {
                InfoSection(
                    text = "When data saver is enabled, images will load in a lower resolution " +
                            "by default. Downloads will always be in the maximum resolution."
                )
            }

            if (currentSettings.imageSource.imageBoard.localFilterType != ImageBoardRequirement.NOT_NEEDED) {
                item {
                    InfoSection(
                        text = "Danbooru limits searches to 2 tags (which includes ratings) " +
                                "without an API key. If you are using Danbooru without an API key, " +
                                "you should enable 'Filter ratings locally' to filter by rating. " +
                                "Yande.re always requires this option."
                    )
                }
            }

            item {
                InfoSection(
                    text = "Filtering ratings locally has the benefit of being able to " +
                            "adjust the filter after searching and allows filtering without " +
                            "an API key on Danbooru, but may cause less results to be shown at " +
                            "once and result in higher data usage for the same number of " +
                            "visible images."
                )
            }

            item {
                InfoSection(
                    text = "When fixed links are enabled, sharing an image may use an " +
                            "alternative link depending on the source. Bluesky links are " +
                            "transformed into fxbsky.app, Pixiv links are transformed into " +
                            "phixiv.net, and Twitter links are transformed into fxtwitter.com."
                )
            }
        }
    }
}


@Composable
private fun AuthDialog(
    selectedBoard: ImageBoard,
    default: ImageBoardAuth?,
    onDismissRequest: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var userId by remember { mutableStateOf(default?.user ?: "") }
    var apiKey by remember { mutableStateOf(default?.apiKey ?: "") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Set API key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PreferenceTextBox(
                    value = userId,
                    label = "User ID/name",
                    obscured = false
                ) {
                    userId = it.trim()
                }
                PreferenceTextBox(
                    value = apiKey,
                    label = "API key",
                    keyboardType = KeyboardType.Password,
                    obscured = true
                ) {
                    apiKey = it.trim()
                }

                selectedBoard.apiKeyCreationUrl?.let { url ->
                    val apiKeyCreationText = buildAnnotatedString {
                        val link = LinkAnnotation.Url(
                            url,
                            TextLinkStyles(
                                SpanStyle(color = MaterialTheme.colorScheme.secondary, textDecoration = TextDecoration.Underline)
                            )
                        ) {
                            launchInDefaultBrowser(context, url)
                        }

                        withLink(link) {
                            append("Find your credentials...")
                        }
                    }

                    Text(
                        text = apiKeyCreationText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
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
