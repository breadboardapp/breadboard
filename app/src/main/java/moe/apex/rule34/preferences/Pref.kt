package moe.apex.rule34.preferences

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import moe.apex.rule34.image.Danbooru
import moe.apex.rule34.image.Gelbooru
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageBoard
import moe.apex.rule34.image.Rule34
import moe.apex.rule34.image.Safebooru
import java.io.IOException


val LocalPreferences = compositionLocalOf {
    Prefs.DEFAULT
}


interface PrefEnum<T : Enum<T>> {
    val description: String
}


data object PrefNames {
    const val DATA_SAVER = "data_saver"
    const val STORAGE_LOCATION = "storage_location"
    const val FAVOURITE_IMAGES = "favourite_images"
    const val EXCLUDE_AI = "exclude_ai"
    const val IMAGE_SOURCE = "image_source"
    const val FAVOURITES_FILTER = "favourites_filter"
    const val LAST_USED_VERSION_CODE = "last_used_version_code"
}


enum class DataSaver(override val description: String) : PrefEnum<DataSaver> {
    ON ("Always"),
    OFF ("Never"),
    AUTO ("When using mobile data")
}


data class Prefs(
    val dataSaver: DataSaver,
    val storageLocation: Uri,
    val favouriteImages: List<Image>,
    val excludeAi: Boolean,
    val imageSource: ImageSource,
    val favouritesFilter: List<ImageSource>,
    val lastUsedVersionCode: Int,
) {
    companion object {
        val DEFAULT = Prefs(
            DataSaver.AUTO,
            Uri.EMPTY,
            emptyList(),
            false, ImageSource.SAFEBOORU,
            ImageSource.entries.toList(),
            0, // We'll update this later
        )
    }
}


class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private object PreferenceKeys {
        val DATA_SAVER = stringPreferencesKey(PrefNames.DATA_SAVER)
        val STORAGE_LOCATION = stringPreferencesKey(PrefNames.STORAGE_LOCATION)
        val FAVOURITE_IMAGES = byteArrayPreferencesKey(PrefNames.FAVOURITE_IMAGES)
        val EXCLUDE_AI = booleanPreferencesKey(PrefNames.EXCLUDE_AI)
        val IMAGE_SOURCE = stringPreferencesKey(PrefNames.IMAGE_SOURCE)
        val FAVOURITES_FILTER = stringSetPreferencesKey(PrefNames.FAVOURITES_FILTER)
        val LAST_USED_VERSION_CODE = intPreferencesKey(PrefNames.LAST_USED_VERSION_CODE)
    }

    val getPreferences: Flow<Prefs> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                Log.e("preferences", "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            mapUserPreferences(preferences)
        }

    suspend fun handleMigration(context: Context) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val isOnFirstInstallVersion = packageInfo.firstInstallTime == packageInfo.lastUpdateTime

        val currentPreferences = dataStore.data.first()
        val lastUsedVersionCode = currentPreferences[PreferenceKeys.LAST_USED_VERSION_CODE] ?: 0

        if (isOnFirstInstallVersion) {
            return updateLastUsedVersionCode(packageInfo)
        }

        /* Version code 230 introduced app version tracking and also changed the default source from
           R34 to Safebooru but we don't want to change it for existing users.
           If the last used version code is 0 (i.e. before 2.3.0) and isFirstRun is false then it
           means the user has updated the app. */
        if (lastUsedVersionCode == 0)
            updateImageSource(ImageSource.R34)


        // Place any future migrations above this line by checking the last used version code.
        if (getCurrentRunningVersionCode(packageInfo) >= lastUsedVersionCode)
            updateLastUsedVersionCode(packageInfo)
    }


    suspend fun updateDataSaver(to: DataSaver) {
        // updateData handles data transactionally, ensuring that if the sort is updated at the same
        // time from another thread, we won't have conflicts
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DATA_SAVER] = to.name
        }
    }

    suspend fun updateStorageLocation(to: Uri) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.STORAGE_LOCATION] = to.toString()
        }
    }

    private suspend fun updateFavouriteImages(to: List<Image>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FAVOURITE_IMAGES] = to.encodeToByteArray()
        }
    }

    suspend fun addFavouriteImage(image: Image) {
        val images = getPreferences.first().favouriteImages.toMutableList().apply { add(image) }
        updateFavouriteImages(images)
    }

    suspend fun removeFavouriteImage(image: Image) {
        val images = getPreferences.first().favouriteImages.toMutableList().apply { remove(image) }
        updateFavouriteImages(images)
    }

    suspend fun updateExcludeAi(to: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.EXCLUDE_AI] = to
        }
    }

    suspend fun updateImageSource(to: ImageSource) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IMAGE_SOURCE] = to.name
        }
    }

    private suspend fun updateFavouritesFilter(to: MutableSet<ImageSource>) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.FAVOURITES_FILTER] = to.mapTo(mutableSetOf()) { it.name }
        }
    }

    suspend fun addFavouritesFilter(source: ImageSource) {
        val sources = getPreferences.first().favouritesFilter.toMutableSet().apply { add(source) }
        updateFavouritesFilter(sources)
    }

    suspend fun removeFavouritesFilter(source: ImageSource) {
        val sources = getPreferences.first().favouritesFilter.toMutableSet().apply { remove(source) }
        updateFavouritesFilter(sources)
    }

    @Suppress("Deprecation")
    private fun getCurrentRunningVersionCode(packageInfo: PackageInfo): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode.toInt()
        else packageInfo.versionCode
    }

    private suspend fun updateLastUsedVersionCode(packageInfo: PackageInfo) {
        val versionCode = getCurrentRunningVersionCode(packageInfo)

        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_USED_VERSION_CODE] = versionCode
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    private fun mapUserPreferences(preferences: Preferences): Prefs {
        val dataSaver = DataSaver.valueOf(
            preferences[PreferenceKeys.DATA_SAVER] ?: DataSaver.AUTO.name
        )
        val storageLocation = Uri.parse(preferences[PreferenceKeys.STORAGE_LOCATION] ?: "")
        val favouriteImagesRaw = preferences[PreferenceKeys.FAVOURITE_IMAGES]
        val favouriteImages: List<Image> = favouriteImagesRaw?.let { Cbor.decodeFromByteArray(it) } ?: emptyList()
        val excludeAi = preferences[PreferenceKeys.EXCLUDE_AI] ?: false
        val imageSource = ImageSource.valueOf(preferences[PreferenceKeys.IMAGE_SOURCE] ?: ImageSource.SAFEBOORU.name)
        val favouritesFilter = (
            preferences[PreferenceKeys.FAVOURITES_FILTER]?.map { ImageSource.valueOf(it) } ?: ImageSource.entries
        )
        val lastUsedVersionCode = preferences[PreferenceKeys.LAST_USED_VERSION_CODE] ?: 0

        return Prefs(dataSaver, storageLocation, favouriteImages, excludeAi, imageSource, favouritesFilter, lastUsedVersionCode)
    }

}


@OptIn(ExperimentalSerializationApi::class)
private fun List<Image>.encodeToByteArray(): ByteArray {
    return Cbor.encodeToByteArray(this)
}


enum class ImageSource(override val description: String, val site: ImageBoard) : PrefEnum<ImageSource> {
    SAFEBOORU("Safebooru", Safebooru()),
    DANBOORU("Danbooru", Danbooru()),
    GELBOORU("Gelbooru", Gelbooru()),
    R34("Rule34", Rule34())
}