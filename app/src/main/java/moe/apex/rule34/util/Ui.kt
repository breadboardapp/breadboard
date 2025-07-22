package moe.apex.rule34.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.history.SearchHistoryEntry
import moe.apex.rule34.image.Image
import moe.apex.rule34.largeimageview.LargeImageView
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.prefTitle


private const val LARGE_CORNER_DP = 20
private const val SMALL_CORNER_DP = 4
/** 24dp spacing */
const val LARGE_SPACER = 24
/** 20dp spacing */
const val MEDIUM_LARGE_SPACER = 20
/** 12dp spacing */
const val MEDIUM_SPACER = 12
/** 8dp spacing */
const val SMALL_SPACER = 8
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
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
                contentDescription = "Discover"
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
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
        navigationIcon = { NavigationIcon(navController) },
        colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = TopAppBarDefaults.topAppBarColors().scrolledContainerColor)
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
fun AnimatedVisibilityLargeImageView(
    navController: NavController,
    shouldShowLargeImage: MutableState<Boolean>,
    initialPage: Int,
    allImages: List<Image>,
    bottomBarVisibleState: MutableState<Boolean>? = null
) {
    LaunchedEffect(shouldShowLargeImage.value) {
        if (bottomBarVisibleState != null)
            bottomBarVisibleState.value = !shouldShowLargeImage.value
    }

    AnimatedVisibility(
        visible = shouldShowLargeImage.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        key(initialPage) {
            LargeImageView(
                navController,
                shouldShowLargeImage,
                initialPage,
                allImages
            )
        }
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
        floatingActionButton = floatingActionButton,
        content = content
    )
}


/** A lower level MainScreenScaffold that allows passing in a custom top bar for more
    fine grained control over its behaviour and appearance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenScaffold(
    topAppBar: @Composable () -> Unit,
    addBottomPadding: Boolean = true,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = topAppBar,
        floatingActionButton = { floatingActionButton?.let {
            Box(Modifier.offset(y = if (addBottomPadding) -BOTTOM_APP_BAR_HEIGHT.dp else 0.dp)) {
                it()
            }
        } }
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
}




@Composable
fun Summary(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        text = text,
        color = Color.Gray,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
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
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(48.dp)) {
                it()
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .alpha(if (enabled) 1f else DISABLED_OPACITY)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.prefTitle,
                modifier = Modifier
                    .padding(
                        top = 16.dp,
                        bottom = (if (summary == null) 16.dp else 2.dp)
                    )
            )

            if (summary != null) {
                Summary(
                    text = summary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
        trailingIcon?.let {
            Box(
                modifier = Modifier
                    .padding(end = SMALL_SPACER.dp)
                    .size(48.dp)
            ) {
                it()
            }
        }
    }
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
    BaseHeading(modifier.padding(horizontal = 16.dp), text)
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = MEDIUM_LARGE_SPACER.dp, top = SMALL_SPACER.dp, bottom = SMALL_SPACER.dp)
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
                contentPadding = PaddingValues(start = VERTICAL_DIVIDER_SPACING.dp, end = 16.dp)
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CombinedClickableFilterChip(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    CombinedClickableAction(
        interactionSource = interactionSource,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        FilterChip(
            onClick = onClick,
            selected = true,
            label = label,
            modifier = modifier,
            interactionSource = interactionSource
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
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .weight(1f, true)
                .fillMaxHeight()
                .clip(RoundedCornerShape(
                    topStart = largerShapeCornerSize,
                    bottomStart = largerShapeCornerSize,
                    topEnd = 4.dp,
                    bottomEnd = 4.dp
                ))
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
                .clip(RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = largerShapeCornerSize,
                    bottomEnd = largerShapeCornerSize,
                    bottomStart = 4.dp
                ))
                .clickable { onContainerClick() }
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
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
                .padding(16.dp)
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                .padding(horizontal = 16.dp)
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
    content: @Composable ExpressiveGroupScope.() -> Unit
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
    content: @Composable ExpressiveGroupScope.() -> Unit
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


val bottomAppBarAndNavBarHeight: Dp
    @Composable
    get() = BOTTOM_APP_BAR_HEIGHT.dp + navBarHeight

val navBarHeight: Dp
    @Composable
    get() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

val largerShapeCornerSize = LARGE_CORNER_DP.dp
val largerShape = RoundedCornerShape(largerShapeCornerSize)
