package moe.apex.rule34.util

import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageRating
import kotlin.text.lowercase


object RecommendationsHelper {
    private const val DEFAULT_POOL_SIZE = 7


    fun getAllTags(
        images: List<Image>,
        allowAllRatings: Boolean,
        excludedTags: Collection<String> = emptyList()
    ): List<String> {
        return images
            .filter { it.metadata != null }
            .filter { allowAllRatings || it.metadata!!.rating == ImageRating.SAFE }
            .flatMap { it.metadata!!.tags }
            .filterNot { tag -> tag in excludedTags }
    }


    /** Get the most common followed tags until `followedTagsLimit` is reached.
     *
     *  Set `includeUnwantedTagsInResult` to `true` to include unfollowed tags in the
     *  result. This may cause the returned list size to be greater than the specified
     *  limit.
     *
     *  `hiddenTags` refers to the Breadboard-provided ignored tags.
     *  The user does not have control over these and should not see them in the list.
     *
     *  `unfollowedTags` are tags the user has chosen to ignore. */
    fun getMostCommonTags(
        allTags: List<String>,
        followedTagsLimit: Int = DEFAULT_POOL_SIZE,
        hiddenTags: Set<String> = emptySet(),
        unfollowedTags: Set<String> = emptySet(),
        includeUnwantedTagsInResult: Boolean = false
    ): List<Pair<String, Int>> {
        val sortedTags = allTags
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key.lowercase() to it.value }
            .filter { it.first !in hiddenTags  }

        if (!includeUnwantedTagsInResult) {
            return sortedTags
                .filterNot { it.first in unfollowedTags }
                .take(followedTagsLimit)
        } else {
            val wantedTags = mutableListOf<Pair<String, Int>>()
            var nonExcludedCount = 0

            for (tag in sortedTags) {
                wantedTags.add(tag)

                if (tag.first !in unfollowedTags) {
                    nonExcludedCount++
                    if (nonExcludedCount == followedTagsLimit) {
                        break
                    }
                }
            }
            return wantedTags
        }
    }
}