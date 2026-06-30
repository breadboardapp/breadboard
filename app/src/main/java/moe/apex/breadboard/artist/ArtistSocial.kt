package moe.apex.breadboard.artist

import androidx.compose.runtime.Immutable
import moe.apex.breadboard.R
import androidx.core.net.toUri


@Immutable
data class ArtistSocial(
    val url: String,
    val isActive: Boolean
)


enum class SocialSite(
    val label: String,
    val color: Long?,
    val iconRes: Int,
    private val domains: List<String>
) {
    BILIBILI("Bilibili", 0xFF4C93FF, R.drawable.ic_bilibili, listOf("bilibili.com")),
    BLUESKY("Bluesky", 0xFF1185FE, R.drawable.ic_bluesky, listOf("bsky.social", "bsky.app")),
    DISCORD("Discord", 0xFF5865F2, R.drawable.ic_discord, listOf("discord.com", "discordapp.com", "discord.gg")),
    FACEBOOK("Facebook", 0xFF1877F2, R.drawable.ic_facebook, listOf("facebook.com")),
    FANBOX("pixivFANBOX", 0xFF958F5A, R.drawable.ic_pixiv, listOf("fanbox.cc")), // Fanbox is part of Pixiv and I can't find an appropriate dedicated icon.
    INSTAGRAM("Instagram", 0xFFC13584, R.drawable.ic_instagram, listOf("instagram.com")),
    LOFTER("Lofter", 0xFF459A94, R.drawable.ic_lofter, listOf("lofter.com")),
    PATREON("Patreon", 0xFF000000, R.drawable.ic_patreon, listOf("patreon.com")),
    PAWOO("Pawoo", 0xFF6364FF, R.drawable.ic_mastodon, listOf("pawoo.net")),
    PIXIV("pixiv", 0xFF0096FA, R.drawable.ic_pixiv, listOf("pixiv.net")),
    REDDIT("Reddit", 0xFFFF4500, R.drawable.ic_reddit, listOf("reddit.com", "old.reddit.com")),
    SKEB("Skeb", 0xFF1E5E71, R.drawable.ic_skeb, listOf("skeb.jp")),
    THREADS("Threads", 0xFF000000, R.drawable.ic_threads, listOf("threads.net", "threads.com")),
    TIKTOK("TikTok", 0xFF000000, R.drawable.ic_tiktok, listOf("tiktok.com")),
    TWITTER("Twitter", 0xFF1DA1F2, R.drawable.ic_twitter, listOf("twitter.com", "x.com")),
    WEIBO("Weibo", 0xFFE6162D, R.drawable.ic_weibo, listOf("weibo.com", "weibo.cn")),
    YOUTUBE("YouTube", 0xFFFF1A47, R.drawable.ic_youtube, listOf("youtube.com", "youtu.be")),
    OTHER("Other links", null, R.drawable.ic_link, emptyList());

    companion object {
        fun fromUrl(url: String): SocialSite {
            val uri = try {
                url.toUri()
            } catch (_: Exception) {
                null
            }
            val host = uri?.host?.lowercase()?.removePrefix("www.") ?: ""

            if (host.isEmpty()) {
                val lowercaseUrl = url.lowercase()
                return entries.find { site ->
                    site.domains.any { domain -> lowercaseUrl.contains(domain) }
                } ?: OTHER
            }

            return entries.find { site ->
                site.domains.any { domain ->
                    host == domain || host.endsWith(".$domain")
                }
            } ?: OTHER
        }
    }
}
