package moe.apex.rule34.image

import moe.apex.rule34.RequestUtil
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.tag.TagSuggestion
import org.json.JSONArray


interface ImageBoard {
    val autoCompleteSearchUrl: String
    val imageSearchUrl: String
    val aiTagName: String

    fun loadAutoComplete(searchString: String): List<TagSuggestion>
    fun loadPage(tags: String, page: Int): List<Image>

    fun formatTagString(tags: List<TagSuggestion>): String {
        return tags.joinToString("+") { it.formattedLabel }
    }
}


class Rule34 : ImageBoard {
    override val autoCompleteSearchUrl = "https://rule34.xxx/public/autocomplete.php?q=%s"
    override val imageSearchUrl = "https://api.rule34.xxx/index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=%s&pid=%d"
    override val aiTagName = "ai_generated"


    override fun loadAutoComplete(searchString: String): List<TagSuggestion> {
        val suggestions = mutableListOf<TagSuggestion>()
        val isExcluded = searchString.startsWith("-")
        val query = searchString.replace("^-".toRegex(), "")
        val body = RequestUtil.get(autoCompleteSearchUrl.format(query)) {
            addHeader("Referer", "https://rule34.xxx/")
        }.get()
        val results = JSONArray(body)
        val resultCount = results.length()

        for (i in 0 until resultCount) {
            val suggestion = results.getJSONObject(i)
            val label = suggestion.getString("label")
            val value = suggestion.getString("value")
            val type = suggestion.getString("type")
            suggestions.add(TagSuggestion(label, value, type, isExcluded))
        }

        return suggestions.toList()
    }


    override fun loadPage(tags: String, page: Int): List<Image> {
        val body = RequestUtil.get(imageSearchUrl.format(tags, page)).get()

        if (body.isEmpty()) {
            return emptyList()
        }

        val json = JSONArray(body)
        val subjects = mutableListOf<Image>()

        for (i in 0 until json.length()) {
            val e = json.getJSONObject(i)
            if (e.isNull("image")) continue

            val (fileName, fileFormat) = e.getString("image").split('.', limit = 2)
            val fileUrl = e.getString("file_url")
            val sampleUrl = e.optString("sample_url", "")
            val previewUrl = e.getString("preview_url")

            if (fileFormat != "jpeg" && fileFormat != "jpg" && fileFormat != "png" && fileFormat != "gif") {
                println(fileFormat)
                continue
            }

            subjects.add(Image(fileName, fileFormat, previewUrl, fileUrl, sampleUrl, ImageSource.R34))
        }

        return subjects.toList()
    }
}


class Danbooru : ImageBoard {
    override val autoCompleteSearchUrl = "https://danbooru.donmai.us/autocomplete.json?search[query]=%s&search[type]=tag_query&limit=20"
    override val imageSearchUrl = "https://danbooru.donmai.us/posts.json?tags=%s&page=%d&limit=100"
    override val aiTagName = "ai-generated"


    override fun loadAutoComplete(searchString: String): List<TagSuggestion> {
        val suggestions = mutableListOf<TagSuggestion>()
        val isExcluded = searchString.startsWith("-")
        val query = searchString.replace("^-".toRegex(), "")
        val body = RequestUtil.get(autoCompleteSearchUrl.format(query)).get()
        val results = JSONArray(body)
        val resultCount = results.length()

        for (i in 0 until resultCount) {
            val suggestion = results.getJSONObject(i)
            val label = suggestion.getString("label")
            val value = suggestion.getString("value")
            val type = suggestion.getString("type")
            suggestions.add(TagSuggestion(label, value, type, isExcluded))
        }

        return suggestions.toList()
    }


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

            subjects.add(Image(fileName, fileFormat, previewUrl, fileUrl, sampleUrl, ImageSource.DANBOORU))
        }

        return subjects.toList()
    }
}