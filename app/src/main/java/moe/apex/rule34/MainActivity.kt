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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import moe.apex.rule34.detailview.SearchResults
import moe.apex.rule34.favourites.FavouritesPage
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.PreferencesScreen
import moe.apex.rule34.preferences.UserPreferencesRepository
import moe.apex.rule34.tag.TagSuggestion
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.ui.theme.searchField
import moe.apex.rule34.util.CHIP_SPACING
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.NAV_BAR_HEIGHT
import moe.apex.rule34.util.VerticalSpacer
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
        runBlocking { prefs.handleMigration(applicationContext) }
        val initialPrefs = runBlocking { prefs.getPreferences.first() }

        setContent {
            val navController = rememberNavController()
            val prefs = prefs.getPreferences.collectAsState(initialPrefs).value
            CompositionLocalProvider(LocalPreferences provides prefs) {
                Navigation(navController)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(navController: NavController, focusRequester: FocusRequester) {
    /* We use shouldShowSuggestions for determining autocomplete section visibility because if we
       used mostRecentSuggestions.isNotEmpty(), it would temporarily show the "No results" message
       while disappearing and that looks bad. */
    var shouldShowSuggestions by remember { mutableStateOf(false) }
    val tagChipList = remember { mutableStateListOf<TagSuggestion>() }
    var searchString by remember { mutableStateOf("") }
    var cleanedSearchString by remember { mutableStateOf("") }
    val mostRecentSuggestions = remember { mutableStateListOf<TagSuggestion>() }
    var forciblyAllowedAi by remember { mutableStateOf(false) }
    var showRatingFilter by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (showRatingFilter) 180f else 0f)

    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val excludeAi = prefs.excludeAi
    val currentSource = prefs.imageSource

    var searchJob: Job? = null
    val scope = rememberCoroutineScope()


    fun addToFilter(tag: TagSuggestion) {
        val index = tagChipList.getIndexByName(tag.value)
        if (index != null) {
            // For some reason `tagChipList[index] = tag` doesn't update in the UI
            tagChipList.removeAt(index)
            tagChipList.add(index, tag)
            return
        } else tagChipList.add(tag)
    }


    fun danbooruLimitCheck(): Boolean {
        if (tagChipList.size == 2 && prefs.imageSource == ImageSource.DANBOORU) {
            Toast.makeText(context, "Danbooru supports up to 2 tags", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


    fun getSuggestions() {
        searchJob?.cancel()
        searchJob = scope.launch(Dispatchers.IO) {
            if (cleanedSearchString.isNotEmpty()) delay(200)
            if (cleanedSearchString !in listOf("", "-")) {
                try {
                    val suggestions = currentSource.site.loadAutoComplete(cleanedSearchString)
                    /* This check shouldn't be needed but avoids a race condition whereby clearing
                       the query in the time between getting suggestions and displaying them will cause
                       the old suggestions to be displayed. */
                    if (cleanedSearchString.isEmpty()) return@launch
                    mostRecentSuggestions.clear()
                    mostRecentSuggestions.addAll(suggestions)
                    shouldShowSuggestions = true
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error fetching results", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("App", "Error fetching autocomplete results", e)
                }
            } else {
                shouldShowSuggestions = false
            }
        }
    }


    @Composable
    fun TagListEntry(tag: TagSuggestion) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .heightIn(min = 72.dp)
                .fillMaxWidth()
                .clickable {
                    if (!danbooruLimitCheck()) return@clickable
                    searchString = ""
                    cleanedSearchString = ""
                    shouldShowSuggestions = false
                    addToFilter(tag)
                }
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = tag.label,
                fontSize = 16.sp,
                lineHeight = 17.sp
            )

            tag.type?.let {
                Text(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    text = it,
                    fontSize = 12.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }


    @Composable
    fun AutoCompleteTagResults() {
        Column(
            modifier = Modifier
                .consumeWindowInsets(PaddingValues(0.dp, 0.dp, 0.dp, (NAV_BAR_HEIGHT + 16).dp))
                .imePadding()
        ) {
            Surface(
                modifier = Modifier
                    .padding(
                        top = 12.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                    .clip(MaterialTheme.shapes.large)
                    .verticalScroll(rememberScrollState()),
                tonalElevation = 4.dp
            ) {
                Spacer(Modifier.size(18.dp))

                Column(
                    Modifier
                        .clip(MaterialTheme.shapes.large)
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    if (mostRecentSuggestions.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(20.dp),
                            fontSize = 16.sp,
                            text = "No results :("
                        )
                    } else {
                        for (t in mostRecentSuggestions) {
                            TagListEntry(tag = t)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }


    fun performSearch() {
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

    fun beginSearch() {
        if (searchString.isNotEmpty()) {
            if (mostRecentSuggestions.isNotEmpty()) {
                if (!danbooruLimitCheck()) return
                addToFilter(mostRecentSuggestions[0])
                searchString = ""
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
            performSearch()
        }
    }

    BreadboardTheme {
        MainScreenScaffold("Breadboard") {
            Column(Modifier.padding(it)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    TextField(
                        modifier = Modifier
                            .weight(1f, true)
                            .focusRequester(focusRequester),
                        value = searchString,
                        textStyle = MaterialTheme.typography.searchField,
                        onValueChange = {
                            searchString = it
                            cleanedSearchString = searchString
                                .trim()
                                .replace(" ", "_")
                            getSuggestions()
                        },
                        placeholder = {
                            Text(
                                text = "Search Tags",
                                style = MaterialTheme.typography.searchField
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Search
                            // Maybe also look into https://issuetracker.google.com/issues/359257538
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { beginSearch() },
                            onSearch = { beginSearch() }
                        ),
                        colors = TextFieldDefaults.colors().copy(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                8.dp
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                8.dp
                            )
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Filter",
                                modifier = Modifier
                                    .rotate(chevronRotation)
                                    .clickable {
                                        showRatingFilter = !showRatingFilter
                                    }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.size(10.dp))

                    FloatingActionButton(
                        onClick = { performSearch() },
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.Search,
                            contentDescription = "Search"
                        )
                    }
                }

                VerticalSpacer()

                AnimatedVisibility(showRatingFilter) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Ratings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        VerticalDivider(Modifier.height(FilterChipDefaults.Height))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.width(14.dp))
                            for (rating in ImageRating.entries.filter { it != ImageRating.UNKNOWN }) {
                                FilterChip(
                                    selected = rating in prefs.ratingsFilter,
                                    label = { Text(rating.label) },
                                    onClick = {
                                        scope.launch {
                                            if (rating in prefs.ratingsFilter) context.prefs.removeRatingFilter(
                                                rating
                                            )
                                            else context.prefs.addRatingFilter(rating)
                                        }
                                    }
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }

                AnimatedVisibility(tagChipList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp)
                    ) {
                        Spacer(Modifier.size(8.dp))

                        for (t in tagChipList) {
                            FilterChip(
                                label = { Text(t.value) },
                                selected = !t.isExcluded,
                                onClick = {
                                    if (t.value == prefs.imageSource.site.aiTagName) {
                                        if (t.isExcluded) forciblyAllowedAi = true
                                        else addAiExcludedTag()
                                    }
                                    tagChipList.remove(t)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
                AnimatedVisibility(shouldShowSuggestions) {
                    AutoCompleteTagResults()
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
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    BreadboardTheme {
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
                                label = { Text("Search") },
                                selected = currentRoute == "home" || "searchResults" in currentRoute!!,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Search, // Outlined and Filled search are the same
                                        contentDescription = "Search"
                                    )
                                },
                                onClick = {
                                    if (currentRoute != "home") {
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
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
                                        navController.navigate("favourite_images") {
                                            popUpTo("favourite_images") { inclusive = true }
                                        }
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
                                        navController.navigate("settings") {
                                            popUpTo("settings") { inclusive = true }
                                        }
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
                    composable("home") { HomeScreen(navController, focusRequester) }
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
