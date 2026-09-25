package com.localmusic.player.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/**
 * Soft mint-to-white vertical wash used by the list oriented screens (home, local,
 * playlists, profile) so they all share the same light backdrop.
 */
@Composable
fun PageBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppPageTop,
                        AppPageBottom,
                    ),
                ),
            ),
    ) {
        content()
    }
}
