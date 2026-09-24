package com.localmusic.player.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.localmusic.player.data.BackgroundMode

@Composable
fun DynamicBackground(
    mode: BackgroundMode,
    image: Any? = null,
    fallbackArtwork: Any? = null,
    primary: Color = Color(0xFF15121C),
    secondary: Color = Color(0xFF332044),
    blur: Int = 42,
    dim: Int = 72,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Brush.linearGradient(listOf(primary, secondary)))) {
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
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(primary, secondary))))
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim.coerceIn(0, 95) / 100f)))
    }
}
