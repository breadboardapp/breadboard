package moe.apex.rule34.util

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
        if (uri.host == originalHost) {
            return uri.buildUpon().authority(fixedHost).build().toString()
        } else if (uri.host!!.endsWith(".$originalHost")) {
            val subdomain = uri.host!!.substringBeforeLast(".$originalHost")
            val newHost = "$subdomain.$fixedHost"
            return uri.buildUpon().authority(newHost).build().toString()
        }
    }
    return link
}


fun String.isWebLink(): Boolean {
    return this.startsWith("http://") || this.startsWith("https://")
}
