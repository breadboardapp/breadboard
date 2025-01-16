package moe.apex.rule34

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.datastore.preferences.preferencesDataStoreFile
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import moe.apex.rule34.largeimageview.LargeImageView
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.ui.theme.Typography


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
            val prefs = prefs.getPreferences.collectAsState(initialPrefs).value
            var uri by rememberSaveable { mutableStateOf(intent.data) }
            CompositionLocalProvider(LocalPreferences provides prefs) {
                DisposableEffect(Unit) {
                    val listener = Consumer<Intent> { newIntent -> uri = newIntent.data }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }
                BreadboardTheme {
                    Surface {
                        DeepLinkLargeImageView(uri)
                    }
                }
            }
        }
    }
}


@Composable
fun DeepLinkLargeImageView(uri: Uri?) {
    val image = uri?.let { ImageSource.loadImageFromUri(it) }
    if (image == null) return ImageNotFound()

    LargeImageView(
        initialPage = 0,
        allImages = listOf(image)
    )
}


@Composable
fun ImageNotFound() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Image not found :(",
            style = Typography.titleLarge
        )
    }
}