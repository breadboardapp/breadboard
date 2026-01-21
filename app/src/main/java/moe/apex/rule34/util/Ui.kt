package moe.apex.rule34.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.history.SearchHistoryEntry
import moe.apex.rule34.preferences.Experiment
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.ui.theme.prefTitle


private const val LARGE_CORNER_DP = 20
private const val SMALL_CORNER_DP = 4
/** 24dp spacing */
const val LARGE_SPACER = 24
/** 20dp spacing */
const val MEDIUM_LARGE_SPACER = 20
/** 16dp spacing */
const val SMALL_LARGE_SPACER = 16
/** 12dp spacing */
const val MEDIUM_SPACER = 12
/** 8dp spacing */
const val SMALL_SPACER = 8
/** 4dp spacing */
const val TINY_SPACER = 4
private const val VERTICAL_DIVIDER_SPACING = 32
const val BOTTOM_APP_BAR_HEIGHT = 80
const val CHIP_SPACING = 12
const val DISABLED_OPACITY = 0.38f
private val CHIP_TOTAL_VERTICAL_PADDING = 16.dp
private val CHIP_TOTAL_HEIGHT = FilterChipDefaults.Height + 16.dp


enum class ListItemPosition(val topSize: Dp, val bottomSize: Dp) {
    TOP(LARGE_CORNER_DP.dp, SMALL_CORNER_DP.dp),
    MIDDLE(SMALL_CORNER_DP.dp, SMALL_CORNER_DP.dp),
    BOTTOM(SMALL_CORNER_DP.dp, LARGE_CORNER_DP.dp),
    SINGLE_ELEMENT(LARGE_CORNER_DP.dp, LARGE_CORNER_DP.dp)
}


@Composable
fun animateTopCornerSizeForPosition(position: ListItemPosition): Dp {
    val cornerSize by animateDpAsState(position.topSize)
    return cornerSize
}


@Composable
fun animateBottomCornerSizeForPosition(position: ListItemPosition): Dp {
    val cornerSize by animateDpAsState(position.bottomSize)
    return cornerSize
}


@Composable
private fun NavigationIcon(navController: NavController? = null) {
    val context = LocalContext.current
    if (navController != null) {
        FilledIconButton(
            modifier = Modifier.padding(horizontal = SMALL_SPACER.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = IconButtonDefaults.filledIconButtonColors().disabledContainerColor.copy(alpha = 0.065f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = {
                if (navController.previousBackStackEntry != null) {
                    navController.navigateUp()
                } else {
                    (context as Activity).finish()
                }
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back"
            )
        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LargeTitleBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior?,
    navController: NavController? = null,
    additionalActions: @Composable RowScope.() -> Unit = { }
) {
    LargeTopAppBar(
        title = { Text(title, overflow = TextOverflow.Ellipsis) },
        scrollBehavior = scrollBehavior,
        actions = additionalActions,
        navigationIcon = { NavigationIcon(navController) },
        colors = TopAppBarDefaults.largeTopAppBarColors().copy(
            scrolledContainerColor = BreadboardTheme.colors.titleBar
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallTitleBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navController: NavController? = null,
    additionalActions: @Composable RowScope.() -> Unit = { }
) {
    TopAppBar(
        title = { Text(title) },
        scrollBehavior = scrollBehavior,
        actions = additionalActions,
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor = BreadboardTheme.colors.titleBar
        ),
        navigationIcon = { NavigationIcon(navController) }
    )
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


@Composable
fun PaddingValues.withoutVertical(top: Boolean = true, bottom: Boolean = true) : PaddingValues {
    val lld = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(lld),
        end = calculateEndPadding(lld),
        top = if (top) 0.dp else calculateTopPadding(),
        bottom = if (bottom) 0.dp else calculateBottomPadding()
    )
}


/** A higher level MainScreenScaffold that provides two preset types of top bars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenScaffold(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    largeTopBar: Boolean = true,
    addBottomPadding: Boolean = true,
    blur: Boolean = false,
    floatingActionButton: (@Composable () -> Unit)? = null,
    additionalActions: @Composable RowScope.() -> Unit = { },
    content: @Composable (PaddingValues) -> Unit
) {
    MainScreenScaffold(
        topAppBar = {
            if (largeTopBar) {
                LargeTitleBar(title, scrollBehavior, additionalActions = additionalActions)
            } else {
                SmallTitleBar(
                    title,
                    scrollBehavior = scrollBehavior,
                    additionalActions = additionalActions
                )
            }
        },
        addBottomPadding = addBottomPadding,
        blur = blur,
        floatingActionButton = floatingActionButton,
        content = content
    )
}


/** A lower level MainScreenScaffold that allows passing in a custom top bar for more
    fine grained control over its behaviour and appearance.

    [blur] is not supported on Android 11 or below.*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenScaffold(
    topAppBar: @Composable () -> Unit,
    addBottomPadding: Boolean = true,
    blur: Boolean = false,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val isBlurEnabled = rememberIsBlurEnabled()
    val graphicsLayer = rememberGraphicsLayer()

    /* Animating the blur radius gets expensive and performs badly on low end devices.
       Instead, we're going to fade in a "copy" of the content with a pre-applied blur effect.
       It doesn't look as good as animating the radius but it is much better for performance.
       https://developer.android.com/develop/ui/compose/graphics/draw/modifiers#composable-to-bitmap
    */

    Scaffold(
        modifier = if (isBlurEnabled) {
            Modifier.drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
        } else Modifier,
        topBar = topAppBar,
        floatingActionButton = {
            floatingActionButton?.let {
                Box(Modifier.offset(y = if (addBottomPadding) -BOTTOM_APP_BAR_HEIGHT.dp else 0.dp)) {
                    it()
                }
            }
        }
    ) {
        val lld = LocalLayoutDirection.current
        val newPadding = PaddingValues(
            start = it.calculateStartPadding(lld),
            end = it.calculateEndPadding(lld),
            top = it.calculateTopPadding(),
            bottom = if (addBottomPadding) it.calculateBottomPadding() + BOTTOM_APP_BAR_HEIGHT.dp else 0.dp
        )
        content(newPadding)
    }

    AnimatedVisibility(
        visible = blur && isBlurEnabled,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(400))
    ) {
        var bitmap: ImageBitmap? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            bitmap = graphicsLayer.toImageBitmap()
        }

        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp)
            )
        }
    }
}




@Composable
fun Summary(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.fillMaxWidth()
    )
}


@Composable
fun TitleSummary(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val baseModifier = modifier.heightIn(min = 76.dp)
    val finalModifier = onClick?.let { baseModifier.clickable(enabled) { it() } } ?: baseModifier

    Row(
        modifier = finalModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.let {
            Spacer(Modifier.width(SMALL_LARGE_SPACER.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .alpha(if (enabled) 1f else DISABLED_OPACITY),
                contentAlignment = Alignment.Center
            ) {
                it()
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = SMALL_LARGE_SPACER.dp)
                .alpha(if (enabled) 1f else DISABLED_OPACITY)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.prefTitle,
                modifier = Modifier
                    .padding(
                        top = SMALL_LARGE_SPACER.dp,
                        bottom = (if (summary == null) SMALL_LARGE_SPACER.dp else 2.dp)
                    )
            )

            if (summary != null) {
                Summary(
                    text = summary,
                    modifier = Modifier.padding(bottom = SMALL_LARGE_SPACER.dp)
                )
            }
        }
        trailingIcon?.let {
            Box(
                modifier = Modifier
                    .padding(end = SMALL_SPACER.dp)
                    .size(48.dp)
                    .alpha(if (enabled) 1f else DISABLED_OPACITY),
                contentAlignment = Alignment.Center
            ) {
                it()
            }
        }
    }
}


@Composable
fun ChevronRight() {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
    )
}


/** A heading with no horizontal padding */
@Composable
fun BaseHeading(
    modifier: Modifier,
    text: String
) {
    Text(
        modifier = modifier,
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium
    )
}


/** A heading with 16dp horizontal padding */
@Composable
fun Heading(modifier: Modifier = Modifier, text: String) {
    BaseHeading(modifier.padding(horizontal = SMALL_LARGE_SPACER.dp), text)
}


/** A heading with 20dp horizontal padding */
@Composable
fun ExpressiveGroupHeading(modifier: Modifier = Modifier, text: String) {
    BaseHeading(modifier.padding(horizontal = MEDIUM_LARGE_SPACER.dp), text)
}


@Composable
fun SmallVerticalSpacer() {
    Spacer(Modifier.height(SMALL_SPACER.dp))
}


/** A vertical spacer with 12dp height. */
@Composable
fun VerticalSpacer() {
    Spacer(Modifier.height(MEDIUM_SPACER.dp))
}


/** A vertical spacer with 20dp height. */
@Composable
fun MediumLargeVerticalSpacer() {
    Spacer(Modifier.height(MEDIUM_LARGE_SPACER.dp))
}


/** A vertical spacer with 24dp height. */
@Composable
fun LargeVerticalSpacer() {
    Spacer(Modifier.height(LARGE_SPACER.dp))
}


/** A vertical spacer with the height of the navigation bar. */
@Composable
fun NavBarHeightVerticalSpacer() {
    Spacer(
        modifier = Modifier.height(
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )
    )
}


@Composable
fun HorizontallyScrollingChipsWithLabels(
    modifier: Modifier = Modifier,
    labels: List<String>,
    content: List<List<@Composable () -> Unit>>
) {
    if (labels.size != content.size) {
        throw IllegalArgumentException(
            "labels and content lists must be the same size. " +
            "labels: ${labels.size}, content: ${content.size}"
        )
    }
    if (labels.isEmpty()) return
    val rows = labels.zip(content)

    Surface(
        modifier = modifier,
        shape = largerShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    start = MEDIUM_LARGE_SPACER.dp,
                    top = SMALL_SPACER.dp,
                    bottom = SMALL_SPACER.dp
                )
                .height(CHIP_TOTAL_HEIGHT * labels.size)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxHeight(),
            ) {
                for (item in rows) {
                    Text(
                        text = item.first,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(CHIP_TOTAL_HEIGHT * labels.size - CHIP_TOTAL_VERTICAL_PADDING)
                    .padding(start = VERTICAL_DIVIDER_SPACING.dp)
            )

            LazyColumn(
                userScrollEnabled = false,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                contentPadding = PaddingValues(start = VERTICAL_DIVIDER_SPACING.dp, end = SMALL_LARGE_SPACER.dp)
            ) {
                for (item in rows) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp)) {
                            item.second.forEach { it() }
                        }
                    }
                }
            }
        }
    }
}


val filterChipSolidColor: SelectableChipColors
    @Composable
    get() = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )


fun String.pluralise(count: Int, pluralised: String) : String {
    return if (count == 1) this else pluralised
}


suspend fun copyText(
    context: Context,
    clipboard: Clipboard,
    text: String,
    message: String = "Copied to clipboard"
) {
    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, text)))
    // Android 13 has its own text copied popup but we'll ignore that for now
    withContext(Dispatchers.Main) {
        showToast(context, message)
    }
}


/** A FilterChip that supports both click and long click actions.
 *  @param modifier The modifier to be applied to the chip.
 *  @param label The composable content to be displayed inside the chip.
 *  @param selected Whether the chip is selected or not.
 *  @param warning Whether the chip is in a warning state. If true, will override `selected`.
 *  @param onLongClick The action to be performed when the chip is long clicked.
 *  @param onClick The action to be performed when the chip is clicked.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CombinedClickableFilterChip(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    selected: Boolean = true,
    warning: Boolean = false,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    CombinedClickableAction(
        interactionSource = interactionSource,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        FilterChip(
            onClick = onClick,
            selected = selected && !warning,
            label = label,
            modifier = modifier,
            interactionSource = interactionSource,
            colors = if (!warning) {
                filterChipSolidColor
            } else {
                FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            },
            border = null
        )
    }
}


@Composable
private fun SpacePaddedChips(
    desiredPaddingDp: Int = 16,
    content: @Composable () -> Unit
) {
    Spacer(Modifier.width((desiredPaddingDp - CHIP_SPACING).coerceAtLeast(0).dp))
    content()
    Spacer(Modifier.width((desiredPaddingDp - CHIP_SPACING).coerceAtLeast(0).dp))
}


@Composable
fun SearchHistoryListItem(
    item: SearchHistoryEntry,
    onContainerClick: () -> Unit
) {
    /* We share the interactionSource between the result's Surface and the touch-blocking Box.
       This allows the ripple animation to cover the whole thing while simultaneously allowing the
       touch blocker to still work. */
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .weight(1f, true)
                .fillMaxHeight()
                .clip(largerShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) {
                    onContainerClick()
                }
        ) {
            Column(Modifier.padding(vertical = SMALL_SPACER.dp)) {
                /* We're preventing the chips from consuming touch actions by placing the chips
                   column inside a Box, and then placing an invisible composable of the same
                   size in the same box. This invisible composable sits on top of the column
                   and effectively steals the touches. */
                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp)) {
                            SpacePaddedChips {
                                for (t in item.tags) {
                                    FilterChip(
                                        onClick = { },
                                        label = { Text(t.value) },
                                        colors = filterChipSolidColor,
                                        border = null,
                                        selected = !t.isExcluded
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp)) {
                            val allowedRatings = availableRatingsForSource(item.source)
                            SpacePaddedChips {
                                for (r in item.ratings) {
                                    if (r !in allowedRatings) continue
                                    FilterChip(
                                        onClick = { },
                                        label = { Text(r.label) },
                                        selected = true
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = interactionSource, // Shared with the main Surface
                            indication = null // The Surface will show the ripple so we don't need one here
                        ) {
                            onContainerClick()
                        }
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .clip(largerShape)
                .clickable { onContainerClick() }
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .clickable { scope.launch { context.prefs.removeSearchHistoryEntry(item) } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitledModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets.only(WindowInsetsSides.Horizontal) },
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars),
        sheetState = sheetState,
        contentWindowInsets = contentWindowInsets
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(SMALL_LARGE_SPACER.dp)
        )
        VerticalSpacer()
        content()
    }
}


@Composable
fun HorizontalFloatingToolbar(
    modifier: Modifier = Modifier,
    floatingActionButton: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit
) {
    // TODO: When Material3 1.4 drops, switch to the native implementation
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape,
            modifier = Modifier.height(64.dp),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = SMALL_SPACER.dp)
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(TINY_SPACER.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }
        floatingActionButton?.let {
            Spacer(Modifier.width(SMALL_SPACER.dp))
            it()
        }
    }
}


@Composable
fun ExpressiveTagEntryContainer(
    modifier: Modifier = Modifier,
    label: String,
    supportingLabel: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    position: ListItemPosition,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(
                RoundedCornerShape(
                    animateTopCornerSizeForPosition(position),
                    animateTopCornerSizeForPosition(position),
                    animateBottomCornerSizeForPosition(position),
                    animateBottomCornerSizeForPosition(position)
                )
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick != null) { onClick!!() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = SMALL_LARGE_SPACER.dp)
                .weight(1f, true)
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                lineHeight = 17.sp,
                overflow = TextOverflow.Ellipsis
            )

            supportingLabel?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailingContent?.let {
            Row(Modifier.padding(end = SMALL_SPACER.dp)) {
                it()
            }
        }
    }
}


/** A higher level expressive container.
    This has medium width horizontal padding by default so acts as a drop-in container ideal for
    placement in columns and pairs well with the [ExpressiveGroupHeading]. */
@Composable
fun ExpressiveContainer(
    modifier: Modifier = Modifier,
    position: ListItemPosition,
    content: @Composable () -> Unit
) {
    BasicExpressiveContainer(
        modifier = modifier.padding(horizontal = MEDIUM_SPACER.dp),
        position = position,
        content = content
    )
}


/** A lower level expressive container.
    This does not add padding by default so allows for more flexible placement. */
@Composable
fun BasicExpressiveContainer(
    modifier: Modifier = Modifier,
    position: ListItemPosition,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(
            topStart = animateTopCornerSizeForPosition(position),
            topEnd = animateTopCornerSizeForPosition(position),
            bottomStart = animateBottomCornerSizeForPosition(position),
            bottomEnd = animateBottomCornerSizeForPosition(position)
        )
    ) {
        content()
    }
}


interface ExpressiveGroupScope {
    fun item(content: @Composable () -> Unit)
}


private class PreferencesGroupScopeImpl : ExpressiveGroupScope {
    val items = mutableListOf<@Composable () -> Unit>()

    override fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}


@Composable
fun ExpressiveGroup(
    title: String? = null,
    content: ExpressiveGroupScope.() -> Unit
) {
    val scope = PreferencesGroupScopeImpl()
    scope.content()

    title?.let {
        ExpressiveGroupHeading(
            modifier = Modifier.padding(bottom = SMALL_SPACER.dp),
            text = title
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        scope.items.forEachIndexed { index, itemContent ->
            ExpressiveContainer(
                position = when {
                    scope.items.size == 1 -> ListItemPosition.SINGLE_ELEMENT
                    index == 0 -> ListItemPosition.TOP
                    index == scope.items.lastIndex -> ListItemPosition.BOTTOM
                    else -> ListItemPosition.MIDDLE
                }
            ) {
                itemContent()
            }
        }
    }
}


@Composable
fun BasicExpressiveGroup(
    title: String? = null,
    content: ExpressiveGroupScope.() -> Unit
) {
    val scope = PreferencesGroupScopeImpl()
    scope.content()

    title?.let {
        BaseHeading(
            modifier = Modifier.padding(
                start = SMALL_SPACER.dp,
                end = SMALL_SPACER.dp,
                bottom = SMALL_SPACER.dp
            ),
            text = title
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        scope.items.forEachIndexed { index, itemContent ->
            BasicExpressiveContainer(
                position = when {
                    scope.items.size == 1 -> ListItemPosition.SINGLE_ELEMENT
                    index == 0 -> ListItemPosition.TOP
                    index == scope.items.lastIndex -> ListItemPosition.BOTTOM
                    else -> ListItemPosition.MIDDLE
                }
            ) {
                itemContent()
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CombinedClickableAction(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        content()
        Box(
            modifier = Modifier
                .matchParentSize()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
data class PullToRefreshController(
    val state: PullToRefreshState,
    val indicator: @Composable BoxScope.(PullToRefreshController) -> Unit,
    private val refreshCallback: suspend () -> Unit
) {
    /* We are using the main thread because this is the thread that animations must run on.
       In this case it's for showing/hiding the indicator.
       We'll explicitly use the IO thread when doing the callback. */
    private val scope = CoroutineScope(AndroidUiDispatcher.Main)
    var isRefreshing by mutableStateOf(false)
        private set

    private fun startRefreshing(animate: Boolean) {
        if (isRefreshing) return
        if (animate) {
            scope.launch { state.animateToThreshold() }
        }
        isRefreshing = true
    }

    private fun stopRefreshing(animate: Boolean) {
        if (!isRefreshing) return
        if (animate) {
            scope.launch { state.animateToHidden() }
        }
        isRefreshing = false
    }

    fun refresh(animate: Boolean = false) {
        startRefreshing(animate)
        scope.launch(Dispatchers.IO) {
            refreshCallback()
        }.invokeOnCompletion {
            stopRefreshing(animate)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberPullToRefreshController(
    state: PullToRefreshState = rememberPullToRefreshState(),
    indicator: @Composable BoxScope.(PullToRefreshController) -> Unit = {
        PullToRefreshControllerDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            controller = it
        )
    },
    onRefresh: suspend () -> Unit = { }
): PullToRefreshController {
    return remember {
        PullToRefreshController(
            state = state,
            indicator = indicator,
            refreshCallback = onRefresh
        )
    }
}


object PullToRefreshControllerDefaults {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Indicator(
        modifier: Modifier = Modifier,
        controller: PullToRefreshController,
    ) {
        PullToRefreshDefaults.Indicator(
            state = controller.state,
            isRefreshing = controller.isRefreshing,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}


/** A button that scrolls image grids back to the top.

    Set [animate] to false to scroll immediately without animation. This option is mostly intended
    to work around what I can only assume is a Compose bug whereby the scrolling animation is jumpy
    when there is a full width item in the grid (like the filter). */
@SuppressLint("FrequentlyChangingValue")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollToTopArrow(
    staggeredGridState: LazyStaggeredGridState,
    uniformGridState: LazyGridState,
    animate: Boolean = true,
    alsoOnClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val isScrolledPastFirst by remember(staggeredGridState, uniformGridState) {
        derivedStateOf { staggeredGridState.firstVisibleItemIndex != 0 || uniformGridState.firstVisibleItemIndex != 0 }
    }

    AnimatedVisibility(
        enter = fadeIn(),
        exit = fadeOut(),
        visible = isScrolledPastFirst
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    if (animate) {
                        staggeredGridState.animateScrollToItem(0)
                    } else {
                        staggeredGridState.scrollToItem(0)
                    }
                }
                scope.launch {
                    if (animate) {
                        uniformGridState.animateScrollToItem(0)
                    } else {
                        uniformGridState.scrollToItem(0)
                    }
                }
                alsoOnClick?.invoke()
            }
        ) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Scroll to top")
        }
    }
}


// Sometimes it's okay to break spec :3
/** An button intended to be used inside a list (of buttons).
 *
 *  @param label The composable content to be displayed inside the button.
 *  @param icon An optional icon to display before the label.
 *  @param modifier The modifier to be applied to the button.
 *  @param enabled Whether or not the button is enabled.
 *  @param colors The colors to be used for the button. If null, the default light colours will be used.
 *  @param position The position of the button in the list, used to determine its shape.
 *  @param horizontalArrangement The horizontal arrangement of the button's content. If [icon] is null, defaults to [Arrangement.Center], otherwise defaults to [Arrangement.Start].
 *  @param onClick The action to be performed when the button is clicked.
 */
@Composable
fun ButtonListItem(
    label: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors? = null,
    position: ListItemPosition,
    horizontalArrangement: Arrangement.Horizontal = if (icon == null) Arrangement.Center else Arrangement.Start,
    onClick: () -> Unit,
) {
    /* We're going to use the system colours but always the light variants.
       Android's permission dialogs used to do this until they made them subjectively worse.
       https://www.androidauthority.com/android-16-permissions-dialog-redesign-3573255/ */
    BreadboardTheme(darkTheme = false) {
       val colors = colors ?: ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Surface(
            onClick = onClick,
            modifier = modifier.then(
                Modifier.alpha(if (enabled) 1f else DISABLED_OPACITY)
            ),
            enabled = enabled,
            color = colors.containerColor,
            contentColor = colors.contentColor,
            shape = RoundedCornerShape(
                topStart = animateTopCornerSizeForPosition(position),
                topEnd = animateTopCornerSizeForPosition(position),
                bottomStart = animateBottomCornerSizeForPosition(position),
                bottomEnd = animateBottomCornerSizeForPosition(position)
            )
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .padding(horizontal = MEDIUM_LARGE_SPACER.dp, vertical = SMALL_SPACER.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = horizontalArrangement
            ) {
                icon?.let {
                    it()
                    Spacer(Modifier.width(SMALL_LARGE_SPACER.dp))
                }

                label()
            }
        }
    }
}


/** An button intended to be used inside a list (of buttons).
 *
 *  @param label The text to be displayed inside the button.
 *  @param icon An optional icon to display before the label.
 *  @param modifier The modifier to be applied to the button.
 *  @param enabled Whether or not the button is enabled.
 *  @param colors The colors to be used for the button. If null, the default light colours will be used.
 *  @param position The position of the button in the list, used to determine its shape.
 *  @param horizontalArrangement The horizontal arrangement of the button's label. If [icon] is null, defaults to [Arrangement.Center], otherwise defaults to [Arrangement.Start].
 *  @param onClick The action to be performed when the button is clicked.
 */
@Composable
fun ButtonListItem(
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors? = null,
    position: ListItemPosition,
    horizontalArrangement: Arrangement.Horizontal = if (icon == null) Arrangement.Center else Arrangement.Start,
    onClick: () -> Unit,
) {
    ButtonListItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        icon = icon?.let { {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        } },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        position = position,
        horizontalArrangement = horizontalArrangement,
        onClick = onClick
    )
}


fun <T> bouncyAnimationSpec(): FiniteAnimationSpec<T> = spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)


@Composable
fun rememberIsBlurEnabled(): Boolean {
    val prefs = LocalPreferences.current
    val context = LocalContext.current
    val powerManager = context.getSystemService<PowerManager>()!!
    // TODO: Maybe also check if the Window-level blurs developer option is enabled?
    return remember(powerManager.isPowerSaveMode) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS)
            && !powerManager.isPowerSaveMode
    }
}


val bottomAppBarAndNavBarHeight: Dp
    @Composable
    get() = BOTTOM_APP_BAR_HEIGHT.dp + navBarHeight

val navBarHeight: Dp
    @Composable
    get() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

val largerShapeCornerSize = LARGE_CORNER_DP.dp
val largerShape = RoundedCornerShape(largerShapeCornerSize)
