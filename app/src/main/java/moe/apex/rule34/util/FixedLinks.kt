package moe.apex.rule34.util


private val links = mapOf(
    "bsky.app" to "fxbsky.app",
    "pixiv.net" to "phixiv.net",
    "twitter.com" to "fxtwitter.com",
    "x.com" to "fxtwitter.com",
)


fun fixLink(link: String): String {
    for ((key, value) in links) {
        if (key in link) {
            return link.replace(key, value)
        }
    }
    return link
}
