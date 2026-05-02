package moe.apex.breadboard.image

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import moe.apex.breadboard.RequestUtil
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.tag.TagCategory
import moe.apex.breadboard.tag.TagGroup
import moe.apex.breadboard.tag.TagSuggestion
import moe.apex.breadboard.util.PixivId
import moe.apex.breadboard.util.decodeHtml
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.collections.joinToString


val AI_TAG_NAMES = listOf(
    "ai-generated",
    "ai_generated",
    "ai-assisted",
)


@Serializable
data class ImageBoardAuth(
    val user: String,
    val apiKey: String,
)


enum class ImageBoardRequirement {
    NOT_NEEDED,
    RECOMMENDED,
    REQUIRED
}


interface ImageBoard {
    val baseUrl: String
    val autoCompleteSearchUrl: String
    val autoCompleteCategoryMapping: Map<String, String>
    val imageSearchUrl: String
    val authenticatedImageSearchUrl: String
        get() = "$imageSearchUrl&api_key=%s&user_id=%s"
    val tagSearchUrl: String?
        get() = null
    val authenticatedTagSearchUrl: String?
        get() = tagSearchUrl?.let { "$it&api_key=%s&user_id=%s" }
    val apiKeyCreationUrl: String?
        get() = null
    val firstPageIndex: Int
        get() = 0
    val apiKeyRequirement: ImageBoardRequirement
        get() = ImageBoardRequirement.NOT_NEEDED
    val localFilterType: ImageBoardRequirement
        get() = ImageBoardRequirement.NOT_NEEDED

    suspend fun loadAutoComplete(searchString: String): List<TagSuggestion> {
        val suggestions = mutableListOf<TagSuggestion>()
        val isExcluded = searchString.startsWith("-")
        val tags = searchString.replace("^-".toRegex(), "")
        val encodedTags = URLEncoder.encode(tags, "utf-8")
        val body = RequestUtil.get(autoCompleteSearchUrl.format(encodedTags)) {
            addHeader("Referrer", baseUrl)
        }
        val results = JSONArray(body)
        val resultCount = results.length()

        for (i in 0 until resultCount) {
            val suggestion = results.getJSONObject(i)
            val label = suggestion.optString("label", suggestion.optString("name")).decodeHtml()
            val value = suggestion.optString("value", suggestion.optString("name")).decodeHtml()
            val category = suggestion.optString("category", suggestion.optString("type"))
                .takeIf { it.isNotEmpty() }

            suggestions.add(
                TagSuggestion(
                    label,
                    value,
                    autoCompleteCategoryMapping[category] ?: category,
                    isExcluded,
                ),
            )
        }

        return suggestions.toList()
    }

    fun parseImage(e: JSONObject): Image?

    suspend fun loadImage(id: String, auth: ImageBoardAuth? = null): Image?

    suspend fun loadImageMd5(md5: String, auth: ImageBoardAuth? = null): Image?

    suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth? = null): List<Image>

    suspend fun loadImageGroupedTags(image: Image, auth: ImageBoardAuth? = null): ImageMetadata?

    fun formatTagString(tags: List<TagSuggestion>): String {
        return tags.joinToString(" ") { it.formattedLabel }
    }

    fun formatTagNameString(tags: List<String>): String {
        return tags.joinToString(" ")
    }

    fun getRatingFromString(rating: String): ImageRating

    fun buildImageSearchUrl(tags: String, page: Int, auth: ImageBoardAuth?): String {
        val encodedTags = URLEncoder.encode(tags, "utf-8")

        return if (auth != null) {
            authenticatedImageSearchUrl.format(encodedTags, page, auth.apiKey, auth.user)
        } else {
            imageSearchUrl.format(encodedTags, page)
        }
    }

    fun buildTagSearchUrl(tags: String, auth: ImageBoardAuth?): String? {
        val encodedTags = URLEncoder.encode(tags, "utf-8")

        return if (auth != null) {
            authenticatedTagSearchUrl?.format(encodedTags, auth.apiKey, auth.user)
        } else {
            tagSearchUrl?.format(encodedTags)
        }
    }

    fun ensureSupportedFormat(fileFormat: String): Boolean {
        val supportedFileFormats = setOf("jpeg", "jpg", "png", "gif", "webp", "mp4", "webm")
        val isSupportedFormat = fileFormat.lowercase() in supportedFileFormats
        if (!isSupportedFormat) Log.w("ImageBoard", "Unknown file format: $fileFormat")
        return isSupportedFormat
    }
}


interface GelbooruBasedImageBoard : ImageBoard {
    fun parseImage(imageSource: ImageSource, e: JSONObject): Image? {
        val id = e.getString("id")
        val (fileName, fileFormat) = e.getString("image").split('.', limit = 2)
        val fileUrl = e.getString("file_url")
        val sampleUrl = e.optString("sample_url", "")
        val previewUrl = e.getString("preview_url")
        val imageWidth = e.optInt("width", 1)
        val imageHeight = e.optInt("height", 1)
        val aspectRatio = imageWidth.toFloat() / imageHeight.toFloat()

        if (!ensureSupportedFormat(fileFormat)) return null

        val metaParentId = e.getString("parent_id").takeIf { it != "0" }
        val metaSource = e.getString("source").takeIf { it.isNotEmpty() }
        val metaArtists: List<String>
        val metaGroupedTags: List<TagGroup>

        val tagInfoArray = e.optJSONArray("tag_info")

        if (tagInfoArray != null) {
            val artistTags = mutableListOf<String>()
            val characterTags = mutableListOf<String>()
            val copyrightTags = mutableListOf<String>()
            val generalTags = mutableListOf<String>()
            val metaTags = mutableListOf<String>()

            for (i in 0 until tagInfoArray.length()) {
                val tag = tagInfoArray.getJSONObject(i)
                val tagType = tag.getString("type")
                val tagName = tag.getString("tag").decodeHtml()

                when (tagType) {
                    "artist" -> artistTags.add(tagName)
                    "character" -> characterTags.add(tagName)
                    "copyright" -> copyrightTags.add(tagName)
                    "metadata" -> metaTags.add(tagName)
                    else -> generalTags.add(tagName)
                }
            }

            metaArtists = artistTags
            metaGroupedTags = listOf(
                TagCategory.CHARACTER.group(characterTags),
                TagCategory.COPYRIGHT.group(copyrightTags),
                TagCategory.GENERAL.group(generalTags),
                TagCategory.META.group(metaTags),
            )
                .filter { it.tags.isNotEmpty() }
        } else {
            metaArtists = emptyList()
            metaGroupedTags = listOf(
                TagCategory.GENERAL.group(e.getString("tags").decodeHtml().split(" ")),
            )
        }

        val metaRating = getRatingFromString(e.getString("rating"))
        val metadata = ImageMetadata(
            parentId = metaParentId,
            hasChildren = null, // Not available for Gelbooru-based image boards
            artists = metaArtists,
            source = metaSource,
            groupedTags = metaGroupedTags,
            rating = metaRating,
        )

        return Image(id, fileName, fileFormat, previewUrl, fileUrl, sampleUrl, imageSource, aspectRatio, metadata)
    }

    suspend fun loadImage(imageSource: ImageSource, id: String, postListKey: String?, auth: ImageBoardAuth? = null): Image? {
        val parsedId = id.toIntOrNull() ?: return null
        return loadPage(imageSource, "id:$parsedId", 0, postListKey, auth).getOrNull(0)
    }

    suspend fun loadImageMd5(imageSource: ImageSource, md5: String, postListKey: String?, auth: ImageBoardAuth? = null): Image? {
        return loadPage(imageSource, "md5:$md5", 0, postListKey, auth).find { it.fileName == md5 }
    }

    suspend fun loadPage(imageSource: ImageSource, tags: String, page: Int, postListKey: String?, auth: ImageBoardAuth? = null): List<Image> {
        val url = buildImageSearchUrl(tags, page, auth)
        val body = RequestUtil.get(url) {
            addHeader("Referer", baseUrl)
        }
        if (body.isEmpty()) return emptyList()

        val posts: JSONArray

        try {
            if (postListKey != null) {
                val json = JSONObject(body)
                posts = json.optJSONArray(postListKey) ?: return emptyList()
            } else {
                posts = JSONArray(body)
            }
        } catch (e: JSONException) {
            return emptyList()
        }

        val images = mutableListOf<Image>()

        for (i in 0 until posts.length()) {
            val e = posts.getJSONObject(i)
            parseImage(e)?.let { images.add(it) }
        }

        return images
    }

    override fun getRatingFromString(rating: String): ImageRating {
        return when (rating) {
            "general" -> ImageRating.SAFE
            "safe" -> ImageRating.SAFE
            "sensitive" -> ImageRating.SENSITIVE
            "questionable" -> ImageRating.QUESTIONABLE
            "explicit" -> ImageRating.EXPLICIT
            else -> ImageRating.UNKNOWN
        }
    }
}


object Rule34 : GelbooruBasedImageBoard {
    override val baseUrl = "https://api.rule34.xxx/"
    override val autoCompleteSearchUrl = "${baseUrl}/autocomplete.php?q=%s"
    override val autoCompleteCategoryMapping = emptyMap<String, String>()
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&fields=tag_info&tags=%s&pid=%d"
    override val apiKeyCreationUrl = "https://rule34.xxx/index.php?page=account&s=options"
    override val apiKeyRequirement = ImageBoardRequirement.NOT_NEEDED

    override fun parseImage(e: JSONObject): Image? {
        return parseImage(ImageSource.R34, e)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        return loadImage(ImageSource.R34, id, null, auth)
    }

    override suspend fun loadImageMd5(md5: String, auth: ImageBoardAuth?): Image? {
        return loadImageMd5(ImageSource.R34, md5, null, auth)
    }

    override suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth?): List<Image> {
        return loadPage(ImageSource.R34, tags, page, null, auth)
    }

    override suspend fun loadImageGroupedTags(image: Image, auth: ImageBoardAuth?): ImageMetadata? {
        return image.id?.let { loadImage(it, auth)?.metadata }
    }
}


object Safebooru : GelbooruBasedImageBoard {
    override val baseUrl = "https://safebooru.org/"
    override val autoCompleteSearchUrl = "${baseUrl}autocomplete.php?q=%s"
    override val autoCompleteCategoryMapping = emptyMap<String, String>()
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&fields=tag_info&tags=%s&pid=%d"

    override fun parseImage(e: JSONObject): Image? {
        return parseImage(ImageSource.SAFEBOORU, e)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        return loadImage(ImageSource.SAFEBOORU, id, null, auth)
    }

    override suspend fun loadImageMd5(md5: String, auth: ImageBoardAuth?): Image? {
        return loadImageMd5(ImageSource.SAFEBOORU, md5, null, auth)
    }

    override suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth?): List<Image> {
        return loadPage(ImageSource.SAFEBOORU, tags, page, null, auth)
    }

    override suspend fun loadImageGroupedTags(image: Image, auth: ImageBoardAuth?): ImageMetadata? {
        return image.id?.let { loadImage(it, auth)?.metadata }
    }
}


object Gelbooru : GelbooruBasedImageBoard {
    override val baseUrl = "https://gelbooru.com/"
    override val autoCompleteSearchUrl = "${baseUrl}index.php?page=autocomplete2&term=%s&type=tag_query&limit=10"
    override val autoCompleteCategoryMapping = mapOf("tag" to "general")
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=%s&pid=%d"
    override val tagSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=tag&q=index&names=%s"
    override val apiKeyCreationUrl = "${baseUrl}index.php?page=account&s=options"
    override val apiKeyRequirement = ImageBoardRequirement.REQUIRED

    override fun parseImage(e: JSONObject): Image? {
        return parseImage(ImageSource.GELBOORU, e)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        return loadImage(ImageSource.GELBOORU, id, "post", auth)
    }

    override suspend fun loadImageMd5(md5: String, auth: ImageBoardAuth?): Image? {
        return loadImageMd5(ImageSource.GELBOORU, md5, "post", auth)
    }

    override suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth?): List<Image> {
        return loadPage(ImageSource.GELBOORU, tags, page, "post", auth)
    }

    override suspend fun loadImageGroupedTags(image: Image, auth: ImageBoardAuth?): ImageMetadata? {
        val artistTags = mutableListOf<String>()
        val characterTags = mutableListOf<String>()
        val copyrightTags = mutableListOf<String>()
        val generalTags = mutableListOf<String>()
        val metaTags = mutableListOf<String>()

        val chunkedTags = image.metadata?.tags?.chunked(100) ?: emptyList()

        for (i in 0 until chunkedTags.size) {
            val url = buildTagSearchUrl(chunkedTags[i].joinToString(" "), auth) ?: return null
            val body = RequestUtil.get(url) {
                addHeader("Referer", baseUrl)
            }
            if (body.isEmpty()) return null

            val json: JSONObject

            try {
                json = JSONObject(body)
            } catch (e: JSONException) {
                return null
            }

            val tagInfoArray = json.optJSONArray("tag") ?: return null

            for (i in 0 until tagInfoArray.length()) {
                val tag = tagInfoArray.getJSONObject(i)
                val tagType = tag.getString("type")
                val tagName = tag.getString("name").decodeHtml()

                // Gelbooru's tag types when represented as ints seem to follow Danbooru's autocomplete category mappings
                when (Danbooru.autoCompleteCategoryMapping[tagType]) {
                    "artist" -> artistTags.add(tagName)
                    "copyright" -> copyrightTags.add(tagName)
                    "character" -> characterTags.add(tagName)
                    "meta" -> metaTags.add(tagName)
                    else -> generalTags.add(tagName)
                }
            }

            if (i != chunkedTags.size - 1) delay(200)
        }

        val groupedTags = listOf(
            TagCategory.CHARACTER.group(characterTags),
            TagCategory.COPYRIGHT.group(copyrightTags),
            TagCategory.GENERAL.group(generalTags),
            TagCategory.META.group(metaTags),
        )
            .filter { it.tags.isNotEmpty() }

        return image.metadata?.copy(artists = artistTags, groupedTags = groupedTags)
    }
}


object Danbooru : ImageBoard {
    override val baseUrl = "https://danbooru.donmai.us/"
    override val autoCompleteSearchUrl = "${baseUrl}autocomplete.json?search[query]=%s&search[type]=tag_query&limit=10"
    override val autoCompleteCategoryMapping = mapOf(
        "0" to "general",
        "1" to "artist",
        "3" to "copyright",
        "4" to "character",
        "5" to "meta",
    )
    override val imageSearchUrl = "${baseUrl}posts.json?tags=%s&page=%d&limit=100"
    override val firstPageIndex = 1
    override val authenticatedImageSearchUrl = "$imageSearchUrl&api_key=%s&login=%s"
    override val apiKeyCreationUrl = "${baseUrl}/profile"
    override val apiKeyRequirement = ImageBoardRequirement.RECOMMENDED
    override val localFilterType = ImageBoardRequirement.RECOMMENDED

    override fun parseImage(e: JSONObject): Image? {
        if (e.isNull("md5")) return null

        val id = e.getString("id")
        val fileName = e.getString("md5")
        val fileFormat = e.getString("file_ext")
        val fileUrl = e.getString("file_url")
        val sampleUrl = e.optString("large_file_url", "")
        val previewUrl = e.getString("preview_file_url")
        val imageWidth = e.optInt("image_width", 1)
        val imageHeight = e.optInt("image_height", 1)
        val aspectRatio = imageWidth.toFloat() / imageHeight.toFloat()

        if (!ensureSupportedFormat(fileFormat)) return null

        val tagStringArtist = e.getString("tag_string_artist").decodeHtml()
        val tagCharacter = e.getString("tag_string_character").decodeHtml().split(" ")
        val tagCopyright = e.getString("tag_string_copyright").decodeHtml().split(" ")
        val tagGeneral = e.getString("tag_string_general").decodeHtml().split(" ")
        val tagMeta = e.getString("tag_string_meta").decodeHtml().split(" ")

        val metaParentId = e.getString("parent_id").takeIf { it != "null" }
        val metaHasChildren = e.getBoolean("has_children")
        val metaSource = e.getString("source").takeIf { it.isNotEmpty() }
        val metaArtists = tagStringArtist.takeIf { it.isNotBlank() }?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        val metaGroupedTags = listOf(
            TagCategory.CHARACTER.group(tagCharacter),
            TagCategory.COPYRIGHT.group(tagCopyright),
            TagCategory.GENERAL.group(tagGeneral),
            TagCategory.META.group(tagMeta),
        )
            .filter { it.tags.isNotEmpty() }
        val metaRating = getRatingFromString(e.getString("rating"))
        val metaPixivId = e.optInt("pixiv_id").takeIf { it != 0 }
        val metadata = ImageMetadata(
            parentId = metaParentId,
            hasChildren = metaHasChildren,
            artists = metaArtists,
            source = metaSource,
            groupedTags = metaGroupedTags,
            rating = metaRating,
            pixivId = metaPixivId,
        )

        return Image(id, fileName, fileFormat, previewUrl, fileUrl, sampleUrl, ImageSource.DANBOORU, aspectRatio, metadata)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        val parsedId = id.toIntOrNull() ?: return null
        return loadPage("id:$parsedId", 0, auth).getOrNull(0)
    }

    override suspend fun loadImageMd5(md5: String, auth: ImageBoardAuth?): Image? {
        return loadPage("md5:$md5", 0, auth).find { it.fileName == md5 }
    }

    override suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth?): List<Image> {
        val url = buildImageSearchUrl(tags, page, auth)
        val body = RequestUtil.get(url)
        if (body.isEmpty()) return emptyList()

        val json = JSONArray(body)
        val subjects = mutableListOf<Image>()

        for (i in 0 until json.length()) {
            val e = json.getJSONObject(i)
            parseImage(e)?.let { subjects.add(it) }
        }

        return subjects.toList()
    }

    override suspend fun loadImageGroupedTags(image: Image, auth: ImageBoardAuth?): ImageMetadata? {
        return image.id?.let { loadImage(it, auth)?.metadata }
    }

    override fun getRatingFromString(rating: String): ImageRating {
        return when (rating) {
            "g" -> ImageRating.SAFE
            "s" -> ImageRating.SENSITIVE
            "q" -> ImageRating.QUESTIONABLE
            "e" -> ImageRating.EXPLICIT
            else -> ImageRating.UNKNOWN
        }
    }
}


object Yandere : ImageBoard {
    override val baseUrl = "https://yande.re/"
    override val autoCompleteSearchUrl = "${baseUrl}tag.json?limit=10&order=count&name=%s"
    override val autoCompleteCategoryMapping = mapOf(
        "0" to "general",
        "1" to "artist",
        "3" to "copyright",
        "4" to "character",
        "5" to "circle",
        "6" to "faults",
    )
    override val imageSearchUrl = "${baseUrl}post.json?tags=%s&page=%d&limit=100"
    override val firstPageIndex = 1
    override val localFilterType = ImageBoardRequirement.REQUIRED

    override fun parseImage(e: JSONObject): Image? {
        if (e.isNull("md5")) return null

        val id = e.getString("id")
        val fileName = e.getString("md5")
        val fileFormat = e.getString("file_ext")
        val fileUrl = e.getString("file_url")
        val sampleUrl = e.getString("sample_url")
        val previewUrl = e.getString("preview_url")
        val imageWidth = e.optInt("width", 1)
        val imageHeight = e.optInt("height", 1)
        val aspectRatio = imageWidth.toFloat() / imageHeight.toFloat()

        if (!ensureSupportedFormat(fileFormat)) return null

        val metaParentId = e.getString("parent_id").takeIf { it != "null" }
        val metaHasChildren = e.getBoolean("has_children")
        val metaSource = e.optString("source", "").takeIf { it.isNotEmpty() }
        val metaGroupedTags = listOf(
            TagCategory.GENERAL.group(e.getString("tags").decodeHtml().split(" ")),
        )
        val metaRating = getRatingFromString(e.getString("rating"))
        val metadata = ImageMetadata(
            parentId = metaParentId,
            hasChildren = metaHasChildren,
            source = metaSource,
            groupedTags = metaGroupedTags,
            rating = metaRating,
        )

        return Image(id, fileName, fileFormat, previewUrl, fileUrl, sampleUrl, ImageSource.YANDERE, aspectRatio, metadata)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        val parsedId = id.toIntOrNull() ?: return null
        return loadPage("id:$parsedId", 0).getOrNull(0)
    }

    override suspend fun loadImageMd5(md5: String, auth: ImageBoardAuth?): Image? {
        return loadPage("md5:$md5", 0).find { it.fileName == md5 }
    }

    override suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth?): List<Image> {
        val url = buildImageSearchUrl(tags, page, auth)
        val body = RequestUtil.get(url)
        if (body.isEmpty()) return emptyList()

        val json = JSONArray(body)
        val subjects = mutableListOf<Image>()

        for (i in 0 until json.length()) {
            val e = json.getJSONObject(i)
            parseImage(e)?.let { subjects.add(it) }
        }

        return subjects.toList()
    }

    override suspend fun loadImageGroupedTags(image: Image, auth: ImageBoardAuth?): ImageMetadata? {
        return null // TODO
    }

    override fun getRatingFromString(rating: String): ImageRating {
        return when (rating) {
            "s" -> ImageRating.SAFE
            "q" -> ImageRating.QUESTIONABLE
            "e" -> ImageRating.EXPLICIT
            else -> ImageRating.UNKNOWN
            // Sensitive does not exist for Yande.re
        }
    }
}
