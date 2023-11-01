@file:OptIn(ExperimentalMaterial3Api::class)

package moe.apex.rule34

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import moe.apex.rule34.detailview.SearchResults
import moe.apex.rule34.preferences.PreferencesScreen
import moe.apex.rule34.preferences.UserPreferencesRepository
import moe.apex.rule34.tag.TagSuggestion
import moe.apex.rule34.ui.theme.ProcrasturbatingTheme
import org.json.JSONArray
import soup.compose.material.motion.animation.materialSharedAxisXIn
import soup.compose.material.motion.animation.materialSharedAxisXOut
import soup.compose.material.motion.animation.rememberSlideDistance


val Context.dataStore by preferencesDataStore("preferences")
val Context.prefs: UserPreferencesRepository
    get() = UserPreferencesRepository(dataStore)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationContext.preferencesDataStoreFile("preferences")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val navController = rememberNavController()
            Navigation(navController)
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
    val interactionSource = MutableInteractionSource()
    val context = LocalContext.current


    fun addToFilter(tag: TagSuggestion) {
        if (!tagChipList.contains(tag)) {
            tagChipList.add(tag)
        }
    }


    fun filterTags(): List<TagSuggestion> {
        val suggestions = mutableListOf<TagSuggestion>()
        if (cleanedSearchString != "") {
            val isExcluded = cleanedSearchString.startsWith("-")
            val query = cleanedSearchString
                .replace("^-".toRegex(), "")
            val body = RequestUtil.get("https://rule34.xxx/public/autocomplete.php?q=$query").get()
            val results = JSONArray(body)
            val resultCount = results.length()

            for (i in 0 until resultCount) {
                val suggestion = results.getJSONObject(i)
                val label = suggestion.getString("label")
                val value = suggestion.getString("value")
                val type = suggestion.getString("type")
                suggestions.add(TagSuggestion(label, value, type, isExcluded))
            }
        } else {
            shouldShowSuggestions = false
        }

        return suggestions.toList()
    }


    @Composable
    fun TagListEntry(tag: TagSuggestion) {
        Column(
            Modifier.clickable(
                onClick = {
                    searchString = ""
                    cleanedSearchString = ""
                    lastValidSearchString = ""
                    shouldShowSuggestions = false
                    addToFilter(tag)
                    mostRecentSuggestions.clear()
                }
            )
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

            Divider()
        }
    }


    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun AutoCompleteTagResults(suggestions: List<TagSuggestion>) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
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
                    .verticalScroll(rememberScrollState())
                    .imeNestedScroll(),
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
            val searchTags = tagChipList.joinToString("+") { it.formattedLabel }
            navController.navigate("searchResults/${searchTags}")
        }
    }


    ProcrasturbatingTheme {
        val scope = rememberCoroutineScope()

        // I'd like to move the top app bar to the topBar param in Scaffold but the padding gets
        // messed up and I'd rather keep everything as is because it looks good.
        Scaffold(Modifier.fillMaxSize()) {
            Column {
                LargeTopAppBar(
                    title = { Text("Procrasturbating") },
                    actions = {
                        IconButton(
                            onClick = {
                                navController.navigate("settings")
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    }
                )

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
                                    }
                                    else {
                                        if (mostRecentSuggestions.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "No matching tags",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                else { activateSearch() }
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
                                        interactionSource = interactionSource,
                                        label = { Text(t.formattedLabel) },
                                        selected = !t.isExcluded,
                                        onClick = { tagChipList.remove(t) }
                                    )

                                    Spacer(modifier = Modifier.size(8.dp))
                                }

                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }
                        Column {
                            AnimatedVisibility(
                                shouldShowSuggestions,
                                //enter = expandVertically(),
                                //exit = shrinkVertically()
                            ) {
                                AutoCompleteTagResults(mostRecentSuggestions)
                            }
                        }
                    }
                }

                // I don't like this
                LaunchedEffect(key1 = cleanedSearchString) {
                    println(lastValidSearchString)
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
                                    Log.e("App", "fucked mate", e)
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
    ProcrasturbatingTheme {
        Surface {
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = { materialSharedAxisXIn(!isRtl, slideDistance) },
                exitTransition = { materialSharedAxisXOut(!isRtl, slideDistance) },
                popEnterTransition = { materialSharedAxisXIn(isRtl, slideDistance) },
                popExitTransition = { materialSharedAxisXOut(isRtl, slideDistance) }
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
                composable("settings") { PreferencesScreen(navController)}
            }
        }
    }
}