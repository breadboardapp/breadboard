package moe.apex.breadboard.util

import androidx.core.net.toUri


private val links = mapOf(
    "bsky.app" to "fxbsky.app",
    "pixiv.net" to "phixiv.net",
    "twitter.com" to "fxtwitter.com",
    "x.com" to "fxtwitter.com",
    "bilibili.com" to "vxbilibili.com",
    "weibo.com" to "fxweibo.com", // FxWeibo is provided and run by Breadboard
)


fun fixLink(link: String): String {
    val uri = link.toUri()
    for ((originalHost, fixedHost) in links) {
        val newHost = if (uri.host == originalHost) {
            fixedHost
        } else if (uri.host!!.endsWith(".$originalHost")) {
            val subdomain = uri.host!!.substringBeforeLast(".$originalHost")
            "$subdomain.$fixedHost"
        } else {
            continue
        }

        var newPath: String? = null
        var newFragment: String? = null

        /* The phixiv fixer does not take the official pixiv image index syntax into account, so we have to use its
           own path syntax for indexed images. */
        if (fixedHost == "phixiv.net" && "/artworks/\\d+$".toRegex().containsMatchIn(uri.path ?: "")) {
            val index = uri.fragment?.toIntOrNull().takeIf { it != 0 }
            if (index != null) {
                newPath = "${uri.path}/${index + 1}"
                newFragment = ""
            }
        }

        return uri
            .buildUpon()
            .authority(newHost)
            .path(newPath ?: uri.path)
            .fragment(newFragment ?: uri.fragment)
            .build()
            .toString()
    }
    return link
}


fun String.isWebLink(): Boolean {
    return this.startsWith("http://") || this.startsWith("https://")
}
