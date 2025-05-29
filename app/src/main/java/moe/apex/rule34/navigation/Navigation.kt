package moe.apex.rule34.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import moe.apex.rule34.search.SearchScreen
import moe.apex.rule34.R
import moe.apex.rule34.detailview.SearchResults
import moe.apex.rule34.favourites.FavouritesPage
import moe.apex.rule34.largeimageview.LazyLargeImageView
import moe.apex.rule34.preferences.PreferencesScreen
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.util.withoutVertical
import moe.apex.rule34.viewmodel.BreadboardViewModel


@Composable
fun Navigation(navController: NavHostController, viewModel: BreadboardViewModel, startDestination: Any = Search) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val bottomBarVisibleState = remember { mutableStateOf(true) }
    val currentBSE by navController.currentBackStackEntryAsState()
    val currentRoute = currentBSE?.destination
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val slideDistance = remember {
        val distance = 70.let { if(isRtl) -it else it }.dp
        with(density) { distance.roundToPx() }
    }

    val easing = CubicBezierEasing(0.4f, 0.0f, 0.0f, 1f)
    val enterTransition = slideInHorizontally(tween(easing = easing), { slideDistance }) + fadeIn(tween(easing = easing))
    val exitTransition = slideOutHorizontally(tween(easing = easing), { -slideDistance }) + fadeOut(tween(easing = easing))
    val popExitTransition = slideOutHorizontally(tween(easing = easing),  { slideDistance }) + fadeOut( tween(easing = easing))
    val popEnterTransition = slideInHorizontally(tween(easing = easing), { -slideDistance }) + fadeIn(tween(easing = easing))

    BreadboardTheme {
        Surface {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = currentRoute.routeIs(Search::class, Settings::class, Favourites::class)
                                && bottomBarVisibleState.value,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        NavigationBar {
                            NavigationBarItem(
                                label = { Text("Search") },
                                selected = currentRoute.routeIs(Search::class, Results::class),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Search, // Outlined and Filled search are the same
                                        contentDescription = "Search"
                                    )
                                },
                                onClick = {
                                    if (!currentRoute.routeIs(Search::class)) {
                                        navController.navigate(Search) {
                                            popUpTo(Search) { inclusive = true }
                                        }
                                    } else {
                                        focusRequester.requestFocus()
                                        keyboard?.show() /* Not technically necessary but allows the keyboard to appear
                                                            again if the user taps away while the search bar is still
                                                            focused */
                                    }
                                }
                            )
                            NavigationBarItem(
                                label = { Text("Favourites") },
                                selected = currentRoute.routeIs(Favourites::class),
                                icon = {
                                    Icon(
                                        painter = painterResource(
                                            if (currentRoute.routeIs(Favourites::class)) R.drawable.ic_star_filled
                                            else R.drawable.ic_star_hollow
                                        ),
                                        contentDescription = "Favourite images"
                                    )
                                },
                                onClick = {
                                    if (!currentRoute.routeIs(Favourites::class)) {
                                        navController.navigate(Favourites) {
                                            popUpTo(Favourites) { inclusive = true }
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                label = { Text("Settings") },
                                selected = currentRoute.routeIs(Settings::class),
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute.routeIs(Settings::class)) Icons.Filled.Settings
                                        else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                onClick = {
                                    if (!currentRoute.routeIs(Settings::class)) {
                                        navController.navigate(Settings) {
                                            popUpTo(Settings) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                NavHost(
                    modifier = Modifier.padding(paddingValues.withoutVertical()),
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = {
                        if (targetState.destination.routeIs(Results::class, ImageView::class))
                            enterTransition
                        else fadeIn()
                    },
                    exitTransition = {
                        if (targetState.destination.routeIs(Results::class, ImageView::class))
                            exitTransition
                        else fadeOut()
                    },
                    popEnterTransition = {
                        if (initialState.destination.routeIs(Results::class, ImageView::class))
                            popEnterTransition
                        else fadeIn()
                    },
                    popExitTransition = {
                        if (initialState.destination.routeIs(Results::class, ImageView::class))
                            popExitTransition
                        else fadeOut()
                    }
                ) {
                    composable<ImageView> {
                        val args = it.toRoute<ImageView>()
                        LazyLargeImageView(navController) { args.source.site.loadImage(args.id) }
                    }
                    composable<Search> { SearchScreen(navController, focusRequester, viewModel) }
                    composable<Results> {
                        val args = it.toRoute<Results>()
                        SearchResults(navController, args.source, args.tags)
                    }
                    composable<Favourites> { FavouritesPage(navController, bottomBarVisibleState) }
                    composable<Settings> { PreferencesScreen(viewModel) }
                }
            }
        }
    }
}
