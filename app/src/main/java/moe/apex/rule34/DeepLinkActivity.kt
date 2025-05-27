package moe.apex.rule34

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
                            } ?: openInBrowser(uri)
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
                    } ?: openInBrowser(it)
                } ?: finish()
            }
        }
    }


    private fun openInBrowser(uri: Uri) {
        /* Breadboard can't handle all types of links (for example search pages).
           We'll open these ones in the browser instead. */
        val browserIntent = Intent(Intent.ACTION_VIEW, "http://example.com".toUri())
        val resolveInfo = packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null) {
            val realIntent = Intent(Intent.ACTION_VIEW, uri)
            realIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            realIntent.setPackage(resolveInfo.activityInfo.packageName)
            startActivity(realIntent)
        }
        finishAndRemoveTask()
    }
}
