package moe.apex.rule34.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val DarkColorScheme = darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40

        /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)


@Immutable
data class BreadboardColors(
    val outlineStrong: Color,
    val titleBar: Color,
)


val LocalBreadboardColors = staticCompositionLocalOf {
    BreadboardColors(
        outlineStrong = Color.Unspecified,
        titleBar = Color.Unspecified,
    )
}


@Composable
fun BreadboardTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = true,
        content: @Composable () -> Unit
) {
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extraColours = BreadboardColors(
        outlineStrong = colorScheme.outline.copy(),
        titleBar = if (darkTheme) {
            colorScheme.surfaceContainer
        } else {
            colorScheme.surfaceContainerHigh
        }.copy()
    )

    colorScheme = colorScheme.copy(
        outline = colorScheme.outlineVariant,
        background = if (darkTheme) colorScheme.surfaceContainerLow else colorScheme.surfaceContainer,
        surface = if (darkTheme) colorScheme.surfaceContainerLow else colorScheme.surfaceContainer,
        surfaceContainerLowest = if (darkTheme) colorScheme.surfaceContainerLow else colorScheme.surfaceContainerHigh,
        surfaceContainerLow = if (darkTheme) colorScheme.surfaceContainer else colorScheme.surfaceContainerLow,
        surfaceContainer = if (darkTheme) colorScheme.surfaceContainer else colorScheme.surfaceContainerLow,
        surfaceContainerHigh = if (darkTheme) colorScheme.surfaceContainerHigh else colorScheme.surfaceBright,
        surfaceContainerHighest = if (darkTheme) colorScheme.surfaceContainerHighest else colorScheme.surfaceContainerHigh
    )

    CompositionLocalProvider(LocalBreadboardColors provides extraColours) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}


object BreadboardTheme {
    val colors: BreadboardColors
        @Composable
        get() = LocalBreadboardColors.current
}
