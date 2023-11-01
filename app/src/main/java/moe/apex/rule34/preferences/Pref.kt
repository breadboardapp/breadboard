package moe.apex.rule34.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException


enum class DataSaver(val description: String) {
    ON ("Always"),
    OFF ("Never"),
    AUTO ("When using mobile data")
}


data class Prefs(
    val dataSaver: DataSaver
)


class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferenceKeys {
        val DATA_SAVER = stringPreferencesKey("data_saver")
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

    private fun mapUserPreferences(preferences: Preferences): Prefs {
        // Get the sort order from preferences and convert it to a [SortOrder] object
        val dataSaver =
            DataSaver.valueOf(
                preferences[PreferenceKeys.DATA_SAVER] ?: DataSaver.AUTO.name
            )

        return Prefs(dataSaver)
    }

}