package moe.apex.breadboard.navigation

import android.net.Uri
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.ImageSource.DANBOORU
import moe.apex.breadboard.preferences.ImageSource.GELBOORU
import moe.apex.breadboard.preferences.ImageSource.R34
import moe.apex.breadboard.preferences.ImageSource.SAFEBOORU
import moe.apex.breadboard.preferences.ImageSource.YANDERE
import kotlin.reflect.KClass


@Serializable
data class ImageView(
    val source: ImageSource,
    val id: String,
    val isMd5: Boolean
) {
    companion object {
        fun fromUri(uri: Uri): ImageView? {
            val imageSource = when (uri.host) {
                "safebooru.org" -> SAFEBOORU
                "danbooru.donmai.us", "cdn.donmai.us" -> DANBOORU
                "yande.re", "files.yande.re" -> YANDERE
                else -> {
                    // Gelbooru and R34 have dynamic CDN subdomains. We'll just handle them here.
                    if (uri.host == "gelbooru.com" || uri.host?.endsWith(".gelbooru.com") == true) GELBOORU
                    else if (uri.host == "rule34.xxx" || uri.host?.endsWith(".rule34.xxx") == true) R34
                    else return null
                }
            }

            var isMd5 = false

            val id = when (imageSource) {
                SAFEBOORU -> {
                    /* Safebooru does not have actual image MD5 hashes inside their direct file URLs,
                       so we cannot load images from them. */
                    uri.getQueryParameter("id")
                }
                DANBOORU -> {
                    if (uri.host == "danbooru.donmai.us") {
                        uri.path?.split('/')?.getOrNull(2)
                    } else {
                        isMd5 = true
                        uri.path?.split('/', '_', '-')?.lastOrNull()?.split(".")?.firstOrNull()
                    }
                }
                GELBOORU -> {
                    if (uri.host == "gelbooru.com") {
                        uri.getQueryParameter("id")
                    } else {
                        isMd5 = true
                        uri.path?.split('/', '_')?.lastOrNull()?.split(".")?.firstOrNull()
                    }
                }
                YANDERE -> {
                    if (uri.host == "yande.re") {
                        uri.path?.split('/')?.getOrNull(3)
                    } else {
                        isMd5 = true
                        uri.path?.split('/')?.getOrNull(2)
                    }
                }
                R34 -> {
                    if (uri.host == "rule34.xxx") {
                        uri.getQueryParameter("id")
                    } else {
                        isMd5 = true
                        uri.path?.split('/', '_')?.lastOrNull()?.split(".")?.firstOrNull()
                    }
                }
            } ?: return null

            return ImageView(imageSource, id, isMd5)
        }
    }
}

@Serializable
object Home

@Serializable
object Search

@Serializable
data class Results(
    val source: ImageSource,
    val tags: List<String>
)

@Serializable
object Favourites

@Serializable
object Settings

@Serializable
object BlockedTagsSettings

@Serializable
object LibrariesSettings

@Serializable
object AboutSettings

@Serializable
object ExperimentalSettings

@Serializable
object RecommendationsSettings

@Serializable
object IgnoredTagsSettings

fun NavDestination?.routeIs(routes: Collection<KClass<*>>): Boolean {
    return routeIs(*routes.toTypedArray())
}


fun NavDestination?.routeIs(vararg routes: KClass<*>): Boolean {
    for (route in routes) {
        if (this?.hasRoute(route) == true)
            return true
    }
    return false
}
