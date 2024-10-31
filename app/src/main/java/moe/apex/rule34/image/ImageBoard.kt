package moe.apex.rule34.image

import moe.apex.rule34.RequestUtil
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.tag.TagSuggestion
import moe.apex.rule34.util.extractPixivId
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


interface ImageBoard {
    val imageSource: ImageSource
    val baseUrl: String
    val autoCompleteSearchUrl: String
    val imageSearchUrl: String
    val aiTagName: String
    val firstPageIndex: Int
        get() = 0

    fun loadAutoComplete(searchString: String): List<TagSuggestion> {
        val suggestions = mutableListOf<TagSuggestion>()
        val isExcluded = searchString.startsWith("-")
        val query = searchString.replace("^-".toRegex(), "")
        val body = RequestUtil.get(autoCompleteSearchUrl.format(query)) {
            addHeader("Referrer", baseUrl)
        }.get()
        val results = JSONArray(body)
        val resultCount = results.length()

        for (i in 0 until resultCount) {
            val suggestion = results.getJSONObject(i)
            val label = suggestion.getString("label")
            val value = suggestion.getString("value")
            val type = suggestion.optString("type", suggestion.optString("category")).takeIf { it.isNotEmpty() }
            suggestions.add(TagSuggestion(label, value, type, isExcluded))
        }

        return suggestions.toList()
    }

    fun loadPage(tags: String, page: Int): List<Image>

    fun formatTagString(tags: List<TagSuggestion>): String {
        return tags.joinToString("+") { it.formattedLabel }
    }
}


interface GelbooruBasedImageBoard : ImageBoard {
    fun loadPage(tags: String, page: Int, source: ImageSource, postListKey: String? = null): List<Image> {
        val body = RequestUtil.get(imageSearchUrl.format(tags, page)).get()

        if (body.isEmpty()) {
            return emptyList()
        }

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

            val (fileName, fileFormat) = e.getString("image").split('.', limit = 2)
            val fileUrl = e.getString("file_url")
            val sampleUrl = e.optString("sample_url", "")
            val previewUrl = e.getString("preview_url")

            if (fileFormat != "jpeg" && fileFormat != "jpg" && fileFormat != "png" && fileFormat != "gif") {
                continue
            }

            val metaSource = e.optString("source", "").takeIf { it.isNotEmpty() }
            val metaTags = e.getString("tags").split(" ")
            val metaRating = ImageRating.fromString(e.getString("rating"))
            val metaPixivId = extractPixivId(metaSource)
            val metadata = ImageMetadata(null, metaSource, metaTags, metaRating, metaPixivId)

            images.add(Image(fileName, fileFormat, previewUrl, fileUrl, sampleUrl, source, metadata))
        }

        return images
    }
}


class Rule34 : GelbooruBasedImageBoard {
    override val imageSource = ImageSource.R34
    override val baseUrl = "https://rule34.xxx/"
    override val autoCompleteSearchUrl = "${baseUrl}public/autocomplete.php?q=%s"
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=%s&pid=%d"
    override val aiTagName = "ai_generated"

    override fun loadPage(tags: String, page: Int): List<Image> {
        return loadPage(tags, page, ImageSource.R34)
    }
}


class Safebooru : GelbooruBasedImageBoard {
    override val imageSource = ImageSource.SAFEBOORU
    override val baseUrl = "https://safebooru.org/"
    override val autoCompleteSearchUrl = "${baseUrl}autocomplete.php?q=%s"
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=%s&pid=%d"
    override val aiTagName = "ai-generated"

    override fun loadPage(tags: String, page: Int): List<Image> {
        return loadPage(tags, page, ImageSource.SAFEBOORU)
    }
}


class Gelbooru : GelbooruBasedImageBoard {
    override val imageSource = ImageSource.GELBOORU
    override val baseUrl = "https://gelbooru.com/"
    override val autoCompleteSearchUrl = "${baseUrl}index.php?page=autocomplete2&term=%s&type=tag_query&limit=10"
    override val imageSearchUrl = "${baseUrl}index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=%s&pid=%d"
    override val aiTagName = "ai-generated"

    override fun loadPage(tags: String, page: Int): List<Image> {
        return loadPage(tags, page, ImageSource.GELBOORU, "post")
    }
}


class Danbooru : ImageBoard {
    override val imageSource = ImageSource.DANBOORU
    override val baseUrl = "https://danbooru.donmai.us/"
    override val autoCompleteSearchUrl = "${baseUrl}autocomplete.json?search[query]=%s&search[type]=tag_query&limit=20"
    override val imageSearchUrl = "${baseUrl}posts.json?tags=%s&page=%d&limit=100"
    override val aiTagName = "ai-generated"
    override val firstPageIndex = 1

    override fun loadPage(tags: String, page: Int): List<Image> {
        val body = RequestUtil.get(imageSearchUrl.format(tags, page)).get()

        if (body.isEmpty()) {
            return emptyList()
        }

        val json = JSONArray(body)
        val subjects = mutableListOf<Image>()

        for (i in 0 until json.length()) {
            val e = json.getJSONObject(i)
            if (e.isNull("md5")) continue

            val fileName = e.getString("md5")
            val fileFormat = e.getString("file_ext")
            val fileUrl = e.getString("file_url")
            val sampleUrl = e.optString("large_file_url", "")
            val previewUrl = e.getString("preview_file_url")

            if (fileFormat != "jpeg" && fileFormat != "jpg" && fileFormat != "png" && fileFormat != "gif") {
                continue
            }

            val metaSource = e.optString("source", "").takeIf { it.isNotEmpty() }
            val metaArtist = e.optString("tag_string_artist", "").takeIf { it.isNotEmpty() }
            var metaTags = e.getString("tag_string").split(" ")
            if (metaArtist != null)
                metaTags = metaTags
                    .toMutableList()
                    .minus(metaArtist)
                    .toList()
            val metaRating = ImageRating.fromString(e.optString("rating"))
            val metaPixivId = e.optInt("pixiv_id").takeIf { it != 0 }
            val metadata = ImageMetadata(metaArtist, metaSource, metaTags, metaRating, metaPixivId)

            subjects.add(Image(fileName, fileFormat, previewUrl, fileUrl, sampleUrl, ImageSource.DANBOORU, metadata))
        }

        return subjects.toList()
    }
}
