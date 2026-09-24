package moe.apex.breadboard.image

import moe.apex.breadboard.tag.TagCategory
import moe.apex.breadboard.tag.TagGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageMetadataTest {

    @Test
    fun testMetadataTagsIncludesArtists() {
        val metadata = ImageMetadata(
            rating = ImageRating.SAFE,
            artists = listOf("artist_1", "artist_2"),
            groupedTags = listOf(
                TagGroup(TagCategory.GENERAL, listOf("tag_1", "tag_2")),
                TagGroup(TagCategory.CHARACTER, listOf("character_1"))
            )
        )

        val tags = metadata.tags
        assertEquals(listOf("artist_1", "artist_2", "tag_1", "tag_2", "character_1"), tags)
    }

    @Test
    fun testMetadataTagsDeduplication() {
        val metadata = ImageMetadata(
            rating = ImageRating.SAFE,
            artists = listOf("artist_1"),
            groupedTags = listOf(
                TagGroup(TagCategory.GENERAL, listOf("artist_1", "tag_1"))
            )
        )

        val tags = metadata.tags
        assertEquals(listOf("artist_1", "tag_1"), tags)
    }

    @Test
    fun testHasGroupedTagsWithArtists() {
        val imageWithArtist = Image(
            fileName = "test",
            fileFormat = "jpg",
            previewUrl = "",
            fileUrl = "",
            sampleUrl = "",
            metadata = ImageMetadata(
                rating = ImageRating.SAFE,
                artists = listOf("artist_1"),
                groupedTags = listOf(TagGroup(TagCategory.GENERAL, listOf("tag_1")))
            )
        )

        assertTrue(imageWithArtist.hasGroupedTags)
    }
}
