package moe.apex.rule34.largeimageview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
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
import moe.apex.rule34.util.showToast
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
    val popupVisibilityState = remember { mutableStateOf(false) }

    val prefs = LocalPreferences.current
    val dataSaver = prefs.dataSaver
    val storageLocation = prefs.storageLocation
    val favouriteImages = prefs.favouriteImages

    if (popupVisibilityState.value) {
        InfoSheet(currentImage, popupVisibilityState)
    }

    LaunchedEffect(visible.value) {
        if (visible.value) offset = 0.dp
    }

    PredictiveBackHandler(visible.value) { progress ->
        try {
            progress.collect { backEvent ->
                offset = (backEvent.progress * 300).dp
            }
            visible.value = false
        }
        catch(_: Exception) { }
    }

    @Composable
    fun LargeImage(imageUrl: String, previewImageUrl: String, aspectRatio: Float?) {
        /* Poor method of the preliminary work to get rounded corners for favourites saved before
           we started saving the aspect ratio. */
        val modifier = if (aspectRatio == null) {
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxHeight()
            }
        } else Modifier.aspectRatio(aspectRatio)

        val model =
            ImageRequest.Builder(context)
                .data(imageUrl)
                .build()

        SubcomposeAsyncImage(
            model = model,
            contentDescription = "Image",
            loading = { Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = previewImageUrl,
                    contentDescription = "Image",
                    contentScale = ContentScale.Fit,
                    modifier = modifier.clip(RoundedCornerShape(24.dp))
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    FullscreenLoadingSpinner()
                }
            } },
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
                                    painter = painterResource(vectorIcon),
                                    contentDescription = "Toggle HD",
                                    modifier = Modifier.scale(1.2F)
                                )
                            }
                            if (favouriteImages.any { it.fileName == currentImage.fileName && it.imageSource == currentImage.imageSource }) {
                                IconButton(onClick = {
                                    scope.launch {
                                        context.prefs.removeFavouriteImage(currentImage)
                                        showToast(context, "Removed from your favourites")
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_star_filled),
                                        contentDescription = "Remove from favourites"
                                    )
                                }
                            } else {
                                IconButton(onClick = {
                                    scope.launch {
                                        context.prefs.addFavouriteImage(currentImage)
                                        showToast(context, "Added to your favourites")
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_star_hollow),
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
                            if (currentImage.metadata != null) {
                                IconButton(
                                    onClick = { popupVisibilityState.value = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Info"
                                    )
                                }
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
                                                showToast(context, "Image saved.")
                                            } else {
                                                val exc = result.exceptionOrNull()!!
                                                exc.printStackTrace()

                                                if (exc is MustSetLocation) {
                                                    storageLocationPromptLaunched.value = true
                                                }
                                                showToast(context, exc.message ?: "Unknown error")
                                                Log.e("Downloader", exc.message ?: "Error downloading image", exc)
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
                                        painter = painterResource(R.drawable.ic_download),
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
            ) { index ->
                val imageAtIndex = allImages[index]

                if (imageAtIndex.hdQualityOverride == null) {
                    when (dataSaver) {
                        DataSaver.ON -> imageAtIndex.preferHd = false
                        DataSaver.OFF -> imageAtIndex.preferHd = true
                        DataSaver.AUTO -> imageAtIndex.preferHd = isUsingWifi
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
                        LargeImage(
                            imageUrl = if (imageAtIndex.preferHd) imageAtIndex.highestQualityFormatUrl
                                       else imageAtIndex.sampleUrl,
                            previewImageUrl = imageAtIndex.previewUrl,
                            aspectRatio = imageAtIndex.aspectRatio
                        )
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
