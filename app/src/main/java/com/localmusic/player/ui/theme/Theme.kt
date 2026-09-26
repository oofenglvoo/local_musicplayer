package com.localmusic.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.localmusic.player.data.ThemeMode

val AppAccent = Color(0xFF00D98B)
val AppAccentDark = Color(0xFF00A76B)
val AppPageTop = Color(0xFFEFF8F2)
val AppPageBottom = Color(0xFFF7F8F7)
val AppInk = Color(0xFF1A1C1B)
val AppInkSoft = Color(0xFF7A807C)
val AppSurfaceSoft = Color(0xFFF0F2F1)

private val LightScheme = lightColorScheme(
    primary = AppAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F7E7),
    onPrimaryContainer = Color(0xFF00392A),
    secondary = AppAccentDark,
    onSecondary = Color.White,
    tertiary = Color(0xFF4A9E7F),
    background = AppPageBottom,
    onBackground = AppInk,
    surface = Color.White,
    onSurface = AppInk,
    surfaceVariant = AppSurfaceSoft,
    onSurfaceVariant = AppInkSoft,
    outline = Color(0xFFC9CFCB),
    outlineVariant = Color(0xFFE2E7E4),
    error = Color(0xFFE5484D),
)

private val DarkScheme = darkColorScheme(
    primary = AppAccent,
    onPrimary = Color(0xFF00281C),
    primaryContainer = Color(0xFF00553D),
    onPrimaryContainer = Color(0xFFB6F5DB),
    secondary = AppAccentDark,
    tertiary = Color(0xFF7FCBAE),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE7EBE8),
    surface = Color(0xFF161B19),
    onSurface = Color(0xFFE7EBE8),
    surfaceVariant = Color(0xFF222825),
    onSurfaceVariant = Color(0xFF9BA5A0),
    outline = Color(0xFF3E4643),
    outlineVariant = Color(0xFF2A312E),
)

private val BlackScheme = darkColorScheme(
    primary = AppAccent,
    onPrimary = Color(0xFF00281C),
    primaryContainer = Color(0xFF00553D),
    onPrimaryContainer = Color(0xFFB6F5DB),
    secondary = AppAccentDark,
    tertiary = Color(0xFF7FCBAE),
    background = Color.Black,
    onBackground = Color(0xFFE7EBE8),
    surface = Color.Black,
    onSurface = Color(0xFFE7EBE8),
    surfaceVariant = Color(0xFF111513),
    onSurfaceVariant = Color(0xFF9BA5A0),
    outline = Color(0xFF3E4643),
    outlineVariant = Color(0xFF222725),
)

/**
 * Applies the selected theme. [ThemeMode.SYSTEM] follows the system dark mode.
 * When [dynamicColor] is enabled on Android 12+ the wallpaper palette is used,
 * otherwise [seedColor] (when present) derives a tonal scheme.
 */
@Composable
fun LocalMusicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    seedColor: Int? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        themeMode == ThemeMode.BLACK -> BlackScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        seedColor != null -> {
            val scheme = if (darkTheme) DarkScheme else LightScheme
            scheme.copy(
                primary = Color(seedColor),
                secondary = Color(seedColor),
            )
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
