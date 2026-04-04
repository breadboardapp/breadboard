package moe.apex.rule34.util

// Current pixiv direct image URLs
private val PIXIV_CURRENT_RX =
    // https://i.pximg.net/img-original/img/2022/11/27/21/27/08/103150283_p0.jpg (Safebooru #6517847)
    // https://i.pximg.net/img-master/img/2019/07/09/08/27/59/75629295_p0_master1200.jpg (Safebooru #3567627)
    """https?://i\.pximg\.net/img-(?:original|master)/img/\d+/\d+/\d+/\d+/\d+/\d+/(\d+)_p\d+(_master1200)?\.(png|jpg|jpeg|gif)""".toRegex()

// 2012-2016 pixiv direct image URLs
private val PIXIV_2012_TO_2016_RX = listOf(
    // https://i1.pixiv.net/img-original/img/2016/10/02/16/47/39/59270556_p0.jpg (Safebooru #1843535)
    // No source, but I'd assume an `image-master` version exists too on this old domain
    """https?://i\d+\.pixiv\.net/img-(?:original|master)/img/\d+/\d+/\d+/\d+/\d+/\d+/(\d+)_p\d+(_master1200)?\.(png|jpg|jpeg|gif)""".toRegex(),

    // https://i1.pixiv.net/img47/img/l3lc201/34464791.png (Safebooru #1000441)
    // https://i1.pixiv.net/img21/img/togainuakira/34478247_big_p8.jpg (Safebooru #1000649)
    """https?://i\d+\.pixiv\.net/img\d+/img/.+/(\d+)(_big_p\d+)?\.(png|jpg|jpeg|gif)""".toRegex()
)

// Pre-2012 pixiv direct image URLs
private val PIXIV_PRE_2012_RX =
    // https://img13.pixiv.net/img/tubasarei/4894590.jpg (Safebooru #166629)
    """https?://img\d+\.pixiv\.net/img/.+/(\d+)\.(png|jpg|jpeg|gif)""".toRegex()

private val PIXIV_RX = listOf(PIXIV_CURRENT_RX) + PIXIV_2012_TO_2016_RX + PIXIV_PRE_2012_RX

fun extractPixivId(url: String?): Int? {
    if (url == null) return null

    for (regex in PIXIV_RX) {
        val match = regex.find(url)
        val id = match?.groupValues?.get(1)?.toInt()?.takeIf { it != 0 }
        if (id != null) return id
    }

    return null
}
