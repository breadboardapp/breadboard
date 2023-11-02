package me.devoxin.rule34

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URLEncoder

class ImageSource private constructor(vararg tags: String) {
    private val url = BASE_URL + tags.joinToString("+") { URLEncoder.encode(it, Charsets.UTF_8.name()) }
    private var page = 0

    private var images = mutableListOf<Image>()

    private suspend fun nextPage(): Int {
        val body = withContext(Dispatchers.IO) { RequestUtil.get("$url&pid=$page").get() }.takeIf { it.isNotEmpty() }
            ?: return 0

        val json = JSONArray(body)
        var added = 0

        for (i in 0 until json.length()) {
            val e = json.getJSONObject(i)
            if (e.isNull("image")) continue

            val (fileName, fileFormat) = e.getString("image").split('.', limit = 2)
            val fileUrl = e.getString("file_url")
            val sampleUrl = e.optString("sample_url", "")
            val previewUrl = e.getString("preview_url")

            if (fileFormat !in SUPPORTED_FORMATS) {
                Log.d("ImageSource", "Unsupported file format $fileFormat")
                continue
            }

            images.add(Image(fileName, fileFormat, previewUrl, fileUrl, sampleUrl))
            added++
        }

        page++
        return added
    }

    companion object {
        private const val BASE_URL = "https://api.rule34.xxx/index.php?page=dapi&json=1&s=post&q=index&limit=100&tags="
        private val SUPPORTED_FORMATS = setOf("jpeg", "jpg", "png", "gif")

        private var instance: ImageSource? = null
        val images: List<Image>
            get() = instance?.images ?: emptyList()

        val itemCount: Int
            get() = instance?.images?.size ?: 0

        fun withTags(vararg tags: String) {
            instance = ImageSource(*tags)
        }

        suspend fun nextPage() = instance?.nextPage() ?: 0
    }
}
