package moe.apex.rule34.preferences

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.prefs
import moe.apex.rule34.util.ExportDirectoryHandler
import moe.apex.rule34.util.VerticalSpacer
import moe.apex.rule34.util.Heading
import moe.apex.rule34.util.LargeVerticalSpacer
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.NavBarHeightVerticalSpacer
import moe.apex.rule34.util.StorageLocationSelection
import moe.apex.rule34.util.ImportException
import moe.apex.rule34.util.ImportHandler
import moe.apex.rule34.util.PromptType
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
fun PreferencesScreen(viewModel: BreadboardViewModel) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var storageLocationPromptLaunched by remember { mutableStateOf(false) }
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

            Heading(text = "Data saver")
            EnumPref(
                title = "Data saver",
                summary = currentSettings.dataSaver.label,
                enumItems = DataSaver.entries.toTypedArray(),
                selectedItem = currentSettings.dataSaver,
                onSelection = { scope.launch { preferencesRepository.updatePref(PreferenceKeys.DATA_SAVER, it) } }
            )

            LargeVerticalSpacer()

            Heading(text = "Downloads")
            TitleSummary(
                modifier = Modifier.fillMaxWidth(),
                title = "Save downloads to",
                summary = if (currentSettings.storageLocation == Uri.EMPTY) "Tap to set"
                else currentSettings.storageLocation.toString()
            ) {
                storageLocationPromptLaunched = true
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

            Heading(text = "Searching")
            EnumPref(
                title = "Image source",
                summary = currentSettings.imageSource.label,
                enumItems = ImageSource.entries.toTypedArray(),
                selectedItem = currentSettings.imageSource,
                onSelection = {
                    scope.launch { preferencesRepository.updatePref(PreferenceKeys.IMAGE_SOURCE, it) }
                    viewModel.tagSuggestions.clear()
                }
            )
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
            SwitchPref(
                checked = currentSettings.excludeAi,
                title = "Hide AI-generated images",
                summary = "Attempt to remove AI-generated images by excluding the " +
                          "'ai_generated' tag in search queries by default."
            ) {
                scope.launch { preferencesRepository.updatePref(PreferenceKeys.EXCLUDE_AI, it) }
                viewModel.tagSuggestions.removeIf { tag ->
                    tag.value == currentSettings.imageSource.site.aiTagName && tag.isExcluded
                }
            }
            SwitchPref(
                checked = currentSettings.filterRatingsLocally,
                title = "Filter ratings locally",
                summary = "Rather than appending the selected ratings to the search query, " +
                          "filter the results by rating after searching."
            ) {
                scope.launch { preferencesRepository.updatePref(PreferenceKeys.FILTER_RATINGS_LOCALLY, it) }
            }

            LargeVerticalSpacer()

            Heading(text = "Layout")
            SwitchPref(
                checked = currentSettings.useStaggeredGrid,
                title = "Staggered grid",
                summary = "Use a staggered grid for images rather than a uniform grid."
            ) {
                scope.launch { preferencesRepository.updatePref(PreferenceKeys.USE_STAGGERED_GRID, it) }
            }

            LargeVerticalSpacer()

            Heading(text = "Import/export")
            var showExportDialog by remember { mutableStateOf(false) }
            var exportedData: JSONObject? by remember { mutableStateOf(null) }
            var importedData: JSONObject? by remember { mutableStateOf(null) }
            var importingStarted by rememberSaveable { mutableStateOf(false) }

            TitleSummary(
                modifier = Modifier.fillMaxWidth(),
                title = "Export data",
                summary = "Export a backup file containing your current settings, favourite images, " +
                          "and search history."
            ) {
                showExportDialog = true
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

            TitleSummary(
                modifier = Modifier.fillMaxWidth(),
                title = "Import data",
                summary = "Import a Breadboard backup file."
            ) {
                importingStarted = true
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

            HorizontalDivider(Modifier.padding(vertical = 48.dp))

            InfoSection(text = "When data saver is enabled, images will load in a lower resolution " +
                               "by default. Downloads will always be in the maximum resolution.")
            AnimatedVisibility(currentSettings.imageSource == ImageSource.DANBOORU) {
                Column {
                    LargeVerticalSpacer()
                    InfoSection(
                        text = "Danbooru limits searches to 2 tags (which includes ratings), " +
                                "so filtering by rating is difficult. If you are using " +
                                "Danbooru, you should enable 'Filter ratings locally' if " +
                                "you wish to filter by rating."
                    )
                }
            }
            LargeVerticalSpacer()
            InfoSection(text = "Filtering ratings locally has the benefit of being able to " +
                               "adjust the filter after searching and allows filtering on " +
                               "otherwise unsupported sites like Danbooru, but may cause " +
                               "less results to be shown at once and result in higher data "+
                               "usage for the same number of visible images.")
            NavBarHeightVerticalSpacer()
        }
    }
}


private operator fun JSONObject.contains(key: String): Boolean {
    return try {
        get(key)
        true
    } catch (e: JSONException) {
        false
    }
}
