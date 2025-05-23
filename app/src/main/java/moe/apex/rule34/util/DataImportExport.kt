package moe.apex.rule34.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
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
import org.json.JSONException
import org.json.JSONObject


class ImportException(message: String) : Exception(message)


private fun exportSettingsToJson(prefs: Preferences): JSONObject {
    val json = JSONObject()

    for (item in prefs.asMap()) {
        val meta = UserPreferencesRepository.keyMetaMapping[item.key] ?: throw NotImplementedError("Unknown key ${item.key}")
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


private suspend fun importSettings(context: Context, data: JSONObject) {
    /* We can't check the inner type of each preference key because
       Preferences.Key<Int> == Preferences.Key<String> when the key names match.
       We also can't determine the type of each key so we can't directly assign prefValue to it, so
       we'll just create "new" ones with the same type and name as the originals instead.
     */
    val thisCategory = data.getJSONObject(PrefCategory.SETTING.name)
    context.dataStore.edit {
        for (key in UserPreferencesRepository.keyMetaMapping.keys) {
            if (UserPreferencesRepository.keyMetaMapping[key]!!.category != PrefCategory.SETTING) {
                continue
            }

            val prefValue = try {
                thisCategory.get(key.name)
            } catch (e: JSONException) {
                it.remove(key)
                continue
            } // Pref has never been set in the backup, we should use the default.

            when (prefValue) {
                is String -> {
                    if (!prefValue.startsWith("[")) {
                        it[stringPreferencesKey(key.name)] = prefValue
                    } else {
                        val set = mutableSetOf<String>()
                        val ja = JSONArray(prefValue)
                        for (index in 0..<ja.length()) {
                            set.add(ja.getString(index))
                        }
                        it[stringSetPreferencesKey(key.name)] = set
                    } // Because the sets are actually serialised as strings
                }

                is Int -> it[intPreferencesKey(key.name)] = prefValue
                is Boolean -> it[booleanPreferencesKey(key.name)] = prefValue
                else -> throw NotImplementedError("Settings key with unhandled type: ${key.name}")
            }
        }
    }
}


@OptIn(ExperimentalSerializationApi::class)
private suspend fun importFavouriteImages(context: Context, data: JSONObject) {
    val images = Cbor.decodeFromHexString(data.getString(PrefCategory.FAVOURITE_IMAGES.name)) as List<Image>
    updateByteArrayPref(context, data, PreferenceKeys.FAVOURITE_IMAGES, PrefCategory.FAVOURITE_IMAGES.name, images)
}


@OptIn(ExperimentalSerializationApi::class)
private suspend fun importSearchHistory(context: Context, data: JSONObject) {
    val sh = Cbor.decodeFromHexString(data.getString(PrefCategory.SEARCH_HISTORY.name)) as List<SearchHistoryEntry>
    updateByteArrayPref(context, data, PreferenceKeys.SEARCH_HISTORY, PrefCategory.SEARCH_HISTORY.name, sh)
}


@OptIn(ExperimentalSerializationApi::class)
private suspend inline fun <reified T> updateByteArrayPref(
    context: Context,
    data: JSONObject,
    prefKey: Preferences.Key<ByteArray>,
    categoryName: String,
    obj: T
) {
    val hex = data.optString(categoryName)
    if (hex.isNotEmpty()) {
        context.dataStore.edit { it[prefKey] = Cbor.encodeToByteArray(obj) }
    }
}


@Suppress("Deprecation")
suspend fun importData(context: Context, data: JSONObject, categories: Collection<PrefCategory>): Result<Boolean> {
    /* This really is not great but we're working with what we've got.

       If search history is selected for import but saving search history is disabled (i.e. disabled
       currently and the user chooses not to import the settings, the history still gets imported
       but future searches while it's disabled won't be saved. Maybe I'll address that, maybe not. */
    Log.i("Importing", data.toString())
    val current = context.dataStore.data.first()
    val currentCopy = current.toPreferences()

    try {
        context.dataStore.edit {
            it[PreferenceKeys.LAST_USED_VERSION_CODE] = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }

        for (category in categories) {
            when (category) {
                PrefCategory.BUILD -> {
                    context.dataStore.edit {
                        it[PreferenceKeys.LAST_USED_VERSION_CODE] = data.getInt(category.name)
                    }
                }
                PrefCategory.SETTING -> importSettings(context, data)
                PrefCategory.FAVOURITE_IMAGES -> importFavouriteImages(context, data)
                PrefCategory.SEARCH_HISTORY -> importSearchHistory(context, data)
            }
        }

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
    var directoryPickerVisibility by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    StorageLocationSelection(
        promptType = PromptType.CREATE_FILE,
        onFailure = { directoryPickerVisibility = false }
    ) { uri ->
        scope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(data.toString(4).toByteArray())
            }
            withContext(Dispatchers.Main) {
                showToast(context, "Successfully exported data.")
            }
        }.invokeOnCompletion {
            directoryPickerVisibility = false
            callback()
        }
    }
}