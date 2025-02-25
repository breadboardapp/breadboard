package moe.apex.rule34.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import moe.apex.rule34.HomeScreen
import moe.apex.rule34.R
import moe.apex.rule34.detailview.SearchResults
import moe.apex.rule34.favourites.FavouritesPage
import moe.apex.rule34.largeimageview.LazyLargeImageView
import moe.apex.rule34.preferences.PreferencesScreen
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.util.withoutVertical
import moe.apex.rule34.viewmodel.BreadboardViewModel
import soup.compose.material.motion.animation.materialSharedAxisXIn
import soup.compose.material.motion.animation.materialSharedAxisXOut
import soup.compose.material.motion.animation.rememberSlideDistance


@Composable
fun Navigation(navController: NavHostController, viewModel: BreadboardViewModel, uri: Uri? = null) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val slideDistance = rememberSlideDistance()
    val bottomBarVisibleState = remember { mutableStateOf(true) }
    val currentBSE by navController.currentBackStackEntryAsState()
    val currentRoute = currentBSE?.destination
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

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
                    startDestination = uri?.let { ImageView.fromUri(it) } ?: Search,
                    enterTransition = {
                        if (targetState.destination.routeIs(Results::class))
                            materialSharedAxisXIn(!isRtl, slideDistance)
                        else fadeIn()
                    },
                    exitTransition = {
                        if (targetState.destination.routeIs(Results::class))
                            materialSharedAxisXOut(!isRtl, slideDistance)
                        else fadeOut()
                    },
                    popEnterTransition = {
                        if (initialState.destination.routeIs(Results::class))
                            materialSharedAxisXIn(isRtl, slideDistance)
                        else fadeIn()
                    },
                    popExitTransition = {
                        if (initialState.destination.routeIs(Results::class))
                            materialSharedAxisXOut(isRtl, slideDistance)
                        else fadeOut()
                    }
                ) {
                    composable<ImageView> {
                        val args = it.toRoute<ImageView>()
                        LazyLargeImageView(navController) { args.source.site.loadImage(args.id) }
                    }
                    composable<Search> { HomeScreen(navController, focusRequester, viewModel) }
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
