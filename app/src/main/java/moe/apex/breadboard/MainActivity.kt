package moe.apex.breadboard

import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.util.Consumer
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import moe.apex.breadboard.navigation.ArtistProfile
import moe.apex.breadboard.navigation.Favourites
import moe.apex.breadboard.navigation.Home
import moe.apex.breadboard.navigation.Navigation
import moe.apex.breadboard.navigation.Results
import moe.apex.breadboard.navigation.ReverseSearch
import moe.apex.breadboard.navigation.Search
import moe.apex.breadboard.preferences.DarkTheme
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.preferences.StartDestination
import moe.apex.breadboard.preferences.UserPreferencesRepository
import moe.apex.breadboard.util.FlagSecureHelper
import moe.apex.breadboard.viewmodel.getGlobalViewModel


val Context.dataStore by preferencesDataStore("preferences")
val Context.prefs: UserPreferencesRepository
    get() = UserPreferencesRepository(dataStore)


class MainActivity : SingletonImageLoader.Factory, ComponentActivity(), VolumeButtonHandler {
    override var volumeUpPressedCallback: (() -> Boolean)? = null

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


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (handleVolumeUpKey(keyCode, event)) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }


    private fun maybePrepareArtistDestination(intent: Intent): ArtistProfile? {
        val artist = intent.getStringExtra("artist") ?: return null
        val source = ImageSource.valueOf(intent.getStringExtra("origin_source") ?: return null)
        return ArtistProfile(artist, source)
    }


    private fun maybePrepareResultsDestination(intent: Intent): Results? {
        val searchSource = ImageSource.valueOf(intent.getStringExtra("source") ?: return null)
        val searchQuery = intent.getStringArrayExtra("query") ?: return null
        return Results(searchSource, searchQuery.toList())
    }


    private fun maybePrepareReverseSearchDestination(intent: Intent): ReverseSearch? {
        val initialImageUrl = intent.getStringExtra("initial_image_url")
        val initialFileUri = intent.getStringExtra("initial_file_uri")
        return ReverseSearch(initialImageUrl, initialFileUri)
    }


    private fun determineDestination(intent: Intent): Any? {
        val newIntent = createReverseSearchIntent(intent) ?: intent

        return when (newIntent.getStringExtra("destination")) {
            "artist" -> maybePrepareArtistDestination(newIntent)
            "search" -> maybePrepareResultsDestination(newIntent)
            "reverse_search" -> maybePrepareReverseSearchDestination(newIntent)
            else -> null
        }
    }


    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Having this enabled seems to result in some items becoming invisible when animating lazy lists
        ComposeFoundationFlags.isSkipItemPlacementAnimationFixEnabled = false
        // https://issuetracker.google.com/issues/521534697 TODO
        ComposeMaterial3Flags.isBottomSheetPartiallyExpandedDeterministicEnabled = false

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
            val prefs by prefs.getPreferences.collectAsState(initialPrefs)
            val viewModel = getGlobalViewModel()
            val recommendationsProvider by viewModel.recommendationsProvider.collectAsState()

            /* Sync the UiModeManager's app night mode preference with the selected app dark theme
               preference. This ensures that the splash screen colour scheme matches the selected app
               dark theme preference.

               UiModeManager#setApplicationNightMode() is only supported on Android 12+. */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager

                SideEffect(prefs.darkTheme) {
                    uiModeManager.setApplicationNightMode(
                        when (prefs.darkTheme) {
                            DarkTheme.ON -> UiModeManager.MODE_NIGHT_YES
                            DarkTheme.OFF -> UiModeManager.MODE_NIGHT_NO
                            DarkTheme.AUTO -> UiModeManager.MODE_NIGHT_AUTO
                        }
                    )
                }
            }

            SideEffect(prefs.imageSource, prefs.filterRatingsLocally) {
                if (
                    recommendationsProvider?.imageSource != prefs.imageSource ||
                    recommendationsProvider?.filterRatingsLocally != prefs.filterRatingsLocally
                ) {
                    viewModel.setRecommendationsProvider(null)
                }
            }

            CompositionLocalProvider(LocalPreferences provides prefs) {
                /* When searching for a tag from the info sheet of a deep linked image, we want it
                   to be done inside of this activity rather than the DeepLinkActivity. */
                DisposableEffect(Unit) {
                    val innerListener = Consumer<Intent> { intent ->
                        determineDestination(intent)?.let {
                            navController.navigate(it)
                        }
                    }
                    addOnNewIntentListener(innerListener)
                    onDispose { removeOnNewIntentListener(innerListener) }
                }
                FlagSecureHelper.register()
                Navigation(navController, determineDestination(intent) ?: startDestination)
            }
        }
    }


    private fun createReverseSearchIntent(intent: Intent): Intent? {
        if (intent.action != Intent.ACTION_SEND) return null

        val initialImageUrl = intent.extras?.getString(Intent.EXTRA_TEXT)
        val initialFileUri = intent.clipData?.getItemAt(0)?.uri

        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.component = ComponentName(this, MainActivity::class.java)
        intent.putExtra("destination", "reverse_search")

        /* Sometimes a shared link may contain an image clipData of the favicon, so we must
           prioritize the text over the clipData. Providing them both would be bad. */
        if (initialImageUrl != null) {
            intent.putExtra("initial_image_url", initialImageUrl)
        } else if (initialFileUri != null) {
            intent.putExtra("initial_file_uri", initialFileUri.toString())
        }

        return intent
    }
}
