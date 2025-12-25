package moe.apex.rule34.tag

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import moe.apex.rule34.R
import moe.apex.rule34.RequestUtil
import org.json.JSONArray


object IgnoredTagsHelper {
    private const val URL = "https://raw.githubusercontent.com/breadboardapp/tags/refs/heads/main/tags.json"


    /** Try to fetch the most recent list of ignored tags from the repository.
     *  If it fails, use the default list.
     *
     *  If the online fetch succeeds, `onSuccess` will be called with a set representing the
     *  new blocked tags.
     *
     *  If the online fetch fails, `onFailure` will be called with a set representing the tags
     *  locally stored in Breadboard's resources. */
    suspend fun fetchTagListOnline(
        context: Context,
        onSuccess: (suspend (Set<String>) -> Unit)? = null,
        onFailure: (suspend (Set<String>) -> Unit)? = null,
    ) {
        val tags = withContext(Dispatchers.IO) {
            try {
                withTimeout(1000L) {
                    val data = RequestUtil.get(URL).get()
                    JSONArray(data)
                }
            } catch (e: Exception) {
                Log.e("IgnoredTagsHelper", "Failed to fetch ignored tags list. Using built-in list.", e)
                null
            }
        }

        val ignoredTags = tags?.let {
            Log.i("IgnoredTagsHelper", "Successfully fetched online ignored tags list (${it.length()} tags).")
            mutableSetOf<String>().apply {
                for (i in 0 until it.length()) {
                    add(it.getString(i))
                }
            }
        } ?: fetchTagListOffline(context)

        if (tags != null) {
            onSuccess?.invoke(ignoredTags)
        } else {
            onFailure?.invoke(ignoredTags)
        }
    }


    /** Fetch the local copy of the ignored tags list from Breadboard's resources. */
    fun fetchTagListOffline(context: Context): Set<String> {
        Log.i("IgnoredTagsHelper", "Getting ignored tags list from app resources.")
        return getDefaultIgnoredTags(context)
    }


    private fun getDefaultIgnoredTags(context: Context): Set<String> {
        return context.resources.getStringArray(R.array.default_ignored_tags).toSet()
    }
}
