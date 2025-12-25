package moe.apex.rule34.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import moe.apex.rule34.dataStore
import moe.apex.rule34.preferences.PreferenceKeys
import kotlin.time.Duration


suspend fun <T> saveWithTimestamp(
    context: Context,
    timestampKey: Preferences.Key<Long>,
    dataKey: Preferences.Key<T>,
    data: T
) {
    context.dataStore.edit { prefs ->
        prefs[timestampKey] = System.currentTimeMillis()
        prefs[dataKey] = data
    }
}


suspend fun saveIgnoreListWithTimestamp(context: Context, data: Set<String>) {
    saveWithTimestamp(
        context = context,
        timestampKey = PreferenceKeys.INTERNAL_IGNORE_LIST_TIMESTAMP,
        dataKey = PreferenceKeys.INTERNAL_IGNORE_LIST,
        data = data
    )
}


fun differenceOlderThan(duration: Duration, start: Long, end: Long = System.currentTimeMillis()): Boolean {
    return end - start >= duration.inWholeMilliseconds
}
