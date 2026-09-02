package moe.apex.breadboard.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import moe.apex.breadboard.image.Image
import moe.apex.breadboard.image.ImageBoardAuth
import moe.apex.breadboard.image.ImageBoardRequirement
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.viewmodel.GridStateHolder
import moe.apex.breadboard.viewmodel.GridStateHolderDelegate

class RecommendationsProvider(
    private val seedImages: List<Image>,
    val imageSource: ImageSource,
    val auth: ImageBoardAuth?,
    val showAllRatings: Boolean,
    val filterRatingsLocally: Boolean,
    initialBlockedTags: Set<String>,
    initialUnfollowedTags: Set<String>,
    private val selectionSize: Int,
    private val poolSize: Int,
) : GridStateHolder by GridStateHolderDelegate() {
    companion object {
        private const val SELECTION_SIZE_DANBOORU = 2
    }

    private val _recommendedImages = MutableStateFlow(listOf<Image>())
    val recommendedImages = _recommendedImages.asStateFlow()

    private val _doneInitialLoad = MutableStateFlow(false)
    val doneInitialLoad = _doneInitialLoad.asStateFlow()

    private val _recommendedTags = MutableStateFlow(listOf<String>())
    val recommendedTags = _recommendedTags.asStateFlow()

    private val pageNumber = MutableStateFlow(imageSource.imageBoard.firstPageIndex)
    private val isLoading = MutableStateFlow(false)
    private val shouldKeepSearching = MutableStateFlow(true)

    private val _blockedTags = MutableStateFlow(initialBlockedTags)
    val blockedTags = _blockedTags.asStateFlow()

    private val _unfollowedTags = MutableStateFlow(initialUnfollowedTags)

    fun replaceBlockedTags(tags: Set<String>) {
        _blockedTags.update { tags }
    }

    fun replaceUnfollowedTags(tags: Set<String>) {
        _unfollowedTags.update { tags }
    }

    fun updateImage(index: Int, newImage: Image) {
        _recommendedImages.update { current ->
            current.toMutableList().apply { this[index] = newImage }
        }
    }

    fun prepareRecommendedTags() {
        _recommendedTags.update { emptyList() }
        shouldKeepSearching.update { true }
        pageNumber.update { imageSource.imageBoard.firstPageIndex }

        val filteredSeedImages = seedImages
            .filter { it.imageSource == imageSource }
            .filter { showAllRatings || it.metadata?.rating == ImageRating.SAFE }

        if (filteredSeedImages.isEmpty()) {
            return
        }

        val finalSelectionSize = if (imageSource == ImageSource.DANBOORU && auth == null) SELECTION_SIZE_DANBOORU else selectionSize

        val selected = RecommendationsHelper.getRecommendedTags(
            images = filteredSeedImages,
            selectionSize = finalSelectionSize,
            poolSize = poolSize,
            hiddenTags = _blockedTags.value,
            unfollowedTags = _unfollowedTags.value
        )

        _recommendedTags.update { selected }
    }

    suspend fun recommendImages() {
        if (isLoading.value || !shouldKeepSearching.value) {
            return
        }

        val currentTags = _recommendedTags.value
        val currentPage = pageNumber.value

        Log.i(
            "Recommendations",
            "Fetching recommended posts for tags: ${currentTags.joinToString(", ")} - page $currentPage"
        )
        val filterRatingsLocally = filterRatingsLocally ||
                imageSource.imageBoard.localFilterType == ImageBoardRequirement.REQUIRED ||
                (imageSource == ImageSource.DANBOORU && auth == null)

        val searchQuery = if (filterRatingsLocally) {
            Log.i(
                "Recommendations",
                "Filtering recommendations locally because either the local filter is enabled, or the image source does not support server-side filtering."
            )
            imageSource.imageBoard.formatTagNameString(currentTags)
        } else {
            "${imageSource.imageBoard.formatTagNameString(currentTags)} ${
                ImageRating.buildSearchStringFor(
                    if (showAllRatings) {
                        ImageRating.entries.filter { it != ImageRating.UNKNOWN }
                    } else {
                        listOf(ImageRating.SAFE)
                    }
                )
            }"
        }

        try {
            isLoading.update { true }
            // if recommendedTags is empty, it should just return the most recent uploaded posts
            val results = imageSource.imageBoard.loadPage(
                tags = searchQuery,
                page = currentPage,
                auth = auth,
            )

            val safeResults = results.filter {
                if (filterRatingsLocally) {
                    showAllRatings || it.metadata!!.rating == ImageRating.SAFE
                } else true
            }

            val currentBlocked = _blockedTags.value
            val wantedResults = safeResults.filter {
                it.metadata!!.tags.none { tag -> currentBlocked.contains(tag.lowercase()) }
            }

            Log.i("Recommendations", "Found ${results.size} new images for tags: ${currentTags.joinToString(", ")}")
            Log.i("Recommendations", "Found ${safeResults.size} safe images for tags: ${currentTags.joinToString(", ")}")
            Log.i("Recommendations", "Found ${wantedResults.size} wanted images for tags: ${currentTags.joinToString(", ")}")

            if (results.isEmpty() || safeResults.isEmpty()) {
                shouldKeepSearching.update { false }
            } else {
                if (currentPage == imageSource.imageBoard.firstPageIndex) {
                    _recommendedImages.update { wantedResults }
                } else if (wantedResults.isNotEmpty()) {
                    _recommendedImages.update { current ->
                        current + wantedResults.filter { it !in current }
                    }
                }
            }
            pageNumber.update { it + 1 }
        } catch (e: Exception) {
            Log.e(
                "Recommendations",
                "Error fetching images with recommended tags: ${e.message}"
            )
            shouldKeepSearching.update { false }
        } finally {
            isLoading.update { false }
            _doneInitialLoad.update { true }
        }
    }
}
