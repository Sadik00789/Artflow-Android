package com.artflow.app.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artflow.app.model.StyleCategory
import com.artflow.app.model.StyleCatalog
import com.artflow.app.model.StylePreset
import com.artflow.app.ui.theme.PrimaryNeon
import com.artflow.app.ui.theme.StudioSurface
import com.artflow.app.ui.theme.TextMuted
import com.artflow.app.ui.theme.TextPrimary

/**
 * 120Hz smooth scrolling horizontal carousel featuring all 50 artistic styles categorized across
 * Fine Art, Anime, and Graphic design.
 */
@Composable
fun StyleCarousel(
    selectedStyle: StylePreset,
    onStyleSelected: (StylePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<StyleCategory?>(null) }
    val listState = rememberLazyListState()

    val filteredStyles = remember(selectedCategory) {
        when (selectedCategory) {
            null -> StyleCatalog.allStyles
            StyleCategory.FINE_ART -> StyleCatalog.fineArtStyles
            StyleCategory.ANIME -> StyleCatalog.animeStyles
            StyleCategory.GRAPHIC -> StyleCatalog.graphicStyles
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                label = "All (50)",
                isSelected = selectedCategory == null,
                onClick = { selectedCategory = null }
            )
            CategoryChip(
                label = "Fine Art",
                isSelected = selectedCategory == StyleCategory.FINE_ART,
                onClick = { selectedCategory = StyleCategory.FINE_ART }
            )
            CategoryChip(
                label = "Anime",
                isSelected = selectedCategory == StyleCategory.ANIME,
                onClick = { selectedCategory = StyleCategory.ANIME }
            )
            CategoryChip(
                label = "Graphic",
                isSelected = selectedCategory == StyleCategory.GRAPHIC,
                onClick = { selectedCategory = StyleCategory.GRAPHIC }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Style Carousel
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filteredStyles, key = { it.id }) { preset ->
                StyleThumbnailItem(
                    preset = preset,
                    isSelected = preset.id == selectedStyle.id,
                    onClick = { onStyleSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (isSelected) PrimaryNeon else StudioSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextMuted
        )
    }
}
