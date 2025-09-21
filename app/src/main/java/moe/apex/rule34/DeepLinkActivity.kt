package moe.apex.rule34

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.core.util.Consumer
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
import moe.apex.rule34.navigation.ImageView
import moe.apex.rule34.navigation.Navigation
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.viewmodel.BreadboardViewModel
import moe.apex.rule34.util.FlagSecureHelper
import moe.apex.rule34.util.createViewIntent
import moe.apex.rule34.util.getDefaultPackageForIntent
import moe.apex.rule34.util.launchInWebBrowser
import moe.apex.rule34.util.launchUriWithPackage


class DeepLinkActivity : SingletonImageLoader.Factory, ComponentActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        applicationContext.preferencesDataStoreFile("preferences")
        runBlocking { prefs.handleMigration(applicationContext) }
        val initialPrefs = runBlocking { prefs.getPreferences.first() }

        setContent {
            val navController = rememberNavController()
            val prefs = prefs.getPreferences.collectAsState(initialPrefs).value
            val viewModel = viewModel(BreadboardViewModel::class.java)

            CompositionLocalProvider(LocalPreferences provides prefs) {
                FlagSecureHelper.register()

                DisposableEffect(Unit) {
                    val listener = Consumer<Intent> { newIntent ->
                        val uri = newIntent.data
                        if (uri != null) {
                            ImageView.fromUri(uri)?.let {
                                navController.popBackStack()
                                navController.navigate(it)
                            } ?: reopenInBrowser(newIntent)
                        }
                    }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }
                intent.data?.let {
                    ImageView.fromUri(it)?.let { iv ->
                        Navigation(
                            navController = navController,
                            viewModel = viewModel,
                            startDestination = iv
                        )
                    } ?: reopenInBrowser(intent)
                } ?: finish()
            }
        }
    }


    private fun reopenInBrowser(intent: Intent) {
        val uri = intent.data!!

        // Not all apps set this but some like Chrome and Firefox do. We should use it if available.
        val possibleBrowserPackage = intent.getStringExtra(Browser.EXTRA_APPLICATION_ID)

        if (possibleBrowserPackage != null) {
            try {
                return launchUriWithPackage(this, uri, possibleBrowserPackage)
            } catch (_: ActivityNotFoundException) {
                /* Android System Intelligence (com.google.android.as) powers the "Open" action
                   for supported apps when long-pressing an Imageboard URL and sets the browser
                   intent extra to its own package name, but it can't handle the links itself. */
                Log.i("openInBrowser", "Original browser package $possibleBrowserPackage is not capable of launching URI $uri")
            }
        }

        /* Chrome sets the referrer to the address. Firefox uses its package name with this scheme.
           If the referrer scheme is an android-app and the app can handle the URL,
           we should use it to do so. */
        referrer?.takeIf { it.scheme == "android-app" }?.host?.let { attemptingPackage ->
            // If the referrer is Breadboard itself but we already know Breadboard can't handle the link in-app, we shouldn't try to do so.
            if (attemptingPackage == BuildConfig.APPLICATION_ID) {
                Log.w("openInBrowser", "Intent came from Breadboard itself but Breadboard can't handle URI $uri. If the intention was to open in the browser, call launchInWebBrowser() instead.")
                return@let
            }
            val relaunchIntent = createViewIntent(uri, attemptingPackage)
            if (getDefaultPackageForIntent(packageManager, relaunchIntent) != null) {
                startActivity(relaunchIntent)
                finishAndRemoveTask()
                return
            }
        }

        // If all else fails, just open in the default browser.
        launchInWebBrowser(this, uri)
        finishAndRemoveTask()
    }
}
