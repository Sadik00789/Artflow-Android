package com.artflow.app.ui.editor.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.artflow.app.ui.theme.StudioDark

/**
 * High-performance interactive viewport canvas supporting pinch-to-zoom, panning,
 * double-tap reset, and smooth hardware crossfade between stylization renders.
 */
@Composable
fun ViewportCanvas(
    bitmap: Bitmap?,
    selectedStyleId: String? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1.0f, 5.0f)
        if (scale > 1.0f) {
            val maxOffsetX = 600f * (scale - 1f)
            val maxOffsetY = 800f * (scale - 1f)
            val newX = (offset.x + panChange.x * scale).coerceIn(-maxOffsetX, maxOffsetX)
            val newY = (offset.y + panChange.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
            offset = Offset(newX, newY)
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioDark)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Reset zoom and pan on double-tap
                        scale = 1.0f
                        offset = Offset.Zero
                    }
                )
            }
            .transformable(state = transformState),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            CrossfadeLayer(
                bitmap = bitmap,
                selectedStyleId = selectedStyleId,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
