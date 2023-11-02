package moe.apex.rule34.largeimageview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import moe.apex.rule34.R
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.DataSaver
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.ProcrasturbatingTheme
import moe.apex.rule34.util.MustSetLocation
import moe.apex.rule34.util.SaveDirectorySelection
import moe.apex.rule34.util.downloadImage



private fun isUsingWiFi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(networkInfo)

    return networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}


@Composable
fun FullscreenLoadingSpinner() {
    Row(
        Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LargeImageView(
    navController: NavController,
    initialPage: MutableIntState,
    shouldShowLargeImage: MutableState<Boolean>,
    allImages: SnapshotStateList<Image>
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.intValue,
        initialPageOffsetFraction = 0f
    ) { allImages.size }
    var canChangePage by remember { mutableStateOf(false) }
    val zoomState = rememberZoomableState(ZoomSpec(maxZoomFactor = 3.5f))
    var forciblyShowBottomBar by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(0.dp) }
    val context = LocalContext.current
    val prefs = context.prefs
    val scope = rememberCoroutineScope()
    var dataSaver by remember { mutableStateOf(DataSaver.AUTO) }
    var storageLocation by remember { mutableStateOf(Uri.EMPTY) }
    val isUsingWifi = isUsingWiFi(context)
    val requester = remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }


    LaunchedEffect(true) {
        prefs.getPreferences.collect {value ->
            dataSaver = value.dataSaver
            storageLocation = value.storageLocation
        }
    }

    // Large image view is an overlay rather than a new screen entirely so we need to override
    // the default back button behaviour so we don't get taken to the home page.
    BackHandler(shouldShowLargeImage.value) {
        shouldShowLargeImage.value = false
    }

    PredictiveBackHandler(shouldShowLargeImage.value) { progress ->
        try {
            progress.collect { backEvent ->
                offset = (backEvent.progress * 200).dp
            }
            shouldShowLargeImage.value = false
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

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Image",
            loading = { FullscreenLoadingSpinner() },
            modifier = modifier
                .scale(0.95f)
                .clip(RoundedCornerShape(24.dp))
        )
    }

    ProcrasturbatingTheme {
        if (requester.value) {
            SaveDirectorySelection(requester = requester)
        }

        Scaffold(
            modifier = Modifier.offset(y=offset),
            bottomBar = {
                AnimatedVisibility(
                    visible = ((zoomState.zoomFraction ?: 0f) < 0.15f) || forciblyShowBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    BottomAppBar(
                        actions = {
                            IconButton(
                                onClick = { allImages[pagerState.settledPage].toggleHd() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                val vectorIcon = if (allImages[pagerState.settledPage].preferHd) {
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
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(ImageVector.vectorResource(id = R.drawable.ic_star_hollow),
                                    contentDescription = "Add to favourites")
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, allImages[pagerState.settledPage].highestQualityFormatUrl)
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
                                    scope.launch {
                                        isDownloading = true
                                        val result: Result<Boolean> = downloadImage(
                                            context,
                                            allImages[pagerState.settledPage],
                                            storageLocation
                                        )

                                        if (result.isSuccess) {
                                            Toast.makeText(context,
                                                "Image saved.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        else {
                                            val exc = result.exceptionOrNull()!!
                                            println(exc)
                                            if (exc is MustSetLocation) {
                                                requester.value = true
                                            }
                                            Toast.makeText(
                                                context,
                                                exc.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        isDownloading = false
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
                beyondBoundsPageCount = 1
            ) {index ->
                val currentImg = allImages[index]

                if (currentImg.hdQualityOverride == null) {
                    when (dataSaver) {
                        DataSaver.ON -> currentImg.preferHd = false
                        DataSaver.OFF -> currentImg.preferHd = true
                        DataSaver.AUTO -> currentImg.preferHd = isUsingWifi
                    }
                }

                Column(Modifier.zoomable(
                    zoomState,
                    onClick = {
                        forciblyShowBottomBar = !forciblyShowBottomBar
                    }
                )) {
                    Row(
                        modifier = Modifier
                            .weight(1f, true)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .statusBarsPadding()
                            .padding(bottom = 80.dp), // To account for the bottom bar
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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