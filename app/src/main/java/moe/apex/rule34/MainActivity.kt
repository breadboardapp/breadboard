@file:OptIn(ExperimentalMaterial3Api::class)

package moe.apex.rule34

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.core.util.Consumer
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import moe.apex.rule34.navigation.Favourites
import moe.apex.rule34.navigation.Home
import moe.apex.rule34.navigation.Navigation
import moe.apex.rule34.navigation.Results
import moe.apex.rule34.navigation.Search
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.StartDestination
import moe.apex.rule34.preferences.UserPreferencesRepository
import moe.apex.rule34.viewmodel.BreadboardViewModel


val Context.dataStore by preferencesDataStore("preferences")
val Context.prefs: UserPreferencesRepository
    get() = UserPreferencesRepository(dataStore)


class MainActivity : SingletonImageLoader.Factory, ComponentActivity() {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }


    private fun maybePrepareResultsDestination(intent: Intent): Results? {
        val searchSource = ImageSource.valueOf(intent.getStringExtra("source") ?: return null)
        val searchQuery = intent.getStringArrayExtra("query") ?: return null
        return Results(searchSource, searchQuery.toList())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        applicationContext.preferencesDataStoreFile("preferences")
        runBlocking { prefs.handleMigration(applicationContext) }
        val initialPrefs = runBlocking { prefs.getPreferences.first() }

        Log.i("intent", intent.extras?.keySet()?.toSet().toString())

        val startDestination = when (initialPrefs.defaultStartDestination) {
            StartDestination.HOME -> Home
            StartDestination.SEARCH -> Search
            StartDestination.FAVOURITES -> Favourites
        }

        setContent {
            val navController = rememberNavController()
            val prefs = prefs.getPreferences.collectAsState(initialPrefs).value
            val viewModel = viewModel(BreadboardViewModel::class.java)

            LaunchedEffect(prefs.imageSource, prefs.imageBoardAuths, prefs.filterRatingsLocally, prefs.blockedTags, prefs.recommendAllRatings) {
                if (
                    viewModel.recommendationsProvider?.imageSource != prefs.imageSource ||
                    viewModel.recommendationsProvider?.auth != prefs.authFor(prefs.imageSource) ||
                    viewModel.recommendationsProvider?.filterRatingsLocally != prefs.filterRatingsLocally ||
                    viewModel.recommendationsProvider?.blockedTags != prefs.blockedTags ||
                    viewModel.recommendationsProvider?.showAllRatings != prefs.recommendAllRatings
                ) {
                    viewModel.recommendationsProvider = null
                }
            }

            CompositionLocalProvider(LocalPreferences provides prefs) {
                /* When searching for a tag from the info sheet of a deep linked image, we want it
                   to be done inside of this activity rather than the DeepLinkActivity. */
                DisposableEffect(Unit) {
                    val innerListener = Consumer<Intent> { intent ->
                        maybePrepareResultsDestination(intent)?.let {
                            navController.navigate(it)
                        }
                    }
                    addOnNewIntentListener(innerListener)
                    onDispose { removeOnNewIntentListener(innerListener) }
                }
                Navigation(navController, viewModel, maybePrepareResultsDestination(intent) ?: startDestination)
            }
        }
    }
}
