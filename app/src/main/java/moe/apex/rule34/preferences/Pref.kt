package moe.apex.rule34.preferences

import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import moe.apex.rule34.image.Image
import java.io.IOException


enum class DataSaver(val description: String) {
    ON ("Always"),
    OFF ("Never"),
    AUTO ("When using mobile data")
}


data class Prefs(
    val dataSaver: DataSaver,
    val storageLocation: Uri,
    val favouriteImages: List<Image>
) {
    companion object {
        val DEFAULT = Prefs(DataSaver.AUTO, Uri.EMPTY, emptyList())
    }
}


class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private object PreferenceKeys {
        val DATA_SAVER = stringPreferencesKey("data_saver")
        val STORAGE_LOCATION = stringPreferencesKey("storage_location")
        val FAVOURITE_IMAGES = byteArrayPreferencesKey("favourite_images")
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

    @OptIn(ExperimentalSerializationApi::class)
    private fun mapUserPreferences(preferences: Preferences): Prefs {
        val dataSaver = DataSaver.valueOf(
                preferences[PreferenceKeys.DATA_SAVER] ?: DataSaver.AUTO.name
        )
        val storageLocation = Uri.parse(preferences[PreferenceKeys.STORAGE_LOCATION] ?: "")
        val favouriteImagesRaw = preferences[PreferenceKeys.FAVOURITE_IMAGES]
        val favouriteImages: List<Image> = favouriteImagesRaw?.let { Cbor.decodeFromByteArray(it) } ?: emptyList()

        return Prefs(dataSaver, storageLocation, favouriteImages)
    }

}


@OptIn(ExperimentalSerializationApi::class)
private fun List<Image>.encodeToByteArray(): ByteArray {
    return Cbor.encodeToByteArray(this)
}