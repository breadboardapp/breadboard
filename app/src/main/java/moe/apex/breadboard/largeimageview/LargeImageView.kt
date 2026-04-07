package moe.apex.breadboard.largeimageview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialShapes.Companion.Cookie7Sided
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import moe.apex.breadboard.R
import moe.apex.breadboard.VolumeButtonHandler
import moe.apex.breadboard.image.Image
import moe.apex.breadboard.preferences.AutoplayVideosMode
import moe.apex.breadboard.preferences.DataSaver
import moe.apex.breadboard.preferences.Experiment
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.preferences.ToolbarAction
import moe.apex.breadboard.prefs
import moe.apex.breadboard.ui.theme.BreadboardTheme
import moe.apex.breadboard.ui.theme.Typography
import moe.apex.breadboard.util.CombinedClickableAction
import moe.apex.breadboard.util.showToast
import moe.apex.breadboard.util.FullscreenLoadingSpinner
import moe.apex.breadboard.util.HorizontalFloatingToolbarOptionalFab
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.MorphableRoundedPolygon
import moe.apex.breadboard.util.PromptType
import moe.apex.breadboard.util.MustSetLocation
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.StorageLocationSelection
import moe.apex.breadboard.util.bouncyAnimationSpec
import moe.apex.breadboard.util.downloadImage
import moe.apex.breadboard.util.downloadImageToClipboard
import moe.apex.breadboard.util.fixLink
import moe.apex.breadboard.util.isWebLink
import moe.apex.breadboard.util.rememberIsBlurEnabled
import moe.apex.breadboard.util.saveUriToPref
import moe.apex.breadboard.util.morphingBackground
import moe.apex.breadboard.viewmodel.getGlobalViewModel
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutionException
import kotlin.math.roundToInt


private const val VIDEO_PRIMARY_CONTROL_SIZE_DP = 60
private const val MAX_ZOOM_FOR_PAGE_CHANGE = 0.075f // Allow page change if zoomed in less than 7.5%, allowing for some leniency when the user is trying to quickly zoom out and swipe to the next image before the animation is fully finished.


private fun isUsingWiFi(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
    initialPage: Int,
    allImages: List<Image>,
    onImageUpdate: (suspend (Image, Image) -> Unit)? = null,
    onZoomedStatusChanged: ((Boolean) -> Unit)? = null
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f
    ) { allImages.size }
    var canChangePage by remember { mutableStateOf(false) }
    var activeZoomState by remember { mutableStateOf<ZoomableState?>(null) }
    var toolbarState by remember { mutableStateOf(ToolbarState.DEFAULT) }

    val isFullyZoomedOut by remember {
        derivedStateOf { activeZoomState?.zoomFraction?.let { it == 0f } ?: true }
    }
    val isMostlyZoomedOut by remember {
        derivedStateOf { activeZoomState?.zoomFraction.let { it == null || it < 0.10 } }
    }
    val zoomFractionAllowsPageChange by remember {
        derivedStateOf { activeZoomState?.zoomFraction?.let { it < MAX_ZOOM_FOR_PAGE_CHANGE } ?: true }
    }

    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(allImages.size) {
        if (pagerState.currentPage >= allImages.size && allImages.isNotEmpty()) {
            pagerState.scrollToPage(allImages.size - 1)
        }
    }

    val currentImage = allImages[pagerState.currentPage.coerceIn(0, allImages.size - 1)]
    val hasGroupedTags = remember(currentImage) { currentImage.hasGroupedTags }

    if (!hasGroupedTags && onImageUpdate != null) {
        LaunchedEffect(currentImage) {
            try {
                val metadata = currentImage.imageSource.imageBoard.loadImageGroupedTags(
                    currentImage,
                    prefs.authFor(currentImage.imageSource, context)
                )

                if (metadata != null) {
                    val newImage = currentImage.copy(metadata = metadata)

                    scope.launch {
                        onImageUpdate(currentImage, newImage)
                    }
                }
            } catch (e: CancellationException) {
                // Ignore the CancellationException error above because we want it to be cancelled
            } catch (e: Exception) {
                Log.e("LargeImageView", "Error fetching image grouped tags", e)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) {
        Box(Modifier.fillMaxSize()) {
            fun toggleToolbar() {
                val isVisible =
                    toolbarState == ToolbarState.FORCE_SHOW || (isMostlyZoomedOut && toolbarState != ToolbarState.FORCE_HIDE)
                toolbarState = when (toolbarState) {
                    ToolbarState.FORCE_SHOW -> ToolbarState.FORCE_HIDE
                    ToolbarState.FORCE_HIDE -> ToolbarState.FORCE_SHOW
                    ToolbarState.DEFAULT -> if (isVisible) ToolbarState.FORCE_HIDE else ToolbarState.FORCE_SHOW
                }
            }

            ImagesPager(
                pagerState = pagerState,
                allImages = allImages,
                canChangePage = canChangePage,
                onZoomStateChanged = { activeZoomState = it },
                onImageClick = ::toggleToolbar
            )

            LaunchedEffect(zoomFractionAllowsPageChange, isFullyZoomedOut, pagerState.currentPage) {
                canChangePage = zoomFractionAllowsPageChange
                if (isFullyZoomedOut) {
                    toolbarState = ToolbarState.DEFAULT
                }
            }

            LaunchedEffect(isFullyZoomedOut) {
                onZoomedStatusChanged?.invoke(!isFullyZoomedOut)
            }

            Box(Modifier.align(Alignment.BottomCenter)) {
                LargeImageToolbar(
                    toolbarState = toolbarState,
                    isMostlyZoomedOut = isMostlyZoomedOut,
                    navController = navController,
                    currentImage = currentImage
                )
            }
        }
    }
}


@Composable
private fun ImagesPager(
    pagerState: PagerState,
    allImages: List<Image>,
    canChangePage: Boolean,
    onZoomStateChanged: (ZoomableState) -> Unit,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? VolumeButtonHandler
            activity?.volumeUpPressedCallback = null
            Log.i("video", "Released volume up listener.")
        }
    }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = canChangePage,
        beyondViewportPageCount = 1
    ) { index ->
        val imageAtIndex = allImages[index]

        val zoomState = rememberZoomableState(ZoomSpec(maxZoomFactor = 3.5f))
        val isBarelyZoomedIn by remember {
            derivedStateOf {
                zoomState.zoomFraction?.let { it < MAX_ZOOM_FOR_PAGE_CHANGE } ?: true
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage == index) {
                onZoomStateChanged(zoomState)
            } else {
                zoomState.resetZoom(bouncyAnimationSpec())
            }
        }

        val gestures = if (imageAtIndex.isVideo || (isBarelyZoomedIn && zoomState.isAnimationRunning)) {
            EnabledZoomGestures.None
        } else {
            EnabledZoomGestures.ZoomAndPan
        }

        Box(
            modifier = Modifier.zoomable(
                state = zoomState,
                onClick = { onImageClick() },
                gestures = gestures
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SMALL_LARGE_SPACER.dp)
                    .systemBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                if (imageAtIndex.isVideo) {
                    LargeVideo(
                        image = imageAtIndex,
                        isCurrentPage = pagerState.currentPage == index,
                        onLongClick = onImageClick
                    )
                } else {
                    LargeImage(imageAtIndex)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LargeImageToolbar(
    modifier: Modifier = Modifier,
    toolbarState: ToolbarState,
    isMostlyZoomedOut: Boolean,
    navController: NavController,
    currentImage: Image
) {
    val viewModel = getGlobalViewModel()
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val preferHd = viewModel.rememberImageHdQualityPreference(currentImage, prefs.dataSaver, isUsingWiFi(context))

    val storageLocation = prefs.storageLocation
    val favouriteImages = prefs.favouriteImages
    val actions = prefs.imageViewerActions.drop(1)
    val primaryAction = prefs.imageViewerActions.first()
    val downloadingImages by viewModel.downloadingImages.collectAsState()

    var showInfoSheet by remember { mutableStateOf(false) }
    var storageLocationPromptLaunched by remember { mutableStateOf(false) }

    val actionMapping = mapOf(
        ToolbarAction.TOGGLE_HD to {
            ImageAction(
                onClick = { viewModel.setImageHdQualityOverride(currentImage, !preferHd) }
            ) {
                val vectorIcon = if (preferHd) {
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
            val isFavourited =
                favouriteImages.any { it.fileName == currentImage.fileName && it.imageSource == currentImage.imageSource }
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
                enabled = currentImage !in downloadingImages,
                onClick = {
                    if (currentImage !in downloadingImages) {
                        viewModel.viewModelScope.launch {
                            viewModel.addDownloadingImage(currentImage)
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
                                    showToast(context, exc.message!!)
                                    storageLocationPromptLaunched = true
                                } else {
                                    showToast(context, exc.message ?: "Unknown error")
                                }
                                Log.e(
                                    "Downloader",
                                    exc.message ?: "Error downloading image",
                                    exc
                                )
                            }
                            viewModel.removeDownloadingImage(currentImage)
                        }
                    }
                },
                onLongClick = {
                    if (!prefs.isExperimentEnabled(Experiment.COPY_TO_CLIPBOARD)) return@ImageAction

                    if (currentImage !in downloadingImages) {
                        viewModel.viewModelScope.launch {
                            viewModel.addDownloadingImage(currentImage)
                            val result: Result<Boolean> = downloadImageToClipboard(
                                context = context,
                                clipboard = clipboard,
                                image = currentImage
                            )

                            if (result.isSuccess) {
                                showToast(context, "Copied to clipboard.")
                            } else {
                                val exc = result.exceptionOrNull()!!
                                showToast(context, "Error copying image")
                                Log.e(
                                    "Downloader",
                                    exc.message ?: "Error copying image",
                                    exc
                                )
                            }
                            viewModel.removeDownloadingImage(currentImage)
                        }
                    }
                }
            ) {
                if (currentImage in downloadingImages) {
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
        key(currentImage) {
            InfoSheet(navController, currentImage) {
                showInfoSheet = false
            }
        }
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

    AnimatedVisibility(
        visible = toolbarState == ToolbarState.FORCE_SHOW || (isMostlyZoomedOut && toolbarState != ToolbarState.FORCE_HIDE),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = bouncyAnimationSpec()
        ),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        HorizontalFloatingToolbarOptionalFab(
            modifier = modifier
                .navigationBarsPadding()
                .padding(bottom = SMALL_SPACER.dp),
            floatingActionButton = actionMapping[primaryAction]!!()?.let {
                {
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
                }
            }
        ) {
            for (action in actions) {
                val item = actionMapping[action]!!() ?: continue
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
        }
    }
}


@Composable
fun LazyLargeImageView(
    navController: NavController,
    imageSource: ImageSource,
    id: String,
    isMd5: Boolean
) {
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    var image by remember { mutableStateOf<Image?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val auth = prefs.authFor(imageSource, context)
            image = if (isMd5) imageSource.imageBoard.loadImageMd5(id, auth)
                    else imageSource.imageBoard.loadImage(id, auth)
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
        LargeImageView(
            navController,
            0,
            listOf(image!!),
            onImageUpdate = { _, newImage -> image = newImage }
        )
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
    modifier: Modifier = Modifier,
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


@Composable
fun LargeImage(image: Image) {
    val context = LocalContext.current
    val viewModel = getGlobalViewModel()

    val prefs = LocalPreferences.current
    val preferHd = viewModel.rememberImageHdQualityPreference(image, prefs.dataSaver, isUsingWiFi(context))

    /* Poor method of the preliminary work to get rounded corners for favourites saved before
       we started saving the aspect ratio. */
    val aspectRatio = image.aspectRatio
    val imageUrl = if (preferHd) image.highestQualityFormatUrl else image.sampleUrl
    val previewImageUrl = image.previewUrl
    var isLoading by remember { mutableStateOf(false) }

    val modifier = if (aspectRatio == null) {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxHeight()
        }
    } else Modifier.aspectRatio(aspectRatio)

    val headersBuilder = remember {
        NetworkHeaders.Builder()
            .set("Referer", image.imageSource.imageBoard.baseUrl)
    }

    val model = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .httpHeaders(headersBuilder.build())
            .build()
    }

    val previewModel = remember(previewImageUrl) {
        ImageRequest.Builder(context)
            .data(previewImageUrl)
            .httpHeaders(headersBuilder.build())
            .build()
    }

    Box(
        modifier = Modifier.clip(MaterialTheme.shapes.extraLarge)
    ) {
        AsyncImage(
            model = model,
            placeholder = rememberAsyncImagePainter(previewModel),
            onSuccess = { isLoading = false },
            onError = { isLoading = false },
            onLoading = { isLoading = true },
            contentDescription = "Image",
            modifier = modifier
        )
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LoadingContentPlaceholder()
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoMuteButton(
    muted: Boolean,
    onMutedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconToggleButton(
        modifier = modifier,
        checked = !muted,
        shapes = IconToggleButtonShapes(
            shape = CircleShape,
            pressedShape = MaterialTheme.shapes.medium,
            checkedShape = MaterialTheme.shapes.medium
        ),
        onCheckedChange = {
            onMutedChange(!it)
        },
    ) {
        if (muted) {
            Icon(
                Icons.AutoMirrored.Rounded.VolumeOff,
                contentDescription = "Volume muted. Tap to unmute.",
            )
        } else {
            Icon(
                Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Volume on. Tap to mute.",
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun VideoPlayPauseButton(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val morphProgress by animateFloatAsState(if (isPlaying) 1f else 0f)
    IconButton(
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier
            .size(VIDEO_PRIMARY_CONTROL_SIZE_DP.dp)
            .morphingBackground(
                start = MorphableRoundedPolygon(Circle),
                end = MorphableRoundedPolygon(Cookie7Sided, 1.1f),
                progress = morphProgress,
                color = MaterialTheme.colorScheme.primary
            ),
        onClick = onPlayPauseClick
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
            contentAlignment = Alignment.Center
        ) {
            if (it) {
                Icon(
                    imageVector = Icons.Rounded.Pause,
                    contentDescription = "Pause",
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Resume",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LargeVideo(image: Image, isCurrentPage: Boolean, onLongClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = getGlobalViewModel()

    var wasPlaying by remember { mutableStateOf(false) }

    // TODO: https://github.com/kdroidFilter/ComposeMediaPlayer/issues/107
    val player = rememberVideoPlayerState()

    var doneInitialLoad by remember { mutableStateOf(false) }

    var showControls by remember { mutableStateOf(false) }
    var controlsLastTriggeredAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val controlsInteractionSource = remember { MutableInteractionSource() }
    val isHovered by controlsInteractionSource.collectIsHoveredAsState()
    val focusRequester = remember { FocusRequester() }

    val sliderInteractionSource = remember { MutableInteractionSource() }
    val isSliderDragging by sliderInteractionSource.collectIsDraggedAsState()
    val isSliderPressed by sliderInteractionSource.collectIsPressedAsState()

    val prefs = LocalPreferences.current
    val shouldAutoplay = remember {
        when (prefs.autoplayVideos) {
            AutoplayVideosMode.ON -> true
            AutoplayVideosMode.OFF -> false
            AutoplayVideosMode.AUTO ->
                when (prefs.dataSaver) {
                    DataSaver.ON -> false
                    DataSaver.OFF -> true
                    DataSaver.AUTO -> isUsingWiFi(context)
                }
        }
    }

    val userMutePreference by viewModel.userMutePreference.collectAsState()
    var muted by remember {
        mutableStateOf(userMutePreference ?: shouldAutoplay)
    }

    val aspectRatio = image.aspectRatio

    val modifier = if (aspectRatio == null) {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxHeight()
        }
    } else Modifier.aspectRatio(aspectRatio)

    fun updateControlsLastTriggeredTime() {
        controlsLastTriggeredAt = System.currentTimeMillis()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    /* While most of these run very infrequently or only once,
       I still don't like this LaunchedEffect hell.  */

    LaunchedEffect(Unit) {
        player.loop = true
    }

    LaunchedEffect(player.isLoading) {
        if (player.hasMedia && !player.isLoading) {
            doneInitialLoad = true
        }
    }

    LaunchedEffect(isHovered) {
        showControls = isHovered
    }

    LaunchedEffect(controlsLastTriggeredAt) {
        if (showControls && !isHovered && !isSliderDragging && !isSliderPressed) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 0.5f
    }

    LaunchedEffect(userMutePreference) {
        userMutePreference?.let { muted = it }
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            player.pause()
        } else {
            if (shouldAutoplay) {
                if (player.hasMedia) {
                    player.play()
                } else {
                    player.openUri(image.fileUrl)
                }
            }

            val activity = context as? VolumeButtonHandler
            if (activity != null) {
                fun callback(): Boolean {
                    if (muted) {
                        viewModel.setUserMutePreference(false)
                        return true
                    }
                    return false
                }
                activity.volumeUpPressedCallback = ::callback
                Log.i("video", "Attached volume up listener to a new page.")
            }

            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .width(IntrinsicSize.Min),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable(isCurrentPage, controlsInteractionSource)
                .onKeyEvent { event -> // Unmute with keyboard volume up key
                    if (event.type == KeyEventType.KeyDown && event.key == Key.VolumeUp && muted) {
                        viewModel.setUserMutePreference(false)
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                }
                .combinedClickable(
                    interactionSource = controlsInteractionSource,
                    indication = null,
                    onLongClick = onLongClick
                ) {
                    updateControlsLastTriggeredTime()
                    showControls = !showControls
                }
                .then( // Allow secondary click/right click to do the long press action
                    if (onLongClick == null) Modifier else Modifier.pointerInput(Unit) {
                        while (currentCoroutineContext().isActive) {
                            awaitPointerEventScope {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.buttons.isSecondaryPressed) {
                                    onLongClick()
                                }
                            }
                        }
                    }
                )
        ) {
            if (doneInitialLoad) {
                // The SurfaceView type performs much better but seems to have scaling issues?
                VideoPlayerSurface(modifier = modifier, playerState = player)
            } else {
                AsyncImage(
                    modifier = modifier.blur(16.dp),
                    model = image.previewUrl,
                    contentDescription = "Thumbnail",
                )
            }

            // We're always using dark theme here to give us light buttons since the scrim is black.
            BreadboardTheme(darkTheme = true) {
                AnimatedVisibility(
                    modifier = modifier,
                    visible = showControls || player.isLoading || !player.hasMedia,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                    ) {
                        VideoPlayPauseButton(
                            isPlaying = player.isPlaying,
                            modifier = Modifier.align(Alignment.Center),
                            onPlayPauseClick = {
                                updateControlsLastTriggeredTime()
                                if (!player.hasMedia) {
                                    player.openUri(image.fileUrl)
                                } else if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            }
                        )
                        AnimatedVisibility(
                            visible = player.isLoading,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size((VIDEO_PRIMARY_CONTROL_SIZE_DP + 16).dp),
                                strokeWidth = 6.dp
                            )
                        }
                    }
                }

                SharedTransitionLayout(Modifier.align(Alignment.BottomStart)) {
                    AnimatedContent(
                        targetState = showControls || player.isLoading || !player.hasMedia,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                    ) {
                        if (it) {
                            Row(
                                modifier = Modifier.padding(MEDIUM_SPACER.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp),
                            ) {
                                Slider(
                                    interactionSource = sliderInteractionSource,
                                    modifier = Modifier
                                        .weight(1f)
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState("progress_bar"),
                                            animatedVisibilityScope = this@AnimatedContent
                                        ),
                                    value = player.sliderPos,
                                    valueRange = 0f..1000f,
                                    enabled = doneInitialLoad,
                                    onValueChange = { v ->
                                        showControls = true
                                        updateControlsLastTriggeredTime()
                                        if (player.isPlaying) {
                                            player.pause()
                                            wasPlaying = true
                                        }
                                        player.userDragging = true
                                        player.sliderPos = v // Yes this is still needed despite the above
                                    },
                                    onValueChangeFinished = {
                                        player.userDragging = false
                                        player.seekTo(player.sliderPos)
                                        if (wasPlaying) {
                                            player.play()
                                            wasPlaying = false
                                        }
                                    }
                                )

                                VideoMuteButton(
                                    muted = muted,
                                    modifier = Modifier.animateEnterExit(
                                        enter = slideInHorizontally { it * 2 },
                                        exit = slideOutHorizontally { it * 2 }
                                    ),
                                    onMutedChange = {
                                        updateControlsLastTriggeredTime()
                                        muted = it
                                        viewModel.setUserMutePreference(it)
                                    }
                                )
                            }
                        } else {
                            LinearProgressIndicator(
                                progress = { player.sliderPos / 1000 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState("progress_bar"),
                                        animatedVisibilityScope = this@AnimatedContent
                                    ),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun OffsetBasedLargeImageView(
    navController: NavController,
    isActive: Boolean,
    initialPage: Int,
    allImages: List<Image>,
    onActiveStateChanged: (Boolean) -> Unit = { },
    onImageUpdate: (suspend (Image, Image) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val window = LocalWindowInfo.current
    val isImmersiveModeEnabled = rememberIsBlurEnabled()
    val windowHeightPx = window.containerSize.height.toFloat()
    val dismissVelocityThreshold = windowHeightPx // Pixels per second
    val dismissDistanceThreshold = windowHeightPx * 0.25f
    var canDragDown by remember { mutableStateOf(true) }

    val animatableOffset = remember { Animatable(windowHeightPx) }
    val animationSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)

    var viewerSessionId by remember { mutableLongStateOf(0L) }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            val new = (animatableOffset.value + delta).coerceAtLeast(0f)
            animatableOffset.snapTo(new)
        }
    }

    fun show(velocity: Float = 0f) {
        scope.launch {
            animatableOffset.animateTo(
                targetValue = 0f,
                animationSpec = animationSpec,
                initialVelocity = velocity
            )
        }
    }

    fun snapTo(offset: Float) {
        scope.launch {
            animatableOffset.snapTo(offset)
        }
    }

    fun hide(velocity: Float = animatableOffset.velocity, animate: Boolean = true) {
        scope.launch {
            if (animate) {
                animatableOffset.animateTo(
                    targetValue = windowHeightPx,
                    animationSpec = animationSpec,
                    initialVelocity = velocity
                )
            } else {
                snapTo(windowHeightPx)
            }
        }
        onActiveStateChanged(false)
    }

    if (allImages.isEmpty()) {
        hide(animate = false)
        return
    }

    PredictiveBackHandler(enabled = isActive) { progress ->
        try {
            progress.collect { backEvent ->
                val offsetPx = with(density) { (backEvent.progress * 300f).dp.toPx() }
                snapTo(offsetPx)
            }
            hide()
        } catch (_: Exception) { }
    }

    if (isActive) {
        LaunchedEffect(Unit) {
            /* Theoretically breakable if someone spoofs the system clock to never update,
               that's rather unlikely. */
            onActiveStateChanged(true)
            viewerSessionId = System.currentTimeMillis()
            show()
        }
    }

    /* We should treat this as the proper source of truth as to whether or not the content is
       currently visible.
       I know this whole composable is messy now but hopefully this helps somewhat. */
    val shouldMainContentBeVisible by remember {
        derivedStateOf { isActive || animatableOffset.value < windowHeightPx }
    }

    if (shouldMainContentBeVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (1 - (animatableOffset.value / windowHeightPx)).let {
                        if (isImmersiveModeEnabled) {
                            it * 0.5f // Linear
                        } else {
                            EaseIn.transform(it)
                        }
                    }
                }
                .background(color = MaterialTheme.colorScheme.background)
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(0, animatableOffset.value.roundToInt()) }
                .draggable(
                    enabled = canDragDown && isActive,
                    orientation = Orientation.Vertical,
                    state = draggableState,
                    onDragStopped = { velocity ->
                        scope.launch {
                            if ((velocity > dismissVelocityThreshold || animatableOffset.value > dismissDistanceThreshold) && velocity > 0) {
                                hide(velocity)
                            } else {
                                show(velocity)
                            }
                        }
                    }
                )
        ) {
            key(viewerSessionId) {
                LargeImageView(
                    navController = navController,
                    initialPage = initialPage,
                    allImages = allImages,
                    onImageUpdate = onImageUpdate,
                    onZoomedStatusChanged = { canDragDown = !it }
                )
            }
        }
    }
}
