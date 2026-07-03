package moe.apex.breadboard.preferences

import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.breadboard.prefs
import moe.apex.breadboard.util.ExportDirectoryHandler
import moe.apex.breadboard.util.ImportException
import moe.apex.breadboard.util.ImportHandler
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.LazyExpressiveGroup
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.PromptType
import moe.apex.breadboard.util.StorageLocationSelection
import moe.apex.breadboard.util.TitleSummary
import moe.apex.breadboard.util.exportData
import moe.apex.breadboard.util.importData
import moe.apex.breadboard.util.preImportChecks
import moe.apex.breadboard.util.saveUriToPref
import moe.apex.breadboard.util.showToast
import org.json.JSONException
import org.json.JSONObject
import java.io.FileInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSettingsScreen(navController: NavHostController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var storageLocationPromptLaunched by remember { mutableStateOf(false) }

    val currentSettings = LocalPreferences.current

    if (storageLocationPromptLaunched) {
        StorageLocationSelection(
            promptType = PromptType.DIRECTORY_PERMISSION,
            onFailure = { storageLocationPromptLaunched = false }
        ) { uri ->
            saveUriToPref(context, scope, uri)
            storageLocationPromptLaunched = false
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedData: JSONObject? by remember { mutableStateOf(null) }
    var importedData: JSONObject? by remember { mutableStateOf(null) }
    var importingStarted by rememberSaveable { mutableStateOf(false) }

    if (showExportDialog) {
        val categories = remember { PrefCategory.entries.toMutableStateList() }
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
        ) { categories, merge ->
            scope.launch {
                val result = importData(context, importedData!!, categories, merge)
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
        topAppBar = {
            LargeTitleBar(
                title = "Data and storage",
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

            LazyExpressiveGroup("Your Breadboard data") {
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Export data",
                        summary = "Export a backup file containing your current settings, favourite images, " +
                                  "and search history. Downloads are not included."
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

