package moe.apex.breadboard.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import moe.apex.breadboard.dataStore
import moe.apex.breadboard.preferences.PreferenceKeys
import kotlin.time.Duration


suspend fun <T> saveWithTimestamp(
    context: Context,
    timestampKey: Preferences.Key<Long>,
    dataKey: Preferences.Key<T>,
    data: T,
    timestamp: Long = System.currentTimeMillis()
) {
    context.dataStore.edit { prefs ->
        prefs[timestampKey] = timestamp
        prefs[dataKey] = data
    }
}


suspend fun saveIgnoreListWithTimestamp(
    context: Context,
    data: Set<String>,
    timestamp: Long = System.currentTimeMillis()
) {
    saveWithTimestamp(
        context = context,
        timestampKey = PreferenceKeys.INTERNAL_IGNORE_LIST_TIMESTAMP,
        dataKey = PreferenceKeys.INTERNAL_IGNORE_LIST,
        data = data,
        timestamp = timestamp
    )
}


fun differenceOlderThan(duration: Duration, start: Long, end: Long = System.currentTimeMillis()): Boolean {
    return end - start >= duration.inWholeMilliseconds
}
