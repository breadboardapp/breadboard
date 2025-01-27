package moe.apex.rule34.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass


@Serializable
object Search

@Serializable
data class Results(
    val searchQuery: String,
)

@Serializable
object Favourites

@Serializable
object Settings


fun NavDestination?.routeIs(vararg routes: KClass<*>): Boolean {
    for (route in routes) {
        if (this?.hasRoute(route) == true)
            return true
    }
    return false
}
