package moe.apex.breadboard.artist

import androidx.compose.runtime.Immutable
import moe.apex.breadboard.social.SocialEntry
import moe.apex.breadboard.social.SocialSite


@Immutable
data class Artist(
    val name: String,
    val otherNames: List<String>,
    val socialUrls: List<SocialEntry>,
    val tag: ArtistTag
) {
    fun groupSocials(): Map<SocialSite, List<SocialEntry>> {
        return socialUrls.groupBy { SocialSite.fromUrl(it.url) }
            .toList()
            .sortedWith(compareBy<Pair<SocialSite, List<SocialEntry>>> {
                when (it.first) { // Site priority, always show Pixiv/Fanbox/Twitter first and OTHER last.
                    SocialSite.PIXIV -> 0
                    SocialSite.FANBOX -> 1
                    SocialSite.TWITTER -> 2
                    SocialSite.OTHER -> 4
                    else -> 3
                }
            }.thenBy { it.first.label.lowercase() }) // And then every other site is alphabetical.
            .toMap()
    }
}
