package moe.apex.rule34.image

import moe.apex.rule34.RequestUtil
import org.json.JSONArray


class ImageSource(tags: String) {
    private val baseUrl = "https://api.rule34.xxx/index.php?page=dapi&json=1&s=post&q=index&limit=100&tags=$tags"

    fun loadPage(page: Int): List<Image> {
        val body = RequestUtil.get("$baseUrl&pid=$page").get()

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

            subjects.add(Image(fileName, fileFormat, previewUrl, fileUrl, sampleUrl))
        }

        return subjects.toList()
    }
}