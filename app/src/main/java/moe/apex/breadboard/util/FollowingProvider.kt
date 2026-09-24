package moe.apex.breadboard.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import moe.apex.breadboard.image.Danbooru
import moe.apex.breadboard.image.Image
import moe.apex.breadboard.viewmodel.GridStateHolder
import moe.apex.breadboard.viewmodel.GridStateHolderDelegate

class FollowingProvider(
    initialFollowedArtists: Set<String>,
    initialBlockedTags: Set<String>,
    private val showAllRatings: Boolean,
) : GridStateHolder by GridStateHolderDelegate() {
    private val followedArtists = MutableStateFlow(initialFollowedArtists)
    private val blockedTags = MutableStateFlow(initialBlockedTags)
    private val pageNumber = MutableStateFlow(Danbooru.firstPageIndex)
    private val isLoading = MutableStateFlow(false)
    private val shouldKeepSearching = MutableStateFlow(true)
    private val _doneInitialLoad = MutableStateFlow(false)
    val doneInitialLoad = _doneInitialLoad.asStateFlow()

    fun replaceBlockedTags(tags: Set<String>) {
        blockedTags.update { tags }
    }

    fun replaceFollowedArtists(artists: Set<String>) {
        followedArtists.update { artists }
    }

    fun updateDoneInitialLoad(value: Boolean) {
        _doneInitialLoad.update { value }
    }

    private val _images = MutableStateFlow(listOf<Image>())
    val images = _images.asStateFlow()

    suspend fun loadMore() {
        if (followedArtists.value.isEmpty()) {
            _doneInitialLoad.update { true }
            return _images.update { emptyList() }
        }
        if (isLoading.value || !shouldKeepSearching.value) {
            return
        }

        try {
            isLoading.update { true }
            Log.d("FollowingProvider", "Using Breadboard proxy (showAllRatings: $showAllRatings)")

            val results = Danbooru.loadFollowingPage(
                artists = followedArtists.value.toList(),
                page = pageNumber.value,
                safe = !showAllRatings,
            )

            val filteredResults = results.filter {
                it.metadata!!.tags.none { tag -> blockedTags.value.contains(tag.lowercase()) }
            }

            if (results.isEmpty()) {
                shouldKeepSearching.update { false }
            } else {
                if (pageNumber.value == Danbooru.firstPageIndex) {
                    _images.update { filteredResults }
                } else {
                    _images.update { current ->
                        current + filteredResults.filter { it !in current }
                    }
                }
                pageNumber.update { current -> current + 1 }
            }
        } catch (e: Exception) {
            Log.e("FollowingProvider", "Error fetching following feed", e)
            shouldKeepSearching.update { false }
        } finally {
            isLoading.update { false }
            _doneInitialLoad.update { true }
        }
    }

    fun reset() {
        pageNumber.update { Danbooru.firstPageIndex }
        shouldKeepSearching.update { true }
    }
}
