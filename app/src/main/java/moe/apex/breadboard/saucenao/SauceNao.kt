package moe.apex.breadboard.saucenao

import moe.apex.breadboard.RequestUtil
import moe.apex.breadboard.social.SocialSite
import moe.apex.breadboard.navigation.ImageView
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.util.PixivArtwork
import moe.apex.breadboard.util.isWebLink
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.let


data class SauceNaoResponse(
    val header: SauceNaoResponseHeader,
    val results: List<SauceNaoResult>
)


data class SauceNaoResponseHeader(
    val status: Int,
    val message: String?,
    val resultsCount: Int
)


data class SauceNaoResult(
    val header: SauceNaoResultHeader,
    val data: SauceNaoResultData
)


data class SauceNaoResultHeader(
    val similarity: Float,
    val thumbnail: String,
    // val indexId: Int,
    // val indexName: String,
)


data class SauceNaoResultData(
    val extUrls: Set<String>,
    val title: String?,
    val authorUrl: String?,
    val source: String?,
    val danbooruId: Int?,
    val gelbooruId: Int?,
    val yandereId: Int?,
    val pixivId: Int?,
    val artistName: String?,
) {
    /** Attempts to take the URLs provided by SauceNAO and turn them into useful objects. */
    fun mapUrls(): Map<SocialSite, List<String>> {
        val socials = mutableMapOf<SocialSite, List<String>>()

        // SauceNao::parseResponse also injects the source into extUrls if the source is a URL.
        for (url in extUrls) {
            SocialSite.fromUrl(url).let {
                if (it != SocialSite.IMAGEBOARD) {
                    socials[it] = (socials[it] ?: emptyList()) + url
                }
            }
        }
        return socials
    }


    fun parseImageBoards(): List<ImageView> {
        val boards = mutableListOf<ImageView>()

        danbooruId?.let {
            boards.add(ImageView(ImageSource.DANBOORU, it.toString(), isMd5 = false))
        }
        gelbooruId?.let {
            boards.add(ImageView(ImageSource.GELBOORU, it.toString(), isMd5 = false))
        }
        yandereId?.let {
            boards.add(ImageView(ImageSource.YANDERE, it.toString(), isMd5 = false))
        }

        return boards
    }
}


/* These are currently unused but might be useful in future.
// SauceNAO database index IDs for sites we support
fun getImageSourceForIndex(indexId: Int): ImageSource? {
    return when (indexId) {
        9 -> ImageSource.DANBOORU
        12 -> ImageSource.YANDERE
        25 -> ImageSource.GELBOORU
        // 26 is konachan which we don't currently support but could in the future.
        else -> null
    }
}


fun getSocialSiteForIndex(indexId: Int): SocialSite? {
    return when (indexId) {
        5, 6 -> SocialSite.PIXIV
        35 -> SocialSite.PAWOO
        41 -> SocialSite.TWITTER
        44 -> SocialSite.SKEB
        else -> null
    }
}
*/


object SauceNao {
    private const val API_URL = "https://saucenao.com/search.php"

    suspend fun search(imageUrl: String, apiKey: String, numResults: Int = 10, allowNsfw: Boolean): SauceNaoResponse {
        val encodedUrl = URLEncoder.encode(imageUrl, "utf-8")
        val encodedKey = URLEncoder.encode(apiKey, "utf-8")
        val requestUrl = "$API_URL?output_type=2&hide=${if (allowNsfw) 0 else 3}&numres=$numResults&url=$encodedUrl&api_key=$encodedKey"

        val responseBody = try {
            RequestUtil.get(requestUrl)
        } catch (e: IOException) {
            val errorText = if (e.message?.toInt() == 429) {
                "SauceNAO is limiting you. Try again later."
            } else {
                "Unknown SauceNAO error."
            }
            throw SauceNaoException(errorText, e.message?.toInt())
        }
        return parseResponse(responseBody, allowNsfw)
    }

    suspend fun searchWithFile(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        apiKey: String,
        numResults: Int = 10,
        allowNsfw: Boolean
    ): SauceNaoResponse {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("output_type", "2")
            .addFormDataPart("hide", if (allowNsfw) "0" else "3")
            .addFormDataPart("numres", numResults.toString())
            .addFormDataPart("api_key", apiKey)
            .addFormDataPart(
                "file",
                fileName,
                fileBytes.toRequestBody(mimeType.toMediaType())
            )
            .build()

        val responseBody = try {
            RequestUtil.post(API_URL, body)
        } catch (e: IOException) {
            val errorText = if (e.message?.toInt() == 429) {
                "SauceNAO is limiting you. Try again later."
            } else {
                "Unknown SauceNAO error."
            }
            throw SauceNaoException(errorText, e.message?.toInt())
        }
        return parseResponse(responseBody, allowNsfw)
    }

    private fun parseResponse(body: String, allowNsfw: Boolean): SauceNaoResponse {
        val json = JSONObject(body)
        val headerJson = json.getJSONObject("header")

        val status = headerJson.getInt("status")
        val message = headerJson.optString("message").takeIf { it.isNotEmpty() }
        var resultsCount = headerJson.optInt("results_returned", 0)

        if (status != 0) {
            val errorMessage = message ?: "SauceNAO returned status $status"
            throw SauceNaoException(errorMessage, status)
        }

        val resultsJson = json.optJSONArray("results") ?: return SauceNaoResponse(
            header = SauceNaoResponseHeader(status, message, resultsCount),
            results = emptyList()
        )

        val results = mutableListOf<SauceNaoResult>()

        for (i in 0 until resultsJson.length()) {
            val resultJson = resultsJson.getJSONObject(i)

            val resultHeaderJson = resultJson.getJSONObject("header")

            if (resultHeaderJson.optInt("hidden", 0) != 0 && !allowNsfw) {
                continue
            }

            val resultHeader = SauceNaoResultHeader(
                similarity = resultHeaderJson.optString("similarity", "0").toFloatOrNull() ?: 0f,
                thumbnail = resultHeaderJson.optString("thumbnail", ""),
                // Not currently using these but maybe in future
                // indexId = resultHeaderJson.optInt("index_id", -1),
                // indexName = resultHeaderJson.optString("index_name", ""),
            )

            val resultDataJson = resultJson.getJSONObject("data")

            /* Sometimes the source is a URL that isn't listed in ext_urls.
               We'll add it ourselves for convenience.
               If the source is also a (potentially direct image) Pixiv URL,
               we'll try to normalise it before adding it.*/
            val source = resultDataJson.optString("source").takeIf { it.isNotBlank() }
            val extUrls = mutableSetOf<String>()
            source?.let {
                if (it.isWebLink()) {
                    extUrls.add(PixivArtwork.fromUrl(it)?.toString() ?: it )
                }
            }
            resultDataJson.optJSONArray("ext_urls")?.let { arr ->
                for (index in 0 until arr.length()) {
                    val url = arr.getString(index)
                    // It might return them here too. Same again.
                    extUrls.add(PixivArtwork.fromUrl(url)?.toString() ?: url)
                }
            }

            val resultData = SauceNaoResultData(
                extUrls = extUrls,
                title = resultDataJson.optString("title").takeIf { it.isNotBlank() },
                authorUrl = resultDataJson.optString("author_url").takeIf { it.isNotBlank() },
                source = source,
                danbooruId = resultDataJson.optInt("danbooru_id", 0).takeIf { it != 0 },
                gelbooruId = resultDataJson.optInt("gelbooru_id", 0).takeIf { it != 0 },
                yandereId = resultDataJson.optInt("yandere_id", 0).takeIf { it != 0 },
                pixivId = resultDataJson.optInt("pixiv_id", 0).takeIf { it != 0 },
                artistName = resultDataJson.optString("member_name",
                    resultDataJson.optString("author_name",
                        resultDataJson.optString("creator",
                            resultDataJson.optString("twitter_user_handle"))
                    )
                ).takeIf { it.isNotBlank() }
                ?.let { // Sometimes it's a list and sometimes it's a string. Very cool.
                    if (it.startsWith("[\"")) {
                        JSONArray(it).getString(0)
                    } else {
                        it
                    }
                } ?: run {
                    resultsCount -= 1
                    continue // TODO: No artist name means it's probably from a show or media. We'll skip them for now, but should revisit. Index ID would confirm.
                },
            )

            results.add(SauceNaoResult(resultHeader, resultData))
        }

        val header = SauceNaoResponseHeader(status, message, resultsCount)

        return SauceNaoResponse(header, results)
    }
}


class SauceNaoException(message: String, val statusCode: Int?) : Exception(message)
