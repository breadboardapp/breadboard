@file:OptIn(ExperimentalMaterial3Api::class)

package moe.apex.rule34

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import moe.apex.rule34.detailview.SearchResults
import moe.apex.rule34.favourites.FavouritesPage
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.PreferencesScreen
import moe.apex.rule34.preferences.Prefs
import moe.apex.rule34.preferences.UserPreferencesRepository
import moe.apex.rule34.tag.TagSuggestion
import moe.apex.rule34.ui.theme.ProcrasturbatingTheme
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.NAV_BAR_HEIGHT
import moe.apex.rule34.util.withoutVertical
import soup.compose.material.motion.animation.materialSharedAxisXIn
import soup.compose.material.motion.animation.materialSharedAxisXOut
import soup.compose.material.motion.animation.rememberSlideDistance


val Context.dataStore by preferencesDataStore("preferences")
val Context.prefs: UserPreferencesRepository
    get() = UserPreferencesRepository(dataStore)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applicationContext.preferencesDataStoreFile("preferences")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val navController = rememberNavController()
            val initialPrefs = prefs.getPreferences.collectAsState(Prefs.DEFAULT).value
            CompositionLocalProvider(LocalPreferences provides initialPrefs) {
                Navigation(navController)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(navController: NavController) {
    var shouldShowSuggestions by remember { mutableStateOf(false) }
    val tagChipList = remember { mutableStateListOf<TagSuggestion>() }
    var searchString by remember { mutableStateOf("") }
    var lastValidSearchString by remember { mutableStateOf("") }
    var cleanedSearchString by remember { mutableStateOf("") }
    val mostRecentSuggestions = remember { mutableStateListOf<TagSuggestion>() }
    val scrollState = rememberScrollState()
    var forciblyAllowedAi by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val excludeAi = prefs.excludeAi
    val currentSource = prefs.imageSource


    fun addToFilter(tag: TagSuggestion) {
        val index = tagChipList.getIndexByName(tag.value)
        if (index != null) {
            // For some reason `tagChipList[index] = tag` doesn't update in the UI
            tagChipList.removeAt(index)
            tagChipList.add(index, tag)
            return
        } else tagChipList.add(tag)
    }


    fun filterTags(): List<TagSuggestion> {
        val suggestions = mutableListOf<TagSuggestion>()
        if (cleanedSearchString != "") {
            return currentSource.site.loadAutoComplete(cleanedSearchString)
        } else {
            shouldShowSuggestions = false
        }

        return suggestions.toList()
    }


    @Composable
    fun TagListEntry(tag: TagSuggestion) {
        Column(
            modifier = Modifier.clickable {
                searchString = ""
                cleanedSearchString = ""
                lastValidSearchString = ""
                shouldShowSuggestions = false
                addToFilter(tag)
                mostRecentSuggestions.clear()
            }
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                text = tag.label
            )

            Text(
                modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                text = tag.type,
                fontSize = 12.sp
            )

            HorizontalDivider()
        }
    }



    @Composable
    fun AutoCompleteTagResults(suggestions: List<TagSuggestion>) {
        Column(
            modifier = Modifier
                .consumeWindowInsets(PaddingValues(0.dp, 0.dp, 0.dp, (NAV_BAR_HEIGHT + 16).dp))
                .imePadding()
        ) {
            Surface(
                modifier = Modifier
                    .padding(
                        top = 10.dp,
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 16.dp
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState()),
                tonalElevation = 4.dp
            ) {
                Spacer(Modifier.size(18.dp))

                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    if (suggestions.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "No results :("
                        )
                    } else {
                        for (t in suggestions) {
                            TagListEntry(tag = t)
                        }
                    }
                }
            }
        }
    }


    fun activateSearch() {
        if (tagChipList.isEmpty()) {
            Toast.makeText(
                context,
                "Please select some tags",
                Toast.LENGTH_SHORT
            ).show()
        }
        else {
            val searchTags = currentSource.site.formatTagString(tagChipList)
            navController.navigate("searchResults/${searchTags}")
        }
    }

    fun addAiExcludedTag() {
        if (excludeAi && !forciblyAllowedAi && tagChipList.getIndexByName(currentSource.site.aiTagName) == null) {
            val tag = TagSuggestion("", currentSource.site.aiTagName, "", true)
            tagChipList.add(0, tag)
        }
    }
    addAiExcludedTag()

    ProcrasturbatingTheme {
        val scope = rememberCoroutineScope()

        MainScreenScaffold("Procrasturbating") {
            Column(Modifier.padding(it)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f, true),
                        value = searchString,
                        onValueChange = {
                            searchString = it
                            cleanedSearchString = searchString
                                .trim()
                                .replace(" ", "_")
                        },
                        placeholder = { Text("Search Tags") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (searchString.isNotEmpty()) {
                                    if (mostRecentSuggestions.isNotEmpty()) {
                                        addToFilter(mostRecentSuggestions[0])
                                        searchString = ""
                                        lastValidSearchString = ""
                                        shouldShowSuggestions = false
                                    } else {
                                        if (mostRecentSuggestions.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "No matching tags",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    activateSearch()
                                }
                            }
                        ),
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    FloatingActionButton(
                        onClick = { activateSearch() },
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.Search,
                            contentDescription = "Search"
                        )
                    }
                }

                Spacer(Modifier.size(8.dp))

                Box {
                    Column {
                        AnimatedVisibility(tagChipList.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(scrollState)
                            ) {
                                Spacer(Modifier.size(16.dp))

                                for (t in tagChipList) {
                                    FilterChip(
                                        label = { Text(t.value) },
                                        selected = !t.isExcluded,
                                        onClick = {
                                            if (t.value == "ai_generated") {
                                                if (t.isExcluded) forciblyAllowedAi = true
                                                else addAiExcludedTag()
                                            }
                                            tagChipList.remove(t)
                                        }
                                    )

                                    Spacer(modifier = Modifier.size(8.dp))
                                }

                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }
                        AnimatedVisibility(shouldShowSuggestions) {
                            AutoCompleteTagResults(mostRecentSuggestions)
                        }
                    }
                }

                // I don't like this
                LaunchedEffect(key1 = cleanedSearchString) {
                    if (cleanedSearchString.isNotEmpty()) { delay(200) }
                    if (lastValidSearchString != cleanedSearchString) {
                        lastValidSearchString = cleanedSearchString.trim().replace(" ", "_")

                        if (cleanedSearchString !in arrayListOf("", "-")) {
                            val deferredResponse = scope.async {
                                return@async { filterTags() }
                            }
                            try {
                                val response = deferredResponse.await().invoke()
                                // I don't have a better way of ensuring the right state
                                // without risking noticeable UI lag yet
                                if (cleanedSearchString == lastValidSearchString) {
                                    mostRecentSuggestions.clear()
                                    mostRecentSuggestions.addAll(response)
                                    shouldShowSuggestions = true
                                }
                            } catch (e: Exception) {
                                if (e.message?.contains("was cancelled") != true) {
                                    Toast.makeText(
                                        context,
                                        "Error fetching results",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.e("App", "Error fetching autocomplete results", e)
                                }
                            }
                        } else {
                            shouldShowSuggestions = false
                            mostRecentSuggestions.clear()
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun Navigation(navController: NavHostController) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val slideDistance = rememberSlideDistance()
    val bottomBarVisibleState = remember { mutableStateOf(true) }
    val currentBSE by navController.currentBackStackEntryAsState()
    val currentRoute = currentBSE?.destination?.route

    ProcrasturbatingTheme {
        Surface {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = listOf("home", "settings", "favourite_images").contains(currentRoute)
                            && bottomBarVisibleState.value,
                        enter = expandVertically { 0 },
                        exit = shrinkVertically { 0 }
                    ) {
                        NavigationBar {
                            NavigationBarItem(
                                label = { Text("Home") },
                                selected = currentRoute == "home" || "searchResults" in currentRoute!!,
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute != "home" || "searchResults" in currentRoute) Icons.Outlined.Home
                                                      else Icons.Filled.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                onClick = {
                                    if (currentRoute != "home") {
                                        navController.navigate(
                                            "home"
                                        )
                                    }
                                }
                            )
                            NavigationBarItem(
                                label = { Text("Favourites") },
                                selected = currentRoute == "favourite_images",
                                icon = {
                                    Icon(
                                        painter = painterResource(
                                            if (currentRoute != "favourite_images") R.drawable.ic_star_hollow
                                            else R.drawable.ic_star_filled
                                        ),
                                        contentDescription = "Favourite images"
                                    )
                                },
                                onClick = {
                                    if (currentRoute != "favourite_images") {
                                        navController.navigate("favourite_images")
                                    }
                                }
                            )
                            NavigationBarItem(
                                label = { Text("Settings") },
                                selected = currentRoute == "settings",
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute != "settings") Icons.Outlined.Settings
                                        else Icons.Filled.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                onClick = {
                                    if (currentRoute != "settings") {
                                        navController.navigate("settings")
                                    }
                                }
                            )
                        }
                    }
                }
            ) {
                NavHost(
                    modifier = Modifier.padding(it.withoutVertical()),
                    navController = navController,
                    startDestination = "home",
                    enterTransition = {
                        if (targetState.destination.route?.contains("searchResults") == true)
                            materialSharedAxisXIn(!isRtl, slideDistance)
                        else fadeIn()
                    },
                    exitTransition = {
                        if (targetState.destination.route?.contains("searchResults") == true)
                            materialSharedAxisXOut(!isRtl, slideDistance)
                        else fadeOut()
                    },
                    popEnterTransition = {
                        if (initialState.destination.route?.contains("searchResults") == true)
                            materialSharedAxisXIn(isRtl, slideDistance)
                        else fadeIn()
                    },
                    popExitTransition = {
                        if (initialState.destination.route?.contains("searchResults") == true)
                            materialSharedAxisXOut(isRtl, slideDistance)
                        else fadeOut()
                    }
                ) {
                    composable("home") { HomeScreen(navController) }
                    composable(
                        route = "searchResults/{searchQuery}",
                        arguments = listOf(navArgument("searchQuery") { NavType.StringType })
                    ) { navBackStackEntry ->
                        SearchResults(
                            navController,
                            navBackStackEntry.arguments?.getString("searchQuery") ?: ""
                        )
                    }
                    composable("settings") { PreferencesScreen() }
                    composable("favourite_images") { FavouritesPage(bottomBarVisibleState) }
                }
            }
        }
    }
}


private fun SnapshotStateList<TagSuggestion>.getIndexByName(name: String): Int? {
    this.forEachIndexed { index, tag ->
        if (tag.value == name) return index
    }
    return null
}
