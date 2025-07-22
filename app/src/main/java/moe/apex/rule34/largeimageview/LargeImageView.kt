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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import moe.apex.rule34.R
import moe.apex.rule34.image.Image
import moe.apex.rule34.preferences.DataSaver
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.ToolbarAction
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.Typography
import moe.apex.rule34.util.CombinedClickableAction
import moe.apex.rule34.util.showToast
import moe.apex.rule34.util.FullscreenLoadingSpinner
import moe.apex.rule34.util.HorizontalFloatingToolbar
import moe.apex.rule34.util.PromptType
import moe.apex.rule34.util.MustSetLocation
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.StorageLocationSelection
import moe.apex.rule34.util.downloadImage
import moe.apex.rule34.util.fixLink
import moe.apex.rule34.util.isWebLink
import moe.apex.rule34.util.saveUriToPref
import moe.apex.rule34.viewmodel.BreadboardViewModel
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutionException


private fun isUsingWiFi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(networkInfo)

    return networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}


private enum class ToolbarState {
    DEFAULT,
    FORCE_SHOW,
    FORCE_HIDE
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LargeImageView(
    navController: NavController,
    visible: MutableState<Boolean>? = null,
    initialPage: Int,
    allImages: List<Image>
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f
    ) { allImages.size }
    var canChangePage by remember { mutableStateOf(false) }
    val zoomState = rememberZoomableState(ZoomSpec(maxZoomFactor = 3.5f))
    var toolbarState by remember { mutableStateOf(ToolbarState.DEFAULT) }
    var offset by remember { mutableStateOf(0.dp) }
    val context = LocalContext.current
    val viewModel = viewModel<BreadboardViewModel>()
    val scope = rememberCoroutineScope()
    val isUsingWifi = isUsingWiFi(context)
    var storageLocationPromptLaunched by remember { mutableStateOf(false) }

    val isFullyZoomedOut by remember { derivedStateOf { zoomState.zoomFraction == 0f } }
    val isMostlyZoomedOut by remember { derivedStateOf { zoomState.zoomFraction.let {
        if (it == null) false
        else it < 0.10
    } } }

    if (allImages.isEmpty()) {
        visible?.value = false
        return
    }

    runBlocking {
        if (pagerState.currentPage >= allImages.size)
            pagerState.scrollToPage(allImages.size - 1)
    }

    val currentImage = allImages[pagerState.currentPage]
    var showInfoSheet by rememberSaveable { mutableStateOf(false) }

    val prefs = LocalPreferences.current
    val dataSaver = prefs.dataSaver
    val storageLocation = prefs.storageLocation
    val favouriteImages = prefs.favouriteImages
    val actions = prefs.imageViewerActions.drop(1)
    val primaryAction = prefs.imageViewerActions.first()

    val actionMapping = mapOf<ToolbarAction, @Composable () -> ImageAction?>(
        ToolbarAction.TOGGLE_HD to {
            ImageAction(
                onClick = { currentImage.toggleHd() }
            ) {
                val vectorIcon = if (currentImage.preferHd) {
                    ToolbarAction.TOGGLE_HD.enabledIcon
                } else {
                    ImageVector.vectorResource(R.drawable.ic_hd_disabled)
                }
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = "Toggle HD",
                    modifier = Modifier.scale(1.2F)
                )
            }
        },
        ToolbarAction.FAVOURITE to {
            val isFavourited = favouriteImages.any { it.fileName == currentImage.fileName && it.imageSource == currentImage.imageSource }
            ImageAction(
                onClick = {
                    scope.launch {
                        if (isFavourited) {
                            context.prefs.removeFavouriteImage(currentImage)
                            showToast(context, "Removed from your favourites")
                        } else {
                            context.prefs.addFavouriteImage(currentImage)
                            showToast(context, "Added to your favourites")
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isFavourited) ToolbarAction.FAVOURITE.enabledIcon else Icons.Rounded.FavoriteBorder,
                    contentDescription = "${if (isFavourited) "Remove from" else "Add to"} favourites"
                )
            }
        },
        ToolbarAction.SHARE to {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
            }
            ImageAction(
                onClick = {
                    var shareLink = currentImage.metadata?.pixivUrl
                        ?: currentImage.metadata?.source.let { if (it?.isWebLink() == true) it else null }
                        ?: currentImage.highestQualityFormatUrl
                    if (prefs.useFixedLinks) shareLink = fixLink(shareLink)
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareLink)
                    context.startActivity(Intent.createChooser(shareIntent, null))
                },
                onLongClick = {
                    shareIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        currentImage.highestQualityFormatUrl
                    )
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
            ) {
                Icon(
                    imageVector = ToolbarAction.SHARE.enabledIcon,
                    contentDescription = "Share"
                )
            }
        },
        ToolbarAction.INFO to {
            if (currentImage.metadata != null) {
                ImageAction(
                    onClick = { showInfoSheet = true }
                ) {
                    Icon(
                        imageVector = ToolbarAction.INFO.enabledIcon,
                        contentDescription = "Info"
                    )
                }
            } else {
                null
            }
        },
        ToolbarAction.DOWNLOAD to {
            ImageAction(
                enabled = currentImage !in viewModel.downloadingImages,
                onClick = {
                    if (currentImage !in viewModel.downloadingImages) {
                        viewModel.viewModelScope.launch {
                            viewModel.downloadingImages.add(currentImage)
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
                                    storageLocationPromptLaunched = true
                                }
                                showToast(context, exc.message ?: "Unknown error")
                                Log.e(
                                    "Downloader",
                                    exc.message ?: "Error downloading image",
                                    exc
                                )
                            }
                            viewModel.downloadingImages.remove(currentImage)
                        }
                    }
                }
            ) {
                if (currentImage in viewModel.downloadingImages) {
                    CircularProgressIndicator(
                        modifier = Modifier.scale(0.5F),
                        color = LocalContentColor.current
                    )
                } else {
                    Icon(
                        imageVector = ToolbarAction.DOWNLOAD.enabledIcon,
                        contentDescription = "Save",
                        modifier = Modifier.scale(1.1f)
                    )
                }
            }
        }
    )

    if (showInfoSheet) {
        InfoSheet(navController, currentImage, { showInfoSheet = false })
    }

    LaunchedEffect(visible?.value) {
        if (visible?.value == true) offset = 0.dp
    }

    PredictiveBackHandler(visible?.value == true) { progress ->
        try {
            progress.collect { backEvent ->
                offset = (backEvent.progress * 300).dp
            }
            visible?.value = false
        }
        catch (_: Exception) { }
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
            loading = {
                LoadingContentPlaceholder(modifier) {
                    SubcomposeAsyncImage(
                        model = previewImageUrl,
                        contentDescription = "Image",
                        contentScale = ContentScale.Fit,
                        modifier = modifier.clip(MaterialTheme.shapes.extraLarge)
                    )
                }
            },
            modifier = modifier
                .scale(0.95f)
                .clip(MaterialTheme.shapes.extraLarge)
        )
    }

    if (storageLocationPromptLaunched) {
        StorageLocationSelection(
            promptType = PromptType.DIRECTORY_PERMISSION,
            onFailure = { storageLocationPromptLaunched = false }
        ) {
            saveUriToPref(context, scope, it)
            storageLocationPromptLaunched = false
        }
    }

    Scaffold(modifier = Modifier.offset { IntOffset(y = offset.roundToPx(), x = 0) }) {
        Box(Modifier.fillMaxSize()) {
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

                Box(
                    Modifier.zoomable(
                        zoomState,
                        onClick = {
                            toolbarState = when (toolbarState) {
                                ToolbarState.DEFAULT -> if (isMostlyZoomedOut) ToolbarState.FORCE_HIDE else ToolbarState.FORCE_SHOW
                                ToolbarState.FORCE_SHOW -> ToolbarState.FORCE_HIDE
                                ToolbarState.FORCE_HIDE -> if (isMostlyZoomedOut) ToolbarState.DEFAULT else ToolbarState.FORCE_SHOW
                            }
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
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

            // Disable page changing while zoomed in and reset bottom bar state
            LaunchedEffect(isFullyZoomedOut, pagerState.currentPage) {
                canChangePage = isFullyZoomedOut
                toolbarState = ToolbarState.DEFAULT
            }

            Box(Modifier.align(Alignment.BottomCenter)) {
                AnimatedVisibility(
                    visible = toolbarState == ToolbarState.FORCE_SHOW || (isMostlyZoomedOut && toolbarState != ToolbarState.FORCE_HIDE),
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    HorizontalFloatingToolbar(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = SMALL_LARGE_SPACER.dp),
                        actions = {
                            for (action in actions) {
                                val item = actionMapping[action]!!()
                                if (item == null) continue
                                val interactionSource = remember { MutableInteractionSource() }
                                CombinedClickableAction(
                                    enabled = item.enabled,
                                    interactionSource = interactionSource,
                                    onClick = item.onClick,
                                    onLongClick = item.onLongClick
                                ) {
                                    IconButton(
                                        modifier = item.modifier,
                                        enabled = item.enabled,
                                        onClick = { },
                                        interactionSource = interactionSource
                                    ) {
                                        item.composableContent()
                                    }
                                }
                            }
                        },
                        floatingActionButton = actionMapping[primaryAction]!!()?.let { {
                            val interactionSource = remember { MutableInteractionSource() }
                            CombinedClickableAction(
                                enabled = it.enabled,
                                interactionSource = interactionSource,
                                onClick = it.onClick,
                                onLongClick = it.onLongClick
                            ) {
                                FloatingActionButton(
                                    modifier = it.modifier,
                                    onClick = { },
                                    interactionSource = interactionSource
                                ) {
                                    it.composableContent()
                                }
                            }
                        } }
                    )
                }
            }
        }
    }
}


@Composable
fun LazyLargeImageView(
    navController: NavController,
    onImageLoadRequest: () -> Image?
) {
    val context = LocalContext.current
    var image by remember { mutableStateOf<Image?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                image = onImageLoadRequest()
            }
        } catch (e: ExecutionException) {
            if (e.cause is SocketTimeoutException) {
                showToast(context, "Connection timed out")
            }
        }
        isLoading = false
    }

    if (isLoading)
        FullscreenLoadingSpinner()
    else if (image == null)
        ImageNotFound()
    else
        LargeImageView(navController, null, 0, listOf(image!!))
}


@Composable
private fun ImageNotFound() {
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


@Composable
private fun LoadingContentPlaceholder(
    modifier: Modifier,
    content: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        content?.invoke()
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            FullscreenLoadingSpinner()
        }
    }
}


private data class ImageAction(
    val modifier: Modifier = Modifier,
    var enabled: Boolean = true,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val composableContent: @Composable () -> Unit
)

