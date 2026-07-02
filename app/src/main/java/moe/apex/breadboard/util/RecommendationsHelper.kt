package moe.apex.breadboard.util

import moe.apex.breadboard.image.Image
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.tag.TagCategory
import kotlin.random.Random.Default.nextInt
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

    /**
     * Recommends a set of tags based on the user's favorites, ensuring that the selected
     * tags co-occur in the user's favorites to provide relevant results.
     */
    fun getRecommendedTags(
        images: List<Image>,
        selectionSize: Int,
        poolSize: Int,
        hiddenTags: Set<String> = emptySet(),
        unfollowedTags: Set<String> = emptySet(),
    ): List<String> {
        if (images.isEmpty()) return emptyList()

        /* Get all tags and their frequencies.
           Ideally we'd just be able to use groupedTags, but older favourites don't have these,
           and some sources don't have support for grouped tags at all (yande.re) in Breadboard. */
        val allTags = images.flatMap { it.metadata?.tags ?: emptyList() }
            .map { it.lowercase() }
            .filter { it !in hiddenTags && it !in unfollowedTags }

        if (allTags.isEmpty()) return emptyList()

        val tagFrequencies = allTags.groupingBy { it }.eachCount()

        /* Get the most specific category for each tag.
           Older favourites might not have categorised tags (therefore default to GENERAL),
           so if the same tag exists in more than one image and the categories are different,
           prefer the more specific one. */
        val tagCategories = mutableMapOf<String, TagCategory>()
        images.forEach { image ->
            image.metadata?.groupedTags?.forEach { group ->
                group.tags.forEach { tag ->
                    val lowerTag = tag.lowercase()
                    val currentCategory = tagCategories[lowerTag]
                    // If we find a non-GENERAL category, use that instead.
                    if (currentCategory == null || currentCategory == TagCategory.GENERAL) {
                        tagCategories[lowerTag] = group.category
                    }
                }
            }
        }

        // These are the tags that could actually be chosen
        val pool = tagFrequencies.entries
            .sortedByDescending { it.value }
            .take(poolSize)
            .map { it.key }

        if (pool.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val remainingPool = pool.toMutableList()

        /* Of the tags in the pool, choose one to use as the primary.
           The other chosen tags will be based on this one. */
        val primaryTag = pickWeightedTag(remainingPool, tagFrequencies, tagCategories) ?: return emptyList()
        result.add(primaryTag)
        remainingPool.remove(primaryTag)

        // Pick other tags that have appeared together with the primary one.
        while (result.size < selectionSize && remainingPool.isNotEmpty()) {
            val coExistenceCounts = mutableMapOf<String, Int>()

            // Count how many times each tag in the remaining pool co-exists with the other chosen tags.
            remainingPool.forEach { candidate ->
                var count = 0
                images.forEach { image ->
                    val imageTags = image.metadata?.tags?.map { it.lowercase() } ?: emptyList()
                    if (imageTags.contains(candidate) && imageTags.containsAll(result)) {
                        count++
                    }
                }
                if (count > 0) {
                    coExistenceCounts[candidate] = count
                }
            }

            if (coExistenceCounts.isEmpty()) break

            val nextTag = pickWeightedTag(coExistenceCounts.keys.toList(), coExistenceCounts, tagCategories)
            if (nextTag != null) {
                result.add(nextTag)
                remainingPool.remove(nextTag)
            } else {
                break
            }
        }

        return result
    }


    /**
     * Recommends a set of artists based on the user's favorites, ranked by frequency.
     * Artists already in [followedTags] are excluded.
     */
    fun getRecommendedArtists(
        images: List<Image>,
        followedTags: Set<String>,
        limit: Int = 10
    ): List<String> {
        return images.flatMap { it.metadata?.artists ?: emptyList() }
            .map { it.lowercase() }
            .filter { it.isNotEmpty() && it !in followedTags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }


    private fun pickWeightedTag(
        candidates: List<String>,
        frequencies: Map<String, Int>,
        categories: Map<String, TagCategory>
    ): String? {
        if (candidates.isEmpty()) {
            return null
        }

        val weights = candidates.map { tag ->
            val freq = frequencies[tag] ?: 1
            val category = categories[tag] ?: TagCategory.GENERAL
            val categoryWeight = when (category) {
                // TODO: Consider making these configurable in future?
                TagCategory.ARTIST -> 1.5
                TagCategory.CHARACTER -> 2.0
                TagCategory.COPYRIGHT -> 3.5
                TagCategory.META -> 0.0 // We'll just ignore meta tags entirely for now.
                TagCategory.GENERAL -> 1.0
            }
            (freq * categoryWeight * 10).toInt()
        }

        val totalWeight = weights.sum()
        if (totalWeight == 0) { // This seems exceptionally unlikely but not impossible.
            return candidates.random()
        }

        var random = nextInt(totalWeight)
        for (i in candidates.indices) {
            random -= weights[i]
            if (random < 0) {
                return candidates[i]
            }
        }

        return candidates.last()
    }
}