package moe.apex.breadboard.util

import moe.apex.breadboard.image.Image
import moe.apex.breadboard.image.ImageBoardAuth


/**
 * Refresh the metadata for an image, particularly useful for updating tag categories.
 *
 *  This will make a request to the image board for the metadata and will merge it with the provided
 *  [image] and pass the final [Image] object into [onImageUpdate].
 *
 *  If the new metadata is `null` or is identical to the metadata already on [image],
 *  [onImageUpdate] will **not** be called.
 *
 *  @return A [Boolean] representing whether or not the image was updated.
 */
suspend fun refreshImageMetadata(
    image: Image,
    auth: ImageBoardAuth?,
    onImageUpdate: (Image) -> Unit
): Boolean {
    val metadata = image.imageSource.imageBoard.loadImageGroupedTags(image, auth)

    return if (metadata != null && metadata != image.metadata) {
        val newImage = image.copy(metadata = metadata)
        onImageUpdate(newImage)
        true
    } else false
}
