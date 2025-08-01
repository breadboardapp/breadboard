package moe.apex.rule34.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.rotationMatrix
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import moe.apex.rule34.BuildConfig
import moe.apex.rule34.R
import moe.apex.rule34.navigation.LibrariesSettings
import moe.apex.rule34.util.ChevronRight
import moe.apex.rule34.util.ExpressiveGroup
import moe.apex.rule34.util.LARGE_SPACER
import moe.apex.rule34.util.LargeTitleBar
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.SmallVerticalSpacer
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.TitleSummary
import moe.apex.rule34.util.openUrl
import moe.apex.rule34.util.releasePlatform
import kotlin.math.max


private data class GitHubUser(
    val name: String,
    private val urlSlug: String
) {
    val url = "https://github.com/$urlSlug"
    val avatarUrl = "$url.png"
}


private val apex2504 = GitHubUser("apex2504", "apex2504")
private val devoxin = GitHubUser("devoxin", "devoxin")


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    val cookieShape = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 9,
            innerRadius = .8f,
            rounding = CornerRounding(0.5f),
        ).transformed(rotationMatrix(-90f))
    }
    val clippable = remember { RoundedPolygonShape(cookieShape) }

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "About",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        }
    ) {
        var easterEggCounter by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            while (true) {
                delay(timeMillis = 1000)
                if (easterEggCounter != 0) {
                    easterEggCounter--
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(vertical = MEDIUM_SPACER.dp),
            verticalArrangement = Arrangement.spacedBy(LARGE_SPACER.dp)
        ) {
            item {
                var isMonochrome by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    /* The mismatch between box size and required image size is just a simple way of
                       zooming the image so the icon isn't so small. */
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(clippable)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (easterEggCounter == 5) {
                                    openUrl(context, "https://www.youtube.com/watch?v=3ZeHmdJnny4") // 🐟
                                    easterEggCounter = 0
                                } else {
                                    easterEggCounter++
                                    isMonochrome = !isMonochrome
                                }
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(
                                    if (isMonochrome) R.mipmap.ic_launcher_monochrome
                                    else R.mipmap.ic_launcher_foreground
                                )
                                .crossfade(true)
                                .build(),
                            contentDescription = "App Icon",
                            colorFilter = if (isMonochrome) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .requiredSize(112.dp)
                                .offset(y = (-2).dp),
                        )
                    }
                    SmallVerticalSpacer()
                    Text(
                        text = "Breadboard",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${releasePlatform.displayName})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )
                }
            }
            item {
                ExpressiveGroup("Maintainer") {
                    item {
                        GitHubUserContainer(apex2504)
                    }
                }
            }
            item {
                ExpressiveGroup("Original concept") {
                    item {
                        GitHubUserContainer(devoxin)
                    }
                }
            }
            item {
                ExpressiveGroup("Breadboard") {
                    item {
                        TitleSummary(
                            modifier = Modifier.fillMaxWidth(),
                            title = "GitHub",
                            summary = "Report bugs, request features, or contribute!"
                        ) { openUrl(context, "https://github.com/breadboardapp/breadboard") }
                    }
                    item {
                        TitleSummary(
                            modifier = Modifier.fillMaxWidth(),
                            title = "Third-party notices",
                            summary = "Libraries used in Breadboard",
                            trailingIcon = { ChevronRight() }
                        ) { navController.navigate(LibrariesSettings) }
                    }
                }
            }
        }
    }
}


@Composable
private fun GitHubUserContainer(user: GitHubUser) {
    val context = LocalContext.current

    TitleSummary(
        modifier = Modifier.fillMaxWidth(),
        title = user.name,
        summary = user.url,
        leadingIcon = {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.clip(CircleShape)
            )
        },
        onClick = { openUrl(context, user.url) }
    )
}

// https://developer.android.com/develop/ui/compose/quick-guides/content/clipped-image#create_a_shape
private fun RoundedPolygon.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }

private class RoundedPolygonShape(private val polygon: RoundedPolygon) : Shape {
    private val matrix: Matrix = Matrix()
    private var path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        path.rewind()
        path = polygon.toPath().asComposePath()
        matrix.reset()
        val bounds = polygon.getBounds()
        val maxDimension = max(bounds.width, bounds.height)
        matrix.scale(size.width / maxDimension, size.height / maxDimension)
        matrix.translate(-bounds.left, -bounds.top)

        path.transform(matrix)
        return Outline.Generic(path)
    }
}

