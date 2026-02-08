package moe.apex.rule34.image

import android.util.Log
import kotlinx.serialization.Serializable
import moe.apex.rule34.RequestUtil
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.tag.TagCategory
import moe.apex.rule34.tag.TagSuggestion
import moe.apex.rule34.util.decodeHtml
import moe.apex.rule34.util.extractPixivId
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


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
    val apiKeyCreationUrl: String?
        get() = null
    val aiTagName: String
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
        val body = RequestUtil.get(autoCompleteSearchUrl.format(tags)) {
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

    suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth? = null): List<Image>

    fun formatTagString(tags: List<TagSuggestion>): String {
        return tags.joinToString("+") { it.formattedLabel }
    }

    fun formatTagNameString(tags: List<String>): String {
        return tags.joinToString("+")
    }

    fun getRatingFromString(rating: String): ImageRating

    fun buildImageSearchUrl(tags: String, page: Int, auth: ImageBoardAuth?): String {
        return if (auth != null) {
            authenticatedImageSearchUrl.format(tags, page, auth.apiKey, auth.user)
        } else {
            imageSearchUrl.format(tags, page)
        }
    }

    fun ensureSupportedFormat(fileFormat: String): Boolean {
        val supportedFileFormats = setOf("jpeg", "jpg", "png", "gif", "webp", "mp4")
        val isSupportedFormat = fileFormat.lowercase() in supportedFileFormats
        if (!isSupportedFormat) Log.w("ImageBoard", "Unknown file format: $fileFormat")
        return isSupportedFormat
    }
}


interface GelbooruBasedImageBoard : ImageBoard {
    fun parseImage(e: JSONObject, imageSource: ImageSource): Image? {
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
        val metaGroupedTags = listOf(
            TagCategory.GENERAL.group(e.getString("tags").decodeHtml().split(" ")),
        )
        val metaRating = getRatingFromString(e.getString("rating"))
        val metaPixivId = extractPixivId(metaSource)
        val metadata = ImageMetadata(
            parentId = metaParentId,
            hasChildren = null, // Not available for Gelbooru-based image boards
            source = metaSource,
            groupedTags = metaGroupedTags,
            rating = metaRating,
            pixivId = metaPixivId,
        )

        return Image(id, fileName, fileFormat, previewUrl, fileUrl, sampleUrl, imageSource, aspectRatio, metadata)
    }

    suspend fun loadImage(id: String, postListKey: String?, imageSource: ImageSource, auth: ImageBoardAuth? = null): Image? {
        val parsedId = id.toIntOrNull() ?: return null
        return loadPage("id:$parsedId", 0, postListKey, imageSource, auth).getOrNull(0)
    }

    suspend fun loadPage(tags: String, page: Int, postListKey: String?, imageSource: ImageSource, auth: ImageBoardAuth? = null): List<Image> {
        val url = buildImageSearchUrl(tags, page, auth)
        val body = RequestUtil.get(url)
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
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=%s&pid=%d"
    override val aiTagName = "ai_generated"
    override val apiKeyCreationUrl = "https://rule34.xxx/index.php?page=account&s=options"
    override val apiKeyRequirement = ImageBoardRequirement.NOT_NEEDED

    override fun parseImage(e: JSONObject): Image? {
        return parseImage(e, ImageSource.R34)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        return loadImage(id, null, ImageSource.R34, auth)
    }

    override suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth?): List<Image> {
        return loadPage(tags, page, null, ImageSource.R34, auth)
    }
}


object Safebooru : GelbooruBasedImageBoard {
    override val baseUrl = "https://safebooru.org/"
    override val autoCompleteSearchUrl = "${baseUrl}autocomplete.php?q=%s"
    override val autoCompleteCategoryMapping = emptyMap<String, String>()
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=%s&pid=%d"
    override val aiTagName = "ai-generated"

    override fun parseImage(e: JSONObject): Image? {
        return parseImage(e, ImageSource.SAFEBOORU)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        return loadImage(id, null, ImageSource.SAFEBOORU, auth)
    }

    override suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth?): List<Image> {
        return loadPage(tags, page, null, ImageSource.SAFEBOORU, auth)
    }
}


object Gelbooru : GelbooruBasedImageBoard {
    override val baseUrl = "https://gelbooru.com/"
    override val autoCompleteSearchUrl = "${baseUrl}index.php?page=autocomplete2&term=%s&type=tag_query&limit=10"
    override val autoCompleteCategoryMapping = mapOf("tag" to "general")
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=%s&pid=%d"
    override val apiKeyCreationUrl = "${baseUrl}index.php?page=account&s=options"
    override val aiTagName = "ai-generated"
    override val apiKeyRequirement = ImageBoardRequirement.REQUIRED

    override fun parseImage(e: JSONObject): Image? {
        return parseImage(e, ImageSource.GELBOORU)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        return loadImage(id, "post", ImageSource.GELBOORU, auth)
    }

    override suspend fun loadPage(tags: String, page: Int, auth: ImageBoardAuth?): List<Image> {
        return loadPage(tags, page, "post", ImageSource.GELBOORU, auth)
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
    override val aiTagName = "ai-generated"
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
    override val aiTagName = "ai-generated" // Yande.re does not allow AI-generated images but this tag appears in search
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
        val metaPixivId = extractPixivId(metaSource)
        val metadata = ImageMetadata(
            parentId = metaParentId,
            hasChildren = metaHasChildren,
            source = metaSource,
            groupedTags = metaGroupedTags,
            rating = metaRating,
            pixivId = metaPixivId,
        )

        return Image(id, fileName, fileFormat, previewUrl, fileUrl, sampleUrl, ImageSource.YANDERE, aspectRatio, metadata)
    }

    override suspend fun loadImage(id: String, auth: ImageBoardAuth?): Image? {
        val parsedId = id.toIntOrNull() ?: return null
        return loadPage("id:$parsedId", 0).getOrNull(0)
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
