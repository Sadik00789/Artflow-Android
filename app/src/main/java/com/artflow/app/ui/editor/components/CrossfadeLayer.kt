package com.artflow.app.ui.editor.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * Animated hardware crossfade layer between rendered style transitions.
 * Keys on [selectedStyleId] so that slider adjustments (which update bitmap within the same style)
 * update instantly without re-triggering repeated 280ms crossfade transitions.
 */
@Composable
fun CrossfadeLayer(
    bitmap: Bitmap?,
    selectedStyleId: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    Crossfade(
        targetState = selectedStyleId,
        animationSpec = tween(durationMillis = 280),
        modifier = modifier,
        label = "ArtworkCrossfade"
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Neural Art Preview",
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
