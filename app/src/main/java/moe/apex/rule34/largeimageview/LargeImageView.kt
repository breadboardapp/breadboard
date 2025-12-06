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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableState
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
import moe.apex.rule34.util.bouncyAnimationSpec
import moe.apex.rule34.util.downloadImage
import moe.apex.rule34.util.fixLink
import moe.apex.rule34.util.isWebLink
import moe.apex.rule34.util.rememberIsBlurEnabled
import moe.apex.rule34.util.saveUriToPref
import moe.apex.rule34.viewmodel.BreadboardViewModel
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutionException
import kotlin.math.roundToInt


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
    initialPage: Int,
    allImages: List<Image>,
    backgroundAlpha: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        initialPageOffsetFraction = 0f
    ) { allImages.size }
    var canChangePage by remember { mutableStateOf(false) }
    val zoomState = rememberZoomableState(ZoomSpec(maxZoomFactor = 3.5f))
    var toolbarState by remember { mutableStateOf(ToolbarState.DEFAULT) }
    val viewModel = viewModel<BreadboardViewModel>()

    val isFullyZoomedOut by remember { derivedStateOf { zoomState.zoomFraction == 0f } }
    val isMostlyZoomedOut by remember { derivedStateOf { zoomState.zoomFraction.let { it == null || it < 0.10 } } }

    LaunchedEffect(allImages.size) {
        if (pagerState.currentPage >= allImages.size && allImages.isNotEmpty()) {
            pagerState.scrollToPage(allImages.size - 1)
        }
    }

    val currentImage = allImages[pagerState.currentPage.coerceIn(0, allImages.size - 1)]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = backgroundAlpha)
    ) {
        Box(Modifier.fillMaxSize()) {
            fun toggleToolbar() {
                val isVisible = toolbarState == ToolbarState.FORCE_SHOW || (isMostlyZoomedOut && toolbarState != ToolbarState.FORCE_HIDE)
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
                zoomState = zoomState,
                onImageClick = ::toggleToolbar
            )

            // Disable page changing while zoomed in and reset bottom bar state
            LaunchedEffect(isFullyZoomedOut, pagerState.currentPage) {
                canChangePage = isFullyZoomedOut
                toolbarState = ToolbarState.DEFAULT
            }

            LaunchedEffect(zoomState.zoomFraction) {
                onZoomChange?.invoke(zoomState.zoomFraction ?: 0f)
            }

            Box(Modifier.align(Alignment.BottomCenter)) {
                LargeImageToolbar(
                    toolbarState = toolbarState,
                    isMostlyZoomedOut = isMostlyZoomedOut,
                    viewModel = viewModel,
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
    zoomState: ZoomableState,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val isUsingWifi = remember { isUsingWiFi(context) }
    val dataSaver = prefs.dataSaver

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

        /* TODO: Give each image its own zoom state.
           Need to consider how it interacts with the LargeImageView toolbar and onZoomChange. */
        Box(
            modifier = Modifier.zoomable(
                state = zoomState,
                onClick = { onImageClick() }
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SMALL_LARGE_SPACER.dp)
                    .systemBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                LargeImage(imageAtIndex)
            }
        }
    }
}


@Composable
private fun LargeImageToolbar(
    modifier: Modifier = Modifier,
    toolbarState: ToolbarState,
    isMostlyZoomedOut: Boolean,
    viewModel: BreadboardViewModel,
    navController: NavController,
    currentImage: Image
) {
    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val scope = rememberCoroutineScope()

    val storageLocation = prefs.storageLocation
    val favouriteImages = prefs.favouriteImages
    val actions = prefs.imageViewerActions.drop(1)
    val primaryAction = prefs.imageViewerActions.first()

    var showInfoSheet by remember { mutableStateOf(false) }
    var storageLocationPromptLaunched by remember { mutableStateOf(false) }

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
        HorizontalFloatingToolbar(
            modifier = modifier
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
        LargeImageView(navController, 0, listOf(image!!))
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
    /* Poor method of the preliminary work to get rounded corners for favourites saved before
       we started saving the aspect ratio. */
    val aspectRatio = image.aspectRatio
    val imageUrl = if (image.preferHd) image.highestQualityFormatUrl else image.sampleUrl
    val previewImageUrl = image.previewUrl
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val modifier = if (aspectRatio == null) {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxHeight()
        }
    } else Modifier.aspectRatio(aspectRatio)

    val model = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .build()
    }

    val previewModel = remember(previewImageUrl) {
        ImageRequest.Builder(context)
            .data(previewImageUrl)
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


@Composable
fun OffsetBasedLargeImageView(
    navController: NavController,
    visibilityState: MutableState<Boolean>,
    initialPage: Int,
    allImages: List<Image>,
    bottomBarVisibleState: MutableState<Boolean>? = null,
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

    /* In the past we used initialPage to determine whether or not we should recompose the viewer.
       However, this causes an issue where the viewer doesn't reset when it really should,
       just because the user swiped to a different image and then tapped the first one again.
       For instance, lets say the user taps image 1, swipes to image 2, dismisses the viewer,
       and taps image 1 again before the closing animation is finished. Because initialPage didn't
       change, the user is still seeing image 2, which is bad.

       This is a really poor solution to that. We will trigger a recomposition manually by
       incrementing this value and use a LaunchedEffect that increments the value when the
       visibility state becomes true.

       Why not just use visibilityState directly? Because we don't want to recompose the viewer when
       it becomes false (i.e. the user is dismissing the viewer) because if the user has swiped to
       change page, recreating it with the original initialPage will cause that image to reappear
       as it disappears.

       Why not use a boolean value that just flips to trigger a recomposition? Because it causes
       other issues that are difficult to describe but easy to notice when using the app.

       There is probably a really simple (and, crucially, better) solution to this
       and I'm too stupid to see it. Quite frankly i'm sick of debugging and this works
       so I'm keeping it.

       PRs welcome (please). */
    var stupidFuckingRecompositionCounter by rememberSaveable { mutableIntStateOf(0) }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            val new = (animatableOffset.value + delta).coerceAtLeast(0f)
            animatableOffset.snapTo(new)
        }
    }

    fun calculateScaleFactor(offsetValue: Float): Float {
        return 1 - ((offsetValue * 1.2f) / windowHeightPx)
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
        visibilityState.value = false
    }

    if (allImages.isEmpty()) {
        hide(animate = false)
        return
    }

    PredictiveBackHandler(enabled = visibilityState.value) { progress ->
        try {
            progress.collect { backEvent ->
                val offsetPx = with(density) { (backEvent.progress * 300f).dp.toPx() }
                snapTo(offsetPx)
            }
            hide()
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(visibilityState.value) {
        bottomBarVisibleState?.value = !visibilityState.value
        if (visibilityState.value) {
            stupidFuckingRecompositionCounter ++
            show()
        }
        // No hide() call here because it's managed by the back handler and draggable modifier.
    }

    /* We should treat this as the proper source of truth as to whether or not the content is
       currently visible.
       I know this whole composable is messy now but hopefully this helps somewhat. */
    val shouldMainContentBeVisible by remember {
        derivedStateOf { visibilityState.value || animatableOffset.value < windowHeightPx }
    }

    if (shouldMainContentBeVisible) {
        if (isImmersiveModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = calculateScaleFactor(animatableOffset.value)
                    }
                    .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            )
        } else {
            /* This is so the user doesn't see the grid/background underneath the
               LargeImage carousel if they fling it up quickly (due to the spring animation). */
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, animatableOffset.value.roundToInt().coerceAtLeast(0)) }
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, animatableOffset.value.roundToInt()) }
                .draggable(
                    enabled = canDragDown && visibilityState.value,
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
            key(stupidFuckingRecompositionCounter) {
                LargeImageView(
                    navController = navController,
                    initialPage = initialPage,
                    allImages = allImages,
                    backgroundAlpha = if (isImmersiveModeEnabled) 0f else 1f
                ) {
                    canDragDown = it == 0f
                }
            }
        }
    }
}
