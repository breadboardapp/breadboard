package moe.apex.rule34.largeimageview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import moe.apex.rule34.R
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.DataSaver
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.util.FullscreenLoadingSpinner
import moe.apex.rule34.util.MustSetLocation
import moe.apex.rule34.util.NAV_BAR_HEIGHT
import moe.apex.rule34.util.SaveDirectorySelection
import moe.apex.rule34.util.downloadImage



private fun isUsingWiFi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(networkInfo)

    return networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LargeImageView(
    initialPage: Int,
    visible: MutableState<Boolean>,
    allImages: List<Image>,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f
    ) { allImages.size }
    var canChangePage by remember { mutableStateOf(false) }
    val zoomState = rememberZoomableState(ZoomSpec(maxZoomFactor = 3.5f))
    var forciblyShowBottomBar by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(0.dp) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isUsingWifi = isUsingWiFi(context)
    val storageLocationPromptLaunched = remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    if (allImages.isEmpty()) {
        visible.value = false
        return
    }

    runBlocking {
        if (pagerState.currentPage >= allImages.size)
            pagerState.scrollToPage(allImages.size - 1)
    }

    val currentImage = allImages[pagerState.currentPage]

    val prefs = LocalPreferences.current
    val dataSaver = prefs.dataSaver
    val storageLocation = prefs.storageLocation
    val favouriteImages = prefs.favouriteImages

    // Large image view is an overlay rather than a new screen entirely so we need to override
    // the default back button behaviour so we don't get taken to the home page.
    BackHandler(visible.value) {
        visible.value = false
    }

    PredictiveBackHandler(visible.value) { progress ->
        try {
            progress.collect { backEvent ->
                offset = (backEvent.progress * 200).dp
            }
            visible.value = false
        }
        catch(_: Exception) { }
    }

    @Composable
    fun LargeImage(imageUrl: String) {
        val modifier =
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier.fillMaxWidth()
            } else { Modifier.fillMaxHeight() }
        /*
        This is not foolproof. On smaller displays or aspect ratios, this might cause corners
        to appear wrongly clipped on very long or very wide images. We can use the
        RoundedCornerTransformation to clip the image itself rather than the composable view
        of it, but that bases corner radius on the dimensions of the image rather than the
        display and arguably looks worse.
        Suggestions welcome.
        */

        /* These need to be remembered otherwise recompositions (like when zooming) will cause
           them to flash. */
        val loader = remember {
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }
        val model = remember {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        }
        SubcomposeAsyncImage(
            model = model,
            imageLoader = loader,
            contentDescription = "Image",
            loading = { FullscreenLoadingSpinner() },
            modifier = modifier
                .scale(0.95f)
                .clip(RoundedCornerShape(24.dp))
        )
    }

    BreadboardTheme {
        if (storageLocationPromptLaunched.value) {
            SaveDirectorySelection(storageLocationPromptLaunched)
        }

        Scaffold(
            modifier = Modifier.offset { IntOffset(y = offset.roundToPx(), x = 0) },
            bottomBar = {
                AnimatedVisibility(
                    visible = ((zoomState.zoomFraction ?: 0f) < 0.15f) || forciblyShowBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    BottomAppBar(
                        actions = {
                            IconButton(
                                onClick = { currentImage.toggleHd() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                val vectorIcon = if (currentImage.preferHd) {
                                    R.drawable.ic_hd_enabled
                                } else {
                                    R.drawable.ic_hd_disabled
                                }
                                Icon(
                                    ImageVector.vectorResource(id = vectorIcon),
                                    "Toggle HD",
                                    modifier = Modifier.scale(1.2F)
                                )
                            }
                            if (currentImage in favouriteImages) {
                                IconButton(onClick = {
                                    scope.launch {
                                        context.prefs.removeFavouriteImage(currentImage)
                                        Toast.makeText(context, "Removed from your favourites", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_filled),
                                        contentDescription = "Remove from favourites"
                                    )
                                }
                            } else {
                                IconButton(onClick = {
                                    scope.launch {
                                        context.prefs.addFavouriteImage(currentImage)
                                        Toast.makeText(context, "Added to your favourites", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_star_hollow),
                                        contentDescription = "Add to favourites"
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, currentImage.highestQualityFormatUrl)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    "Share"
                                )
                            }
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    if (!isDownloading) {
                                        scope.launch {
                                            isDownloading = true
                                            val result: Result<Boolean> = downloadImage(
                                                context,
                                                currentImage,
                                                storageLocation
                                            )

                                            if (result.isSuccess) {
                                                Toast.makeText(
                                                    context,
                                                    "Image saved.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val exc = result.exceptionOrNull()!!
                                                exc.printStackTrace()

                                                if (exc is MustSetLocation) {
                                                    storageLocationPromptLaunched.value = true
                                                }
                                                Toast.makeText(
                                                    context,
                                                    exc.message,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            isDownloading = false
                                        }
                                    }
                                },
                                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(Modifier.scale(0.5F))
                                }
                                else {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_download),
                                        contentDescription = "Save"
                                    )
                                }
                            }
                        }
                    )
                }
            }
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = canChangePage,
                beyondViewportPageCount = 1
            ) {index ->
                val currentImg = allImages[index]

                if (currentImg.hdQualityOverride == null) {
                    when (dataSaver) {
                        DataSaver.ON -> currentImg.preferHd = false
                        DataSaver.OFF -> currentImg.preferHd = true
                        DataSaver.AUTO -> currentImg.preferHd = isUsingWifi
                    }
                }

                Box(Modifier.zoomable(
                    zoomState,
                    onClick = {
                        forciblyShowBottomBar = !forciblyShowBottomBar
                    }
                )) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(bottom = NAV_BAR_HEIGHT.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentImg.preferHd) {
                            LargeImage(imageUrl = currentImg.highestQualityFormatUrl)
                        } else {
                            LargeImage(imageUrl = currentImg.sampleUrl)
                        }
                    }
                }
            }

            val isZoomedOut = (zoomState.zoomFraction ?: 0f) < 0.15f
            // Disable page changing while zoomed in and reset bottom bar state
            LaunchedEffect(isZoomedOut) {
                canChangePage = isZoomedOut
                forciblyShowBottomBar = false
            }
        }
    }
}
