package com.artflow.app.ui.editor

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artflow.app.model.StyleCatalog
import com.artflow.app.ui.editor.components.BlendSliderBar
import com.artflow.app.ui.editor.components.StyleCarousel
import com.artflow.app.ui.editor.components.ViewportCanvas
import com.artflow.app.ui.theme.PrimaryNeon
import com.artflow.app.ui.theme.SecondaryCyan
import com.artflow.app.ui.theme.StudioDark
import com.artflow.app.ui.theme.StudioSurface
import com.artflow.app.ui.theme.TextMuted
import com.artflow.app.ui.theme.TextPrimary

/**
 * Main ArtFlow Studio Editor interface.
 */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    // Handle export success notification
    LaunchedEffect(uiState) {
        if (uiState is EditorUiState.Success) {
            val success = uiState as EditorUiState.Success
            if (success.exportedUri != null) {
                Toast.makeText(context, "Saved exact artwork to Pictures/ArtFlow!", Toast.LENGTH_SHORT).show()
                viewModel.dismissExport()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Studio Header Bar
            EditorTopBar(
                hasImage = uiState !is EditorUiState.Idle,
                onPickImage = onPickImage,
                onExport = { viewModel.exportArtwork() },
                onReset = { viewModel.reset() }
            )

            // 2. Central Interactive Canvas Viewport
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is EditorUiState.Idle -> {
                        EmptyCanvasPlaceholder(onPickImage = onPickImage)
                    }
                    is EditorUiState.Processing -> {
                        // Keep previous image visible while rendering with subtle indicator
                        ViewportCanvas(
                            bitmap = state.previousStylizedBitmap ?: state.canvasBitmap,
                            selectedStyleId = state.style.id
                        )
                        ProcessingBadge(message = state.statusMessage)
                    }
                    is EditorUiState.Success -> {
                        ViewportCanvas(
                            bitmap = state.compositePreview,
                            selectedStyleId = state.selectedStyle.id
                        )
                    }
                    is EditorUiState.Error -> {
                        ErrorDisplay(message = state.message, onRetry = onPickImage)
                    }
                }
            }

            // 3. Bottom Controls Panel (Only visible when image is loaded)
            AnimatedVisibility(
                visible = uiState is EditorUiState.Success || uiState is EditorUiState.Processing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, StudioDark)
                            )
                        )
                        .padding(bottom = 12.dp)
                ) {
                    val currentSuccess = uiState as? EditorUiState.Success
                    val hasMask = currentSuccess?.segmentationMask != null

                    // Dual Sliders for Intensity & Subject Preserve
                    BlendSliderBar(
                        settings = settings,
                        onSettingsChanged = { viewModel.updateSettings(it) },
                        hasSegmentationMask = hasMask,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 120Hz Style Carousel (50 Styles)
                    val selectedStyle = when (val state = uiState) {
                        is EditorUiState.Success -> state.selectedStyle
                        is EditorUiState.Processing -> state.style
                        else -> StyleCatalog.defaultStyle
                    }

                    StyleCarousel(
                        selectedStyle = selectedStyle,
                        onStyleSelected = { viewModel.selectStyle(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    hasImage: Boolean,
    onPickImage: () -> Unit,
    onExport: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(SecondaryCyan)
            )
            Text(
                text = " ARTFLOW",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasImage) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Canvas",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onExport,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " Save",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onPickImage,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " Open Photo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCanvasPlaceholder(onPickImage: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(StudioSurface)
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            tint = PrimaryNeon,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Offline On-Device Neural Art Studio",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "50 Neural Styles • 1024px Studio Canvas • Universal GPU Acceleration",
            fontSize = 12.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onPickImage,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Select Photo from Gallery", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProcessingBadge(message: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC181924))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                color = SecondaryCyan,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorDisplay(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFFFF5252),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurface)
        ) {
            Text("Try Another Photo")
        }
    }
}
