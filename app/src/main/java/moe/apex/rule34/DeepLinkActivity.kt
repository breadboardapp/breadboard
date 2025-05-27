package moe.apex.rule34

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.core.net.toUri


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
                DisposableEffect(Unit) {
                    val listener = Consumer<Intent> { newIntent ->
                        val uri = newIntent.data
                        if (uri != null) {
                            ImageView.fromUri(uri)?.let {
                                navController.popBackStack()
                                navController.navigate(it)
                            } ?: openInBrowser(newIntent)
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
                    } ?: openInBrowser(intent)
                } ?: finish()
            }
        }
    }


    private fun openInBrowser(intent: Intent) {
        val uri = intent.data!!

        // Chrome and Firefox seem to set this. We should use it if available.
        val possibleBrowserPackage = intent.getStringExtra("com.android.browser.application_id")

        if (possibleBrowserPackage != null) {
            launchUriWithPackage(uri, possibleBrowserPackage)
            return
        }

        /* Chrome sets the referrer to the address. Firefox uses its package name with this scheme.
           If the referrer scheme is an android-app and the app can handle the URL,
           we should use it to do so. */
        referrer?.takeIf { it.scheme == "android-app" }?.host?.let { attemptingPackage ->
            val relaunchIntent = createViewIntent(uri, attemptingPackage)
            if (getResolveInfo(relaunchIntent) != null) {
                startActivity(relaunchIntent)
                finishAndRemoveTask()
                return
            }
        }

        // If all else fails, just open in the default browser.
        val defaultBrowserIntent = Intent(Intent.ACTION_VIEW, "http://example.com".toUri())
        val resolveInfo = getResolveInfo(defaultBrowserIntent)

        if (resolveInfo?.activityInfo?.packageName != null) {
            launchUriWithPackage(uri, resolveInfo.activityInfo.packageName)
        } else {
            Log.e("openInBrowser", "No browser found to handle URI: $uri")
        }
        finishAndRemoveTask()
    }

    private fun createViewIntent(uri: Uri, targetPackage: String? = null): Intent {
        return Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            targetPackage?.let { setPackage(it) }
        }
    }

    private fun launchUriWithPackage(uri: Uri, packageName: String) {
        val launchIntent = createViewIntent(uri, packageName)
        startActivity(launchIntent)
        finishAndRemoveTask()
    }

    private fun getResolveInfo(intent: Intent): ResolveInfo? {
        return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
}
