package moe.apex.breadboard.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.core.graphics.ColorUtils
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import moe.apex.breadboard.navigation.AboutSettings
import moe.apex.breadboard.navigation.ContentSettings
import moe.apex.breadboard.navigation.DataSettings
import moe.apex.breadboard.navigation.ExperimentalSettings
import moe.apex.breadboard.navigation.GeneralSettings
import moe.apex.breadboard.navigation.LayoutSettings
import moe.apex.breadboard.util.ChevronRight
import moe.apex.breadboard.util.LazyExpressiveGroup
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.TitleSummary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavHostController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    MainScreenScaffold(
        title = "Settings",
        scrollBehavior = scrollBehavior,
        additionalActions = {
            var isDropdownVisible by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { isDropdownVisible = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More"
                    )
                }
                DropdownMenu(
                    expanded = isDropdownVisible,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                    onDismissRequest = { isDropdownVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Experimental features") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = "Experimental features"
                            )
                        },
                        onClick = {
                            isDropdownVisible = false
                            navController.navigate(ExperimentalSettings)
                        }
                    )
                }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(MEDIUM_SPACER.dp)
        ) {
            LazyExpressiveGroup(desiredTopPadding = null) {
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "General",
                        summary = "Image source, API keys, search history, and links.",
                        leadingIcon = {
                            ContainedIcon(imageVector = Icons.Rounded.Settings, label = "General")
                        },
                        trailingIcon = { ChevronRight() }
                    ) {
                        navController.navigate(GeneralSettings)
                    }
                }
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Content",
                        summary = "Blocked tags, recommendations, and video settings.",
                        leadingIcon = {
                            ContainedIcon(imageVector = Icons.Rounded.Visibility, label = "Content")
                        },
                        trailingIcon = { ChevronRight() }
                    ) {
                        navController.navigate(ContentSettings)
                    }
                }
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Behaviour and layout",
                        summary = "Start page, app layout, and image viewer actions.",
                        leadingIcon = {
                            ContainedIcon(imageVector = Icons.Rounded.Dashboard, label = "Behaviour and layout")
                        },
                        trailingIcon = { ChevronRight() }
                    ) {
                        navController.navigate(LayoutSettings)
                    }
                }
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Data and storage",
                        summary = "Download location and your Breadboard data.",
                        leadingIcon = {
                            ContainedIcon(imageVector = Icons.Rounded.Storage, label = "Data and storage")
                        },
                        trailingIcon = { ChevronRight() }
                    ) {
                        navController.navigate(DataSettings)
                    }
                }
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "About",
                        summary = "Breadboard info and licenses.",
                        leadingIcon = {
                            ContainedIcon(imageVector = Icons.Rounded.Info, label = "About")
                        },
                        trailingIcon = { ChevronRight() }
                    ) {
                        navController.navigate(AboutSettings)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContainedIcon(
    label: String,
    imageVector: ImageVector
) {
    val isDark = isSystemInDarkTheme()
    // Assign the icon a random colour hue based on its label.
    val seed = label.hashCode()
    val hue = (seed and Integer.MAX_VALUE).rem(360).toFloat()

    /* We're going for light backgrounds with a darker inner icon.
       The specific saturation and lightness depend on whether the user is in dark or light mode. */
    val (bgSat, bgLight) = if (isDark) {
        0.42f to 0.70f
    } else {
        0.65f to 0.87f
    }

    val (iconSat, iconLight) = if (isDark) {
        0.60f to 0.20f
    } else {
        0.65f to 0.27f
    }

    val bgInt = ColorUtils.HSLToColor(floatArrayOf(hue, bgSat, bgLight))
    val iconInt = ColorUtils.HSLToColor(floatArrayOf(hue, iconSat, iconLight))

    val bgColour = Color(bgInt)
    val iconTint = Color(iconInt)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColour)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = iconTint
        )
    }
}
