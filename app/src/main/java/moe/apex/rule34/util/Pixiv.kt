package moe.apex.rule34.util

private val PIXIV_RX = """https?://i\.pximg\.net/img-original/img/\d+/\d+/\d+/\d+/\d+/\d+/(\d+)_p\d+\.(png|jpg|jpeg|gif)""".toRegex()

fun extractPixivId(url: String?): Int? {
    if (url == null) return null
    val match = PIXIV_RX.find(url)
    return match?.groupValues?.get(1)?.toInt()?.takeIf { it != 0 }
}
