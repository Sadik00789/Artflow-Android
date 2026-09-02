package com.artflow.app.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artflow.app.model.EditorSettings
import com.artflow.app.ui.theme.PrimaryNeon
import com.artflow.app.ui.theme.SecondaryCyan
import com.artflow.app.ui.theme.StudioBorder
import com.artflow.app.ui.theme.StudioCard
import com.artflow.app.ui.theme.TextMuted
import com.artflow.app.ui.theme.TextPrimary
import kotlin.math.roundToInt

/**
 * Modern floating glassmorphic slider controls for Style Intensity and Subject Preservation Blend.
 */
@Composable
fun BlendSliderBar(
    settings: EditorSettings,
    onSettingsChanged: (EditorSettings) -> Unit,
    hasSegmentationMask: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StudioCard)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Style Intensity Slider
        SliderRow(
            label = "Style Intensity",
            value = settings.intensity,
            onValueChange = { onSettingsChanged(settings.copy(intensity = it)) },
            activeColor = PrimaryNeon
        )

        // 2. Subject Preservation Slider (enabled when portrait/subject detected)
        if (hasSegmentationMask) {
            Spacer(modifier = Modifier.height(6.dp))
            SliderRow(
                label = "Subject Preserve",
                value = settings.subjectBlend,
                onValueChange = { onSettingsChanged(settings.copy(subjectBlend = it)) },
                activeColor = SecondaryCyan
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    activeColor: androidx.compose.ui.graphics.Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "${(value * 100).roundToInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = activeColor
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
