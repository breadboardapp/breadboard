package moe.apex.rule34.util

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageBoardAuth
import moe.apex.rule34.image.ImageBoardLocalFilterType
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.ImageSource


class RecommendationsProvider(
    private val seedImages: List<Image>,
    val imageSource: ImageSource,
    val auth: ImageBoardAuth?,
    val filterRatingsLocally: Boolean,
    val blockedTags: Set<String>
) {
    companion object {
        private const val POOL_SIZE = 5
        private const val SELECTION_SIZE = 3
        private const val SELECTION_SIZE_DANBOORU = 2
        private val ignoredTags = setOf( // Very general or meta tags, not useful for recommendations
            "1girl",
            "1boy",
            "absurdres",
            "artist_request",
            "bad_id",
            "bad_pixiv_id",
            "commentary",
            "commentary_request",
            "english_commentary",
            "highres",
            "image_macro",
            "lowres",
            "non-web_source",
            "official_art",
            "original",
            "promotional",
            "sample",
            "solo",
            "tagme",
            "translation_request",
            "ultra_highres",
            "wallpaper"
        )
    }

    val recommendedImages = mutableStateListOf<Image>()
    private val recommendedTags = mutableListOf<String>()
    private var pageNumber by mutableIntStateOf(imageSource.imageBoard.firstPageIndex)

    private var isLoading by mutableStateOf(false)
    private var shouldKeepSearching by mutableStateOf(true)


    fun prepareRecommendedTags() {
        recommendedTags.clear()
        shouldKeepSearching = true
        pageNumber = imageSource.imageBoard.firstPageIndex

        val tagsFromFavourites = seedImages
            .filter { it.imageSource == imageSource && it.metadata != null && it.metadata.rating == ImageRating.SAFE }
            .flatMap { it.metadata!!.allTags }
            .filterNot { tag -> ignoredTags.contains(tag.lowercase()) }
            .filterNot { tag -> blockedTags.contains(tag.lowercase())}

        if (tagsFromFavourites.isEmpty()) {
            return
        }

        val tagCounts = tagsFromFavourites.groupingBy { it }.eachCount()
        val topTags = tagCounts.entries
            .sortedByDescending { it.value }
            .take(POOL_SIZE)
            .map { it.key }

        val selectionSize = if (imageSource == ImageSource.DANBOORU && auth == null) SELECTION_SIZE_DANBOORU else SELECTION_SIZE
        val selected = if (topTags.size <= selectionSize) {
            topTags
        } else {
            topTags.shuffled().take(selectionSize)
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
        withContext(Dispatchers.IO) {
            val filterRatingsLocally = this@RecommendationsProvider.filterRatingsLocally ||
                    imageSource.imageBoard.localFilterType == ImageBoardLocalFilterType.REQUIRED ||
                    (imageSource == ImageSource.DANBOORU && auth == null)
            try {
                val searchQuery = if (filterRatingsLocally) {
                    Log.i(
                        "Recommendations",
                        "Filtering recommendations locally because either the local filter is enabled, or the image source does not support server-side filtering."
                    )
                    imageSource.imageBoard.formatTagNameString(recommendedTags)
                } else {
                    "${imageSource.imageBoard.formatTagNameString(recommendedTags)}+${
                        ImageRating.buildSearchStringFor(ImageRating.SAFE)
                    }"
                }

                isLoading = true
                // if recommendedTags is empty, it should just return the most recent uploaded posts
                val results = imageSource.imageBoard.loadPage(
                    tags = searchQuery,
                    page = pageNumber,
                    auth = auth,
                )
                val safeResults = results.filter {
                    it !in recommendedImages &&
                    if (filterRatingsLocally) it.metadata!!.rating == ImageRating.SAFE else true
                }
                val wantedResults = safeResults.filter {
                    it.metadata!!.allTags.none { tag -> blockedTags.contains(tag.lowercase()) }
                }
                if (pageNumber == imageSource.imageBoard.firstPageIndex) {
                    recommendedImages.clear()
                }
                if (results.isEmpty() || safeResults.isEmpty()) {
                    shouldKeepSearching = false
                } else {
                    recommendedImages.addAll(wantedResults)
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
        }
    }
}

