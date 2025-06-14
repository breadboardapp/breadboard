package moe.apex.rule34.ui.theme

import android.app.Activity
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

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
data class ExtraColours(
    val outlineStrong: Color
)


val LocalExtraColours = staticCompositionLocalOf {
    ExtraColours(
        outlineStrong = Color.Unspecified
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

    val extraColours = ExtraColours(outlineStrong = colorScheme.outline.copy())
    colorScheme = colorScheme.copy(
        outline = colorScheme.outlineVariant,
        background = colorScheme.surfaceContainerLow,
        surface = colorScheme.surfaceContainerLow,
        surfaceContainerLowest = colorScheme.surfaceContainerLow,
        surfaceContainerLow = colorScheme.surfaceContainer,
    )
    val view = LocalView.current
    val systemUiController = rememberSystemUiController()
    val darkIcons = !darkTheme
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
            systemUiController.setStatusBarColor(Color.Transparent, darkIcons)
            systemUiController.setNavigationBarColor(Color.Transparent)
        }
    }

    CompositionLocalProvider(LocalExtraColours provides extraColours) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}


object BreadboardTheme {
    val colors: ExtraColours
        @Composable
        get() = LocalExtraColours.current
}
