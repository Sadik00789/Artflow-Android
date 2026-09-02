package com.artflow.app.model

import androidx.annotation.DrawableRes

/**
 * Data model representing an individual neural artistic style.
 */
data class StylePreset(
    val id: String,
    val name: String,
    val category: StyleCategory,
    val modelAssetPath: String,
    @DrawableRes val thumbnailResId: Int,
    val dominantColorHex: Long = 0xFF7B61FF
)
