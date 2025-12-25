package moe.apex.rule34.navigation

import android.net.Uri
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.ImageSource.DANBOORU
import moe.apex.rule34.preferences.ImageSource.GELBOORU
import moe.apex.rule34.preferences.ImageSource.R34
import moe.apex.rule34.preferences.ImageSource.SAFEBOORU
import moe.apex.rule34.preferences.ImageSource.YANDERE
import kotlin.reflect.KClass


@Serializable
data class ImageView(
    val source: ImageSource,
    val id: String
) {
    companion object {
        fun fromUri(uri: Uri): ImageView? {
            val imageSource = when (uri.host) {
                "safebooru.org" -> SAFEBOORU
                "danbooru.donmai.us" -> DANBOORU
                "gelbooru.com" -> GELBOORU
                "yande.re", "files.yande.re" -> YANDERE
                "rule34.xxx" -> R34
                else -> return null
            }

            val postId = when (imageSource) {
                SAFEBOORU,
                GELBOORU,
                R34 -> uri.getQueryParameter("id")
                DANBOORU -> uri.path?.split('/')?.getOrNull(2)
                YANDERE -> {
                    val postId = uri.path?.split('/')?.getOrNull(3)
                    if (uri.host == "files.yande.re") postId?.split(" ")?.getOrNull(1) else postId
                }
            } ?: return null

            return ImageView(imageSource, postId)
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
