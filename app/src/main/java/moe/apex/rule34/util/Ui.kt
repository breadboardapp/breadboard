package moe.apex.rule34.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.apex.rule34.image.Image
import moe.apex.rule34.largeimageview.LargeImageView


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
    navController: NavController? = null
) {
    LargeTopAppBar(
        title = { Text(title, overflow = TextOverflow.Ellipsis) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (navController != null) {
                IconButton(
                    onClick = { navController.navigateUp() }
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
    shouldShowLargeImage: MutableState<Boolean>,
    initialPage: Int,
    allImages: List<Image>,
    bottomBarVisibleState: MutableState<Boolean>? = null
) {
    LaunchedEffect(shouldShowLargeImage.value) {
        if (bottomBarVisibleState != null) {
            bottomBarVisibleState.value = !shouldShowLargeImage.value
        }
    }

    AnimatedVisibility(
        visible = shouldShowLargeImage.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        LargeImageView(
            initialPage,
            shouldShowLargeImage,
            allImages
        )
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
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TitleBar(title, scrollBehavior)
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