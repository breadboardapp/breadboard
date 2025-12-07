package moe.apex.rule34.util

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.image.ImageBoardRequirement
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.viewmodel.GridStateHolderDelegate
import moe.apex.rule34.viewmodel.GridStateHolder


class RecommendationsProvider(
    private val seedImages: List<Image>,
    val imageSource: ImageSource,
    val auth: ImageBoardAuth?,
    val showAllRatings: Boolean,
    val filterRatingsLocally: Boolean,
    private val initialBlockedTags: Set<String>,
    private val initialUnfollowedTags: Set<String>,
    private val selectionSize: Int,
    private val poolSize: Int,
) : GridStateHolder by GridStateHolderDelegate() {
    companion object {
        private const val SELECTION_SIZE_DANBOORU = 2
    }

    val recommendedImages = mutableStateListOf<Image>()
    var doneInitialLoad by mutableStateOf(false)
    val recommendedTags = mutableListOf<String>()
    private var pageNumber by mutableIntStateOf(imageSource.imageBoard.firstPageIndex)

    private var isLoading by mutableStateOf(false)
    private var shouldKeepSearching by mutableStateOf(true)

    private val mutableBlockedTags = mutableStateSetOf<String>().apply { addAll(initialBlockedTags) }
    val blockedTags: Set<String>
        get() = mutableBlockedTags.toSet()
    private val mutableUnfollowedTags = mutableStateSetOf<String>().apply { addAll(initialUnfollowedTags) }
    val unfollowedTags: Set<String>
        get() = mutableUnfollowedTags.toSet()

    fun replaceBlockedTags(tags: Set<String>) {
        Snapshot.withMutableSnapshot {
            mutableBlockedTags.clear()
            mutableBlockedTags.addAll(tags)
        }
    }

    fun replaceUnfollowedTags(tags: Set<String>) {
        Snapshot.withMutableSnapshot {
            mutableUnfollowedTags.clear()
            mutableUnfollowedTags.addAll(tags)
        }
    }

    fun prepareRecommendedTags() {
        recommendedTags.clear()
        shouldKeepSearching = true
        pageNumber = imageSource.imageBoard.firstPageIndex

        val tagsFromFavourites = RecommendationsHelper.getAllTags(
            images = seedImages.filter { it.imageSource == imageSource },
            allowAllRatings = showAllRatings,
            excludedTags = blockedTags
        )

        if (tagsFromFavourites.isEmpty()) {
            return
        }

        val topTags = RecommendationsHelper.getMostCommonTags(
            allTags = tagsFromFavourites,
            followedTagsLimit = poolSize,
            unfollowedTags = unfollowedTags
        )

        val finalSelectionSize = if (imageSource == ImageSource.DANBOORU && auth == null) SELECTION_SIZE_DANBOORU else selectionSize
        val selected = if (topTags.size <= finalSelectionSize) {
            topTags
        } else {
            topTags.shuffled().take(finalSelectionSize)
        }
        recommendedTags.addAll(selected)
    }


    suspend fun recommendImages() {
        if (isLoading || !shouldKeepSearching) {
            return
        }

        Log.i(
            "Recommendations",
            "Fetching recommended posts for tags: ${recommendedTags.joinToString(", ")} - page $pageNumber"
        )
        val filterRatingsLocally = filterRatingsLocally ||
                imageSource.imageBoard.localFilterType == ImageBoardRequirement.REQUIRED ||
                (imageSource == ImageSource.DANBOORU && auth == null)

        val searchQuery = if (filterRatingsLocally) {
            Log.i(
                "Recommendations",
                "Filtering recommendations locally because either the local filter is enabled, or the image source does not support server-side filtering."
            )
            imageSource.imageBoard.formatTagNameString(recommendedTags)
        } else {
            "${imageSource.imageBoard.formatTagNameString(recommendedTags)}+${
                ImageRating.buildSearchStringFor(
                    if (showAllRatings) {
                        ImageRating.entries.filter { it != ImageRating.UNKNOWN }
                    } else {
                        listOf(ImageRating.SAFE)
                    }
                )
            }"
        }

        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                // if recommendedTags is empty, it should just return the most recent uploaded posts
                val results = imageSource.imageBoard.loadPage(
                    tags = searchQuery,
                    page = pageNumber,
                    auth = auth,
                )
                val safeResults = results.filter {
                    if (filterRatingsLocally) {
                        showAllRatings || it.metadata!!.rating == ImageRating.SAFE
                    } else true
                }
                val wantedResults = safeResults.filter {
                    it.metadata!!.tags.none { tag -> blockedTags.contains(tag.lowercase()) }
                }
                Log.i("Recommendations", "Found ${results.size} new images for tags: ${recommendedTags.joinToString(", ")}")
                Log.i("Recommendations", "Found ${safeResults.size} safe images for tags: ${recommendedTags.joinToString(", ")}")
                Log.i("Recommendations", "Found ${wantedResults.size} wanted images for tags: ${recommendedTags.joinToString(", ")}")
                if (results.isEmpty() || safeResults.isEmpty()) {
                    shouldKeepSearching = false
                } else {
                    if (pageNumber == imageSource.imageBoard.firstPageIndex) {
                        Snapshot.withMutableSnapshot {
                            recommendedImages.clear()
                            recommendedImages.addAll(wantedResults)
                        }
                    } else if (wantedResults.isNotEmpty()) {
                        recommendedImages.addAll(wantedResults.filter { it !in recommendedImages })
                    }
                }
                pageNumber++
            } catch (e: Exception) {
                Log.e(
                    "Recommendations",
                    "Error fetching images with recommended tags: ${e.message}"
                )
                shouldKeepSearching = false
            }
            isLoading = false
            if (!doneInitialLoad) {
                doneInitialLoad = true
            }
        }
    }
}
