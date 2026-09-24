package moe.apex.breadboard.util

// Regular public post URLs
private val PIXIV_POST_RX =
    // https://www.pixiv.net/artworks/128105617
    // https://www.pixiv.net/en/artworks/128105617
    // https://www.pixiv.net/member_illust.php?mode=medium&illust_id=128105617
    """https?://(?:www\.)?pixiv\.net/(?:(?:[a-z]{2}/)?artworks/|member_illust\.php\?.*?illust_id=)(?<id>\d+)""".toRegex()

// Current pixiv direct image URLs
private val PIXIV_CURRENT_RX =
    // https://i.pximg.net/img-original/img/2022/11/27/21/27/08/103150283_p0.jpg (Safebooru #6517847)
    // https://i.pximg.net/img-master/img/2019/07/09/08/27/59/75629295_p0_master1200.jpg (Safebooru #3567627)
    // https://i.pximg.net/img-original/img/2026/05/13/20/30/05/144732134-2ec1cc9314f12cef1751801f82cc21a0_p0.jpg (Safebooru #6757725)
    """https?://i\.pximg\.net/img-(original|master)/img/\d+/\d+/\d+/\d+/\d+/\d+/(?<id>\d+)(-[0-9a-f]+)?(?:_p(?<index>\d+)(_master1200)?\.(png|jpg|jpeg|gif))?""".toRegex()

// 2012-2016 pixiv direct image URLs
private val PIXIV_2012_TO_2016_RX = listOf(
    // https://i1.pixiv.net/img-original/img/2016/10/02/16/47/39/59270556_p0.jpg (Safebooru #1843535)
    // No source, but I'd assume an `img-master` version exists too on this old subdomain
    """https?://i\d+\.pixiv\.net/img-(original|master)/img/\d+/\d+/\d+/\d+/\d+/\d+/(?<id>\d+)_p(?<index>\d+)(_master1200)?\.(png|jpg|jpeg|gif)""".toRegex(),

    // https://i1.pixiv.net/img47/img/l3lc201/34464791.png (Safebooru #1000441)
    // https://i1.pixiv.net/img21/img/togainuakira/34478247_big_p8.jpg (Safebooru #1000649)
    """https?://i\d+\.pixiv\.net/img\d+/img/.+/(?<id>\d+)(_big_p(?<index>\d+))?\.(png|jpg|jpeg|gif)""".toRegex()
)

// Pre-2012 pixiv direct image URLs
private val PIXIV_PRE_2012_RX =
    // https://img13.pixiv.net/img/tubasarei/4894590.jpg (Safebooru #166629)
    """https?://img\d+\.pixiv\.net/img/.+/(?<id>\d+)\.(png|jpg|jpeg|gif)""".toRegex()


data class PixivArtwork(val id: Int, val index: Int) {
    companion object {
        fun fromUrl(url: String?): PixivArtwork? {
            if (url == null) return null

            /* Pre-2012 Pixiv URLs don't seem to have indexes at all.
               It feels better and more intentional/deliberate to handle them specifically,
               at least compared to putting a try/catch around the index group. */

            val indexedRegexes = PIXIV_2012_TO_2016_RX + PIXIV_CURRENT_RX
            val nonIndexedRegexes = listOf(PIXIV_POST_RX, PIXIV_PRE_2012_RX)

            for (regex in indexedRegexes) {
                val match = regex.find(url) ?: continue
                val id = match.groups["id"]?.value?.toIntOrNull().takeIf { it != 0 } ?: continue
                val index = match.groups["index"]?.value?.toIntOrNull() ?: 0
                return PixivArtwork(id, index)
            }

            for (regex in nonIndexedRegexes) {
                val match = regex.find(url) ?: continue
                val id = match.groups["id"]?.value?.toIntOrNull().takeIf { it != 0 } ?: continue
                return PixivArtwork(id, 0)
            }

            return null
        }
    }

    /** Return the artwork as a regular Pixiv link. */
    override fun toString(): String {
        return "https://www.pixiv.net/artworks/$id" + if (index != 0) "#$index" else ""
    }
}
