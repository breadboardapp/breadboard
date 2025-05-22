package moe.apex.rule34.util

import android.app.Activity
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.rule34.history.SearchHistoryEntry
import moe.apex.rule34.image.Image
import moe.apex.rule34.largeimageview.LargeImageView
import moe.apex.rule34.prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


const val NAV_BAR_HEIGHT = 80
const val CHIP_SPACING = 12
private const val VERTICAL_DIVIDER_SPACING = 32
private val CHIP_TOTAL_VERTICAL_PADDING = 16.dp
private val CHIP_TOTAL_HEIGHT = FilterChipDefaults.Height + 16.dp


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TitleBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior?,
    navController: NavController? = null,
    additionalActions: @Composable RowScope.() -> Unit = { }
) {
    val context = LocalContext.current
    LargeTopAppBar(
        title = { Text(title, overflow = TextOverflow.Ellipsis) },
        scrollBehavior = scrollBehavior,
        actions = additionalActions,
        navigationIcon = {
            if (navController != null) {
                IconButton(
                    onClick = {
                        if (navController.previousBackStackEntry != null) {
                            navController.navigateUp()
                        } else {
                            (context as Activity).finish()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Home"
                    )
                }
            }
        }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenScaffold(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    additionalActions: @Composable RowScope.() -> Unit = { },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TitleBar(title, scrollBehavior, additionalActions = additionalActions)
        }
    ) {
        val lld = LocalLayoutDirection.current
        val newPadding = PaddingValues(
            start = it.calculateStartPadding(lld),
            end = it.calculateEndPadding(lld),
            top = it.calculateTopPadding(),
            bottom = it.calculateBottomPadding() + NAV_BAR_HEIGHT.dp
        )
        content(newPadding)
    }
}


@Composable
fun Heading(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier.padding(horizontal = 16.dp),
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium
    )
}


/** A vertical spacer with 12dp height. */
@Composable
fun VerticalSpacer() {
    Spacer(Modifier.height(12.dp))
}


/** A vertical spacer with 24dp height. */
@Composable
fun LargeVerticalSpacer() {
    Spacer(Modifier.height(24.dp))
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
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
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
                        lineHeight = MaterialTheme.typography.titleMedium.fontSize
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(CHIP_TOTAL_HEIGHT * labels.size - CHIP_TOTAL_VERTICAL_PADDING)
                    .padding(start = VERTICAL_DIVIDER_SPACING.dp)
            )

            Column(Modifier.horizontalScroll(rememberScrollState())) {
                for (item in rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp)) {
                        Spacer(Modifier.width((VERTICAL_DIVIDER_SPACING - CHIP_SPACING).dp))
                        for (chip in item.second) {
                            chip()
                        }
                        Spacer(Modifier.width((16 - CHIP_SPACING).dp))
                    }
                }
            }
        }
    }
}


fun String.pluralise(count: Int, pluralised: String) : String {
    return if (count == 1) this else pluralised
}


fun copyText(
    context: Context,
    clipboardManager: ClipboardManager,
    text: String,
    message: String = "Copied to clipboard"
) {
    clipboardManager.setText(AnnotatedString(text))
    // if (SDK_INT < VERSION_CODES.TIRAMISU) { // Android 13 has its own text copied popup
    showToast(context, message)
    // }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CombinedClickableSuggestionChip(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    label: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box {
        SuggestionChip(onClick = { }, label = label, interactionSource = interactionSource)
        Box(
            modifier = Modifier
                .matchParentSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    interactionSource = interactionSource,
                    indication = null
                )
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

    val is24h = DateFormat.is24HourFormat(context)
    val timeFormat = if (is24h) "HH:mm" else "h:mm a"
    val date = Date(item.timestamp)
    val formatter = SimpleDateFormat("dd MMM $timeFormat", Locale.getDefault())
    val formattedDate = formatter.format(date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .weight(1f, true)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp, 4.dp, 4.dp, 16.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) { onContainerClick() }
        ) {
            Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                Heading(
                    modifier = Modifier.padding(start = 4.dp),
                    text = "$formattedDate  \u2022  ${item.source.description}"
                )
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
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 4.dp))
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
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
