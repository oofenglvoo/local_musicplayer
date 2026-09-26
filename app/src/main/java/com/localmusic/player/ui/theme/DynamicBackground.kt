package com.localmusic.player.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.localmusic.player.data.BackgroundMode

/**
 * Immersive background for the player (and other full-screen surfaces).
 *
 * When no explicit [primary]/[secondary] color is provided the gradient derives
 * from the current theme: dark themes keep the deep neutral gradient, light
 * themes use a soft light gradient. The scrim (dim overlay) is also theme-aware
 * so it never darkens a light background, unless the caller passes an explicit
 * dark gradient (e.g. a user-chosen solid color).
 */
@Composable
fun DynamicBackground(
    mode: BackgroundMode,
    image: Any? = null,
    fallbackArtwork: Any? = null,
    primary: Color = Color.Unspecified,
    secondary: Color = Color.Unspecified,
    blur: Int = 42,
    dim: Int = 72,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme() ||
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    val hasExplicitColors = primary.isSpecified && secondary.isSpecified

    val topColor = if (primary.isSpecified) primary
    else if (darkTheme) Color(0xFF15121C) else Color(0xFFEFF8F2)
    val bottomColor = if (secondary.isSpecified) secondary
    else if (darkTheme) Color(0xFF332044) else Color(0xFFF7F8F7)

    val gradient = Brush.linearGradient(listOf(topColor, bottomColor))

    // The user explicitly picked dark colors: keep the darkening scrim.
    // Otherwise the scrim follows the theme so light themes stay bright.
    val darkGradient = topColor.luminance() < 0.5f || bottomColor.luminance() < 0.5f
    val scrimColor = if (hasExplicitColors && darkGradient) Color.Black else Color.White
    val scrimAlpha = if (darkGradient) dim.coerceIn(0, 95) / 100f
    else dim.coerceIn(0, 95) / 200f

    Box(modifier.fillMaxSize().background(gradient)) {
        val model = when (mode) {
            BackgroundMode.LOCAL_IMAGE -> image
            BackgroundMode.ARTWORK -> fallbackArtwork
            else -> null
        }
        if (model != null) AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(blur.coerceAtLeast(0).dp),
        )
        if (mode == BackgroundMode.GRADIENT || mode == BackgroundMode.SOLID || model == null) {
            Box(Modifier.fillMaxSize().background(gradient))
        }
        if (scrimAlpha > 0f) {
            Box(Modifier.fillMaxSize().background(scrimColor.copy(alpha = scrimAlpha)))
        }
    }
}
