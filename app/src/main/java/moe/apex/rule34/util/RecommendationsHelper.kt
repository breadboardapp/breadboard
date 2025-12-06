package moe.apex.rule34.util

import moe.apex.rule34.image.Image
import moe.apex.rule34.image.ImageRating
import kotlin.text.lowercase


object RecommendationsHelper {
    private const val DEFAULT_POOL_SIZE = 5
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


    fun getAllTags(
        images: List<Image>,
        allowAllRatings: Boolean,
        excludedTags: Collection<String> = emptyList()
    ): List<String> {
        return images
            .filter { it.metadata != null }
            .filter { allowAllRatings || it.metadata!!.rating == ImageRating.SAFE }
            .flatMap { it.metadata!!.tags }
            .filterNot { tag -> ignoredTags.contains(tag.lowercase()) }
            .filterNot { tag -> excludedTags.contains(tag.lowercase()) }
    }


    fun getMostCommonTags(
        allTags: List<String>,
        limit: Int = DEFAULT_POOL_SIZE,
        excludedTags: Collection<String> = emptyList(),
        limitIncludesExcluded: Boolean = true
    ): List<String> {
        val sortedTags = allTags
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key.lowercase() }

        return if (limitIncludesExcluded) {
            sortedTags
                .take(limit)
                .filterNot { excludedTags.contains(it) }
        } else {
            sortedTags
                .filterNot { excludedTags.contains(it) }
                .take(limit)
        }
    }
}