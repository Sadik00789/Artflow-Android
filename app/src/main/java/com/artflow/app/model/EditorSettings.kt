package com.artflow.app.model

/**
 * Live adjustment parameters for neural stylization.
 * @param intensity Level of artistic styling applied (0.0f = original, 1.0f = full style)
 * @param subjectBlend Segmentation-based mask blend (0.0f = style entire image, 1.0f = keep subject untouched)
 */
data class EditorSettings(
    val intensity: Float = 0.5f,
    val subjectBlend: Float = 0.5f
)
