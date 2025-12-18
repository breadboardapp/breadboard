package moe.apex.rule34.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToHexString
import moe.apex.rule34.dataStore
import moe.apex.rule34.history.SearchHistoryEntry
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.PrefCategory
import moe.apex.rule34.preferences.PreferenceKeys
import moe.apex.rule34.preferences.Prefs
import moe.apex.rule34.preferences.UserPreferencesRepository
import moe.apex.rule34.prefs
import org.json.JSONArray
import org.json.JSONObject


class ImportException(message: String) : Exception(message)


private fun exportSettingsToJson(prefs: Preferences): JSONObject {
    val json = JSONObject()

    for (item in prefs.asMap()) {
        val meta = UserPreferencesRepository.keyMetaMapping[item.key]
        if (meta == null) {
            Log.w("Exporting", "Unknown preference key: ${item.key.name}, skipping.")
            continue
        }
        if (meta.category == PrefCategory.SETTING && meta.exportable) {
            val value = if (item.value is Set<*>) { (item.value as Set<*>).toList() } else item.value
            json.put(item.key.name, value)
        }
    }

    return json
}


@OptIn(ExperimentalSerializationApi::class)
suspend fun exportData(context: Context, categories: Collection<PrefCategory>): JSONObject {
    val rawData = context.dataStore.data.first()
    val prefs = context.prefs.getPreferences.first()
    val out = JSONObject()

    for (category in categories) {
        when (category) {
            PrefCategory.BUILD -> out.put(category.name, prefs.lastUsedVersionCode)
            PrefCategory.SETTING -> out.put(category.name, exportSettingsToJson(rawData))
            PrefCategory.FAVOURITE_IMAGES -> out.put(category.name, Cbor.encodeToHexString(prefs.favouriteImages))
            PrefCategory.SEARCH_HISTORY -> out.put(category.name, Cbor.encodeToHexString(prefs.searchHistory))
        }
    }

    return out
}


fun preImportChecks(currentPreferences: Prefs, json: JSONObject): Result<Boolean> {
    if (currentPreferences.lastUsedVersionCode < json.getInt(PrefCategory.BUILD.name)) {
        return Result.failure(ImportException("Cannot import data from a newer version of Breadboard."))
    }
    return Result.success(true)
}


private fun importSettings(tempPrefs: MutablePreferences, data: JSONObject, merge: Boolean) {
    /* We can't check the inner type of each preference key because
       Preferences.Key<Int> == Preferences.Key<String> when the key names match.
       We also can't determine the type of each key so we can't directly assign prefValue to it, so
       we'll just create "new" ones with the same type and name as the originals instead.
     */
    val thisCategory = data.getJSONObject(PrefCategory.SETTING.name)
    val wantedKeysMeta = UserPreferencesRepository.keyMetaMapping
        .filter { it.value.category == PrefCategory.SETTING }
    val wantedKeys = wantedKeysMeta.keys.map { it.name }

    tempPrefs.apply {
        for (key in thisCategory.keys()) {
            if (key !in wantedKeys) {
                Log.w("Importing", "Unknown settings key: $key. Attempting to continue.")
            }

            val prefValue = thisCategory.get(key)
            val meta = wantedKeysMeta.entries.find { it.key.name == key }?.value
            val mergeThis = merge && meta?.mergeable == true

            when (prefValue) {
                is String -> {
                    if (!prefValue.startsWith("[")) {
                        this[stringPreferencesKey(key)] = prefValue
                    } else {
                        val set = mutableSetOf<String>()
                        val ja = JSONArray(prefValue)
                        for (index in 0..<ja.length()) {
                            set.add(ja.getString(index))
                        }

                        if (mergeThis) {
                            val currentSet = this[stringSetPreferencesKey(key)] ?: emptySet()
                            this[stringSetPreferencesKey(key)] = currentSet + set
                        } else {
                            this[stringSetPreferencesKey(key)] = set
                        }
                    } // Because the sets are actually serialised as strings
                }

                is Int -> this[intPreferencesKey(key)] = prefValue
                is Boolean -> this[booleanPreferencesKey(key)] = prefValue
                is Long -> this[longPreferencesKey(key)] = prefValue // At the time of writing this is only the ignore list timestamp which isn't exportable but I might change my mind
                else -> throw NotImplementedError("Settings key with unhandled type: $key ($prefValue)")
            }
        }
    }
}


@OptIn(ExperimentalSerializationApi::class)
private fun importFavouriteImages(editable: MutablePreferences, data: JSONObject, merge: Boolean) {
    val incomingImages = Cbor.decodeFromHexString(data.getString(PrefCategory.FAVOURITE_IMAGES.name)) as List<Image>
    if (merge) {
        val currentImagesRaw = editable[PreferenceKeys.FAVOURITE_IMAGES]
        val currentImages: List<Image> = currentImagesRaw?.let { Cbor.decodeFromByteArray(it) } ?: emptyList()

        val mergedImages = currentImages.toMutableList()
        for (incoming in incomingImages) {
            if (mergedImages.none { it.fileName == incoming.fileName && it.imageSource == incoming.imageSource }) {
                mergedImages.add(incoming)
            }
        }
        updateByteArrayPref(editable, data, PreferenceKeys.FAVOURITE_IMAGES, PrefCategory.FAVOURITE_IMAGES.name, mergedImages)
    } else {
        updateByteArrayPref(editable, data, PreferenceKeys.FAVOURITE_IMAGES, PrefCategory.FAVOURITE_IMAGES.name, incomingImages)
    }
}


@OptIn(ExperimentalSerializationApi::class)
private fun importSearchHistory(editable: MutablePreferences, data: JSONObject) {
    val sh = Cbor.decodeFromHexString(data.getString(PrefCategory.SEARCH_HISTORY.name)) as List<SearchHistoryEntry>
    updateByteArrayPref(editable, data, PreferenceKeys.SEARCH_HISTORY, PrefCategory.SEARCH_HISTORY.name, sh)
}


@OptIn(ExperimentalSerializationApi::class)
private inline fun <reified T> updateByteArrayPref(
    editable: MutablePreferences,
    data: JSONObject,
    prefKey: Preferences.Key<ByteArray>,
    categoryName: String,
    obj: T
) {
    val hex = data.optString(categoryName)
    if (hex.isNotEmpty()) {
        editable[prefKey] = Cbor.encodeToByteArray(obj)
    }
}


@Suppress("Deprecation")
suspend fun importData(context: Context, data: JSONObject, categories: Collection<PrefCategory>, merge: Boolean = false): Result<Boolean> {
    /* This really is not great but we're working with what we've got.

       If search history is selected for import but saving search history is disabled (i.e. disabled
       currently and the user chooses not to import the settings, the history still gets imported
       but future searches while it's disabled won't be saved. Maybe I'll address that, maybe not. */
    Log.i("Importing", data.toString())
    val current = context.dataStore.data.first()
    val currentCopy = current.toPreferences()
    val editable = currentCopy.toMutablePreferences()

    try {
        context.dataStore.edit {
            it[PreferenceKeys.LAST_USED_VERSION_CODE] = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }

        for (category in categories) {
            when (category) {
                PrefCategory.BUILD -> {
                    editable[PreferenceKeys.LAST_USED_VERSION_CODE] = data.getInt(category.name)
                }
                PrefCategory.SETTING -> importSettings(editable, data, merge)
                PrefCategory.FAVOURITE_IMAGES -> importFavouriteImages(editable, data, merge)
                PrefCategory.SEARCH_HISTORY -> importSearchHistory(editable, data)
            }
        }

        if (!context.prefs.ensureNotMalformed(editable)) {
            throw ImportException("The data is malformed or incomplete. Aborting.")
        }

        context.dataStore.updateData { editable }
        context.prefs.handleMigration(context)
    } catch (e: Exception) {
        Log.e("Importing", null, e)
        context.dataStore.edit { it.clear() }
        context.dataStore.updateData { currentCopy }
        return Result.failure(e)
    }

    return Result.success(true)
}


@Composable
fun ImportHandler(
    onFailure: () -> Unit,
    onSuccess: (Uri) -> Unit
) {
    StorageLocationSelection(
        promptType = PromptType.READ_FILE,
        onFailure = onFailure
    ) {
        onSuccess(it)
    }
}


@Composable
fun ExportDirectoryHandler(data: JSONObject, callback: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    StorageLocationSelection(PromptType.CREATE_FILE) { uri ->
        scope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(data.toString(4).toByteArray())
            }
            withContext(Dispatchers.Main) {
                showToast(context, "Successfully exported data.")
            }
        }.invokeOnCompletion {
            callback()
        }
    }
}