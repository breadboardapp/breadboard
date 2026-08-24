package moe.apex.breadboard.util

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import moe.apex.breadboard.image.Danbooru
import moe.apex.breadboard.image.Image
import moe.apex.breadboard.viewmodel.GridStateHolder
import moe.apex.breadboard.viewmodel.GridStateHolderDelegate

class FollowingProvider(
    followedArtists: Set<String>,
    private val showAllRatings: Boolean,
    private val initialBlockedTags: Set<String>,
) : GridStateHolder by GridStateHolderDelegate() {
    private val mutableFollowedArtists = mutableStateSetOf<String>().apply { addAll(followedArtists) }
    val followedArtists: Set<String>
        get() = mutableFollowedArtists.toSet()

    private val mutableBlockedTags = mutableStateSetOf<String>().apply { addAll(initialBlockedTags) }
    val blockedTags: Set<String>
        get() = mutableBlockedTags.toSet()

    fun replaceBlockedTags(tags: Set<String>) {
        Snapshot.withMutableSnapshot {
            mutableBlockedTags.clear()
            mutableBlockedTags.addAll(tags)
        }
    }

    fun replaceFollowedArtists(artists: Set<String>) {
        Snapshot.withMutableSnapshot {
            mutableFollowedArtists.clear()
            mutableFollowedArtists.addAll(artists)
        }
    }

    private val _images = MutableStateFlow(listOf<Image>())
    val images = _images.asStateFlow()

    var doneInitialLoad by mutableStateOf(false)
    private var pageNumber by mutableIntStateOf(Danbooru.firstPageIndex)

    private var isLoading by mutableStateOf(false)
    private var shouldKeepSearching by mutableStateOf(true)

    suspend fun loadMore() {
        if (followedArtists.isEmpty()) {
            doneInitialLoad = true
            return _images.update { emptyList() }
        }
        if (isLoading || !shouldKeepSearching) {
            return
        }

        try {
            isLoading = true
            Log.d("FollowingProvider", "Using Breadboard proxy (showAllRatings: $showAllRatings)")
            
            val results = Danbooru.loadFollowingPage(
                artists = followedArtists.toList(),
                page = pageNumber,
                safe = !showAllRatings,
            )
            
            val filteredResults = results.filter {
                it.metadata!!.tags.none { tag -> blockedTags.contains(tag.lowercase()) }
            }

            if (results.isEmpty()) {
                shouldKeepSearching = false
            } else {
                if (pageNumber == Danbooru.firstPageIndex) {
                    _images.update { filteredResults }
                } else {
                    _images.update { current ->
                        current + filteredResults.filter { it !in current }
                    }
                }
                pageNumber++
            }
        } catch (e: Exception) {
            Log.e("FollowingProvider", "Error fetching following feed", e)
            shouldKeepSearching = false
        } finally {
            isLoading = false
            doneInitialLoad = true
        }
    }

    fun reset() {
        pageNumber = Danbooru.firstPageIndex
        shouldKeepSearching = true
    }
}
