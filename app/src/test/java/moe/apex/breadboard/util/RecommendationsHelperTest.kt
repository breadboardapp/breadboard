package moe.apex.breadboard.util

import moe.apex.breadboard.image.Image
import moe.apex.breadboard.image.ImageMetadata
import moe.apex.breadboard.image.ImageRating
import moe.apex.breadboard.tag.TagCategory
import moe.apex.breadboard.tag.TagGroup
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationsHelperTest {
    private fun createImage(tags: List<String>, category: TagCategory = TagCategory.GENERAL): Image {
        return Image(
            fileName = "test",
            fileFormat = "jpg",
            previewUrl = "",
            fileUrl = "",
            sampleUrl = "",
            metadata = ImageMetadata(
                rating = ImageRating.SAFE,
                groupedTags = listOf(TagGroup(category, tags))
            )
        )
    }


    @Test
    fun testCoOccurrenceSelection() {
        // Two clusters of unrelated tags
        // Cluster 1: blue_archive, hoshino_(blue_archive), halo
        // Cluster 2: arknights, silverash_(arknights), sword
        val images = listOf(
            createImage(listOf("blue_archive", "hoshino_(blue_archive)", "halo")),
            createImage(listOf("blue_archive", "hoshino_(blue_archive)", "halo")),
            createImage(listOf("blue_archive", "hoshino_(blue_archive)", "halo")),
            createImage(listOf("arknights", "silverash_(arknights)", "sword")),
            createImage(listOf("arknights", "silverash_(arknights)", "sword")),
            createImage(listOf("arknights", "silverash_(arknights)", "sword"))
        )

        val recommended = RecommendationsHelper.getRecommendedTags(
            images = images,
            selectionSize = 2,
            poolSize = 10
        )

        // Resultant tags should all be either BA-related or Arknights-related, but not mixed.
        val cluster1 = setOf("blue_archive", "hoshino_(blue_archive)", "halo")
        val cluster2 = setOf("arknights", "silverash_(arknights)", "sword")

        assertTrue("Recommended tags: $recommended",
            recommended.all { it in cluster1 } || recommended.all { it in cluster2 }
        )
    }


    @Test
    fun testEmptyImages() {
        val recommended = RecommendationsHelper.getRecommendedTags(
            images = emptyList(),
            selectionSize = 2,
            poolSize = 10
        )
        assertTrue(recommended.isEmpty())
    }
}
