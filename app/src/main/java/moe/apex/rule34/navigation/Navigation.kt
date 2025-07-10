package moe.apex.rule34.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import moe.apex.rule34.home.HomeScreen
import moe.apex.rule34.largeimageview.LazyLargeImageView
import moe.apex.rule34.preferences.BlockedTagsScreen
import moe.apex.rule34.preferences.LibrariesScreen
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.PreferencesScreen
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.util.withoutVertical
import moe.apex.rule34.viewmodel.BreadboardViewModel


@Composable
fun Navigation(navController: NavHostController, viewModel: BreadboardViewModel, startDestination: Any = Search) {
    val density = LocalDensity.current
    val prefs = LocalPreferences.current
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

    val topLevelScreens = listOf(Home::class, Search::class, Favourites::class, Settings::class, BlockedTagsSettings::class, LibrariesSettings::class)
    val searchScreens = listOf(Search::class, Results::class)
    val settingsScreens = listOf(Settings::class, BlockedTagsSettings::class, LibrariesSettings::class)

    BreadboardTheme {
        Surface {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = currentRoute.routeIs(topLevelScreens)
                                && bottomBarVisibleState.value,
                        enter = slideInVertically { it /3} + fadeIn(),
                        exit = slideOutVertically { it/3 } + fadeOut()
                    ) {
                        NavigationBar {
                            NavigationBarItem(
                                label = { Text("Browse") },
                                selected = currentRoute.routeIs(Home::class),
                                icon = {
                                    Icon(
                                        painter = painterResource(if (currentRoute.routeIs(Home::class)) R.drawable.ic_home_filled else R.drawable.ic_home_hollow),
                                        contentDescription = "Browse"
                                    )
                                },
                                onClick = {
                                    if (!currentRoute.routeIs(Home::class)) {
                                        navController.navigate(Home) {
                                            popUpTo(Home) { inclusive = true }
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                label = { Text("Search") },
                                selected = currentRoute.routeIs(searchScreens),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
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
                                        imageVector = if (currentRoute.routeIs(Favourites::class)) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
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
                                selected = currentRoute.routeIs(settingsScreens),
                                icon = {
                                    Icon(
                                        painter = if (currentRoute.routeIs(settingsScreens)) rememberVectorPainter(Icons.Rounded.Settings) else painterResource(R.drawable.ic_settings_hollow),
                                        contentDescription = "Settings"
                                    )
                                },
                                onClick = {
                                    if (currentRoute.routeIs(settingsScreens.filter { it != Settings::class })) {
                                        navController.popBackStack()
                                    } else if (!currentRoute.routeIs(Settings::class)) {
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
                        if (targetState.destination.routeIs(Results::class, ImageView::class, BlockedTagsSettings::class, LibrariesSettings::class))
                            enterTransition
                        else fadeIn()
                    },
                    exitTransition = {
                        if (targetState.destination.routeIs(Results::class, ImageView::class, BlockedTagsSettings::class, LibrariesSettings::class))
                            exitTransition
                        else fadeOut()
                    },
                    popEnterTransition = {
                        if (initialState.destination.routeIs(Results::class, ImageView::class, BlockedTagsSettings::class, LibrariesSettings::class))
                            popEnterTransition
                        else fadeIn()
                    },
                    popExitTransition = {
                        if (initialState.destination.routeIs(Results::class, ImageView::class, BlockedTagsSettings::class, LibrariesSettings::class))
                            popExitTransition
                        else fadeOut()
                    }
                ) {
                    composable<ImageView> {
                        val args = it.toRoute<ImageView>()
                        LazyLargeImageView(navController) { args.source.imageBoard.loadImage(args.id, prefs.authFor(args.source)) }
                    }
                    composable<Home> { HomeScreen(navController, viewModel, bottomBarVisibleState) }
                    composable<Search> { SearchScreen(navController, focusRequester, viewModel) }
                    composable<Results> {
                        val args = it.toRoute<Results>()
                        SearchResults(navController, args.source, args.tags)
                    }
                    composable<Favourites> { FavouritesPage(navController, bottomBarVisibleState) }
                    composable<Settings> { PreferencesScreen(navController, viewModel) }
                    composable<BlockedTagsSettings> { BlockedTagsScreen(navController) }
                    composable<LibrariesSettings> { LibrariesScreen(navController) }
                }
            }
        }
    }
}
