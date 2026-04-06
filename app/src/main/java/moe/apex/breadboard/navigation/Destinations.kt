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
            val host = uri.host ?: return null
            val path = uri.path ?: return null

            val imageSource = when (host) {
                "safebooru.org" -> SAFEBOORU
                "danbooru.donmai.us", "cdn.donmai.us" -> DANBOORU
                "yande.re", "files.yande.re", "assets.yande.re" -> YANDERE
                else -> {
                    // Gelbooru and R34 have dynamic CDN subdomains. We'll just handle them here.
                    if (host == "gelbooru.com" || host.endsWith(".gelbooru.com")) GELBOORU
                    else if (host == "rule34.xxx" || host.endsWith(".rule34.xxx")) R34
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
                GELBOORU, R34 -> {
                    if (path.startsWith("/index.php")) {
                        uri.getQueryParameter("id")
                    } else {
                        isMd5 = true
                        path.split('/', '_').lastOrNull()?.split('.')?.firstOrNull()
                    }
                }
                DANBOORU -> {
                    if (path.startsWith("/posts/")) {
                        path.split('/').getOrNull(2)
                    } else {
                        isMd5 = true
                        path.split('/', '_', '-').lastOrNull()?.split('.')?.firstOrNull()
                    }
                }
                YANDERE -> {
                    if (path.startsWith("/post/show/")) {
                        path.split('/').getOrNull(3)
                    } else {
                        isMd5 = true
                        if (path.startsWith("/data/preview/"))
                            path.split('/').getOrNull(5)?.split('.')?.firstOrNull()
                        else
                            path.split('/').getOrNull(2)
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
