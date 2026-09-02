package com.artflow.app.ui.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artflow.app.model.StylePreset
import com.artflow.app.ui.theme.PrimaryNeon
import com.artflow.app.ui.theme.StudioSurface
import com.artflow.app.ui.theme.TextMuted
import com.artflow.app.ui.theme.TextPrimary

/**
 * Individual style preset item in the horizontal carousel with selection animation and glowing border.
 */
@Composable
fun StyleThumbnailItem(
    preset: StylePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "ThumbnailScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryNeon else Color.Transparent,
        label = "BorderColor"
    )

    val shape = RoundedCornerShape(14.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(76.dp)
            .scale(scale)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(
                    elevation = if (isSelected) 8.dp else 2.dp,
                    shape = shape,
                    spotColor = if (isSelected) PrimaryNeon else Color.Black
                )
                .clip(shape)
                .background(Color(preset.dominantColorHex))
                .border(width = if (isSelected) 2.5.dp else 1.dp, color = borderColor, shape = shape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = preset.thumbnailResId),
                contentDescription = preset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Inner subtle aesthetic badge when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0x337B61FF))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = preset.name,
            color = if (isSelected) TextPrimary else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
