package moe.apex.breadboard.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
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
import moe.apex.breadboard.search.SearchScreen
import moe.apex.breadboard.R
import moe.apex.breadboard.artist.ArtistProfileScreen
import moe.apex.breadboard.detailview.SearchResults
import moe.apex.breadboard.favourites.FavouritesPage
import moe.apex.breadboard.home.HomeScreen
import moe.apex.breadboard.largeimageview.LazyLargeImageView
import moe.apex.breadboard.preferences.AboutScreen
import moe.apex.breadboard.preferences.BlockedTagsScreen
import moe.apex.breadboard.preferences.ContentSettingsScreen
import moe.apex.breadboard.preferences.DataSettingsScreen
import moe.apex.breadboard.preferences.ExperimentalScreen
import moe.apex.breadboard.preferences.GeneralSettingsScreen
import moe.apex.breadboard.preferences.IgnoredTagsScreen
import moe.apex.breadboard.preferences.LayoutSettingsScreen
import moe.apex.breadboard.preferences.LibrariesScreen
import moe.apex.breadboard.preferences.PreferencesScreen
import moe.apex.breadboard.preferences.RecommendationsSettingsScreen
import moe.apex.breadboard.saucenao.SauceNaoResultsScreen
import moe.apex.breadboard.saucenao.ReverseSearchScreen
import moe.apex.breadboard.ui.theme.BreadboardTheme
import moe.apex.breadboard.util.withoutVertical
import moe.apex.breadboard.home.FollowedArtistsScreen
import moe.apex.breadboard.preferences.ApiKeysSettingsScreen
import moe.apex.breadboard.util.WhatsNew


@Composable
fun Navigation(navController: NavHostController, startDestination: Any = Search) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var isNavigationBarVisible by remember { mutableStateOf(true) }
    val currentBSE by navController.currentBackStackEntryAsState()
    val currentRoute = currentBSE?.destination
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val slideDistance = remember {
        val distance = 70.let { if(isRtl) -it else it }.dp
        with(density) { distance.roundToPx() }
    }

    val easing = CubicBezierEasing(0.4f, 0.0f, 0.0f, 1f)
    val enterTransition = slideInHorizontally(tween(easing = easing)) { slideDistance } + fadeIn(tween(easing = easing))
    val exitTransition = slideOutHorizontally(tween(easing = easing)) { -slideDistance } + fadeOut(tween(easing = easing))
    val popExitTransition = slideOutHorizontally(tween(easing = easing)) { slideDistance } + fadeOut( tween(easing = easing))
    val popEnterTransition = slideInHorizontally(tween(easing = easing)) { -slideDistance } + fadeIn(tween(easing = easing))

    val homeScreens = listOf(Home::class, FollowedArtists::class)
    val searchScreens = listOf(Search::class, Results::class, ArtistProfile::class)
    val reverseSearchScreens = listOf(ReverseSearch::class, SauceNaoResults::class)
    val settingsScreens = listOf(
        Settings::class,
        GeneralSettings::class,
        ApiKeysSettings::class,
        ContentSettings::class,
        LayoutSettings::class,
        DataSettings::class,
        BlockedTagsSettings::class,
        AboutSettings::class,
        LibrariesSettings::class,
        ExperimentalSettings::class,
        RecommendationsSettings::class,
        IgnoredTagsSettings::class
    )
    val topLevelScreens = listOf(Home::class, Search::class, ReverseSearch::class, Favourites::class, FollowedArtists::class) + settingsScreens
    val slideTransitionScreens = listOf(Results::class, ImageView::class, ArtistProfile::class, FollowedArtists::class, SauceNaoResults::class, *settingsScreens.filter { it != Settings::class }.toTypedArray())

    /* Some screens have the ability to hide the bottom bar, so we need to ensure it appears again
       when navigating to a different screen. */
    SideEffect(currentRoute) {
        if (currentRoute.routeIs(topLevelScreens)) {
            isNavigationBarVisible = true
        }
    }

    BreadboardTheme {
        WhatsNew()

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = currentRoute.routeIs(topLevelScreens) && isNavigationBarVisible,
                    enter = slideInVertically { it /3} + fadeIn(),
                    exit = slideOutVertically { it/3 } + fadeOut()
                ) {
                    NavigationBar(containerColor = BreadboardTheme.colors.titleBar) {
                        NavigationBarItem(
                            label = { Text("Browse") },
                            selected = currentRoute.routeIs(homeScreens),
                            icon = {
                                Icon(
                                    painter = painterResource(if (currentRoute.routeIs(homeScreens)){
                                        R.drawable.ic_home_filled
                                    } else {
                                        R.drawable.ic_home_hollow
                                    }),
                                    contentDescription = "Browse",
                                    modifier = Modifier.pulseOnSelect(currentRoute.routeIs(homeScreens))
                                )
                            },
                            onClick = {
                                /* The 2nd clause is currently always true,
                                   but might not be in the future.
                                   Handling that now because I'll probably forget otherwise. */
                                if (
                                    currentRoute.routeIs(FollowedArtists::class) &&
                                    navController.previousBackStackEntry?.destination.routeIs(Home::class)
                                ) {
                                    navController.popBackStack()
                                } else if (!currentRoute.routeIs(Home::class)) {
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
                                    contentDescription = "Search",
                                    modifier = Modifier.pulseOnSelect(currentRoute.routeIs(searchScreens))
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
                            label = { Text("SauceNAO") },
                            selected = currentRoute.routeIs(reverseSearchScreens),
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        if (currentRoute.routeIs(reverseSearchScreens)) {
                                            R.drawable.ic_image_search_filled
                                        } else {
                                            R.drawable.ic_image_search_hollow
                                        }
                                    ),
                                    contentDescription = "SauceNAO Reverse image search",
                                    modifier = Modifier.pulseOnSelect(currentRoute.routeIs(reverseSearchScreens))
                                )
                            },
                            onClick = {
                                if (!currentRoute.routeIs(ReverseSearch::class)) {
                                    navController.navigate(ReverseSearch()) {
                                        popUpTo(ReverseSearch()) { inclusive = true }
                                    }
                                }
                            }
                        )
                        NavigationBarItem(
                            label = { Text("Favourites") },
                            selected = currentRoute.routeIs(Favourites::class),
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute.routeIs(Favourites::class)) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Favourite posts",
                                    modifier = Modifier.pulseOnSelect(currentRoute.routeIs(Favourites::class))
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
                                    contentDescription = "Settings",
                                    modifier = Modifier.pulseOnSelect(currentRoute.routeIs(settingsScreens))
                                )
                            },
                            onClick = {
                                /* The 2nd clause might be false if the user clicks a button that
                                   takes them to a specific settings page.
                                   In such cases, tapping the settings tab should take them to the
                                   settings home page, not back to where they were before. */
                                if (
                                    currentRoute.routeIs(settingsScreens.filter { it != Settings::class }) &&
                                    navController.previousBackStackEntry?.destination.routeIs(settingsScreens)
                                ) {
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
                    if (targetState.destination.routeIs(slideTransitionScreens))
                        enterTransition
                    else fadeIn()
                },
                exitTransition = {
                    if (targetState.destination.routeIs(slideTransitionScreens))
                        exitTransition
                    else fadeOut()
                },
                popEnterTransition = {
                    if (initialState.destination.routeIs(slideTransitionScreens))
                        popEnterTransition
                    else fadeIn()
                },
                popExitTransition = {
                    if (initialState.destination.routeIs(slideTransitionScreens))
                        popExitTransition
                    else fadeOut()
                }
            ) {
                composable<ImageView> {
                    val args = it.toRoute<ImageView>()
                    LazyLargeImageView(navController, args.source, args.id, args.isMd5)
                }
                composable<Home> { HomeScreen(navController) { isNavigationBarVisible = it } }
                composable<Search> { SearchScreen(navController, focusRequester) }
                composable<ReverseSearch> {
                    val args = it.toRoute<ReverseSearch>()
                    ReverseSearchScreen(navController, args.initialImageUrl, args.initialFileUri)
                }
                composable<Results> {
                    val args = it.toRoute<Results>()
                    SearchResults(navController, args.source, args.tags)
                }
                composable<Favourites> { FavouritesPage(navController) { isNavigationBarVisible = it } }
                composable<Settings> { PreferencesScreen(navController) }
                composable<GeneralSettings> { GeneralSettingsScreen(navController) }
                composable<ApiKeysSettings> { ApiKeysSettingsScreen(navController) }
                composable<ContentSettings> { ContentSettingsScreen(navController) }
                composable<LayoutSettings> { LayoutSettingsScreen(navController) }
                composable<DataSettings> { DataSettingsScreen(navController) }
                composable<BlockedTagsSettings> { BlockedTagsScreen(navController) }
                composable<LibrariesSettings> { LibrariesScreen(navController) }
                composable<AboutSettings> { AboutScreen(navController) }
                composable<ExperimentalSettings> { ExperimentalScreen(navController) }
                composable<RecommendationsSettings> { RecommendationsSettingsScreen(navController) }
                composable<IgnoredTagsSettings> { IgnoredTagsScreen(navController) }
                composable<ArtistProfile> {
                    val args = it.toRoute<ArtistProfile>()
                    ArtistProfileScreen(args.artistTag, args.originImageSource, navController = navController)
                }
                composable<FollowedArtists> { FollowedArtistsScreen(navController) }
                composable<SauceNaoResults> {
                    val args = it.toRoute<SauceNaoResults>()
                    SauceNaoResultsScreen(navController, args.imageUrl, args.fileUri)
                }
            }
        }
    }
}


@Composable
private fun Modifier.pulseOnSelect(selected: Boolean): Modifier {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected) {
            scale.animateTo(1.1f, tween(200))
            scale.animateTo(1f, tween(300))
        }
    }

    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
