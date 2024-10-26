package moe.apex.rule34.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
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
const val CHIP_SPACING = 10


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
