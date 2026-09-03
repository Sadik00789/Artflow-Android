package com.artflow.app.engine.processing

import android.graphics.Bitmap

/**
 * Composites the original photo and the neural stylized output according to:
 * 1. Global artistic intensity (0.0 to 1.0)
 * 2. Subject preservation mask and subject blend factor (0.0 to 1.0)
 */
object Compositor {

    fun composite(
        original: Bitmap,
        stylized: Bitmap,
        mask: FloatArray? = null,
        intensity: Float = 1.0f,
        subjectBlend: Float = 0.0f
    ): Bitmap {
        val width = original.width
        val height = original.height
        val totalPixels = width * height

        val origPixels = IntArray(totalPixels)
        val stylePixels = IntArray(totalPixels)
        val outPixels = IntArray(totalPixels)

        original.getPixels(origPixels, 0, width, 0, 0, width, height)
        stylized.getPixels(stylePixels, 0, width, 0, 0, width, height)

        val clampedIntensity = intensity.coerceIn(0f, 1f)
        val clampedSubjectBlend = subjectBlend.coerceIn(0f, 1f)
        val hasValidMask = mask != null && mask.size == totalPixels && clampedSubjectBlend > 0f

        for (i in 0 until totalPixels) {
            val origColor = origPixels[i]
            val styleColor = stylePixels[i]

            // If mask exists and subjectBlend > 0, only protect pixels where mask probability > 0.15
            val effectiveStyleWeight = if (hasValidMask) {
                val maskProb = mask!![i]
                if (maskProb > 0.15f) {
                    val subjectRetention = maskProb * clampedSubjectBlend
                    clampedIntensity * (1.0f - subjectRetention)
                } else {
                    clampedIntensity // Background always receives full style intensity
                }
            } else {
                clampedIntensity
            }.coerceIn(0f, 1f)

            val origWeight = 1.0f - effectiveStyleWeight

            val oR = (origColor shr 16) and 0xFF
            val oG = (origColor shr 8) and 0xFF
            val oB = origColor and 0xFF

            val sR = (styleColor shr 16) and 0xFF
            val sG = (styleColor shr 8) and 0xFF
            val sB = styleColor and 0xFF

            val outR = (oR * origWeight + sR * effectiveStyleWeight).toInt().coerceIn(0, 255)
            val outG = (oG * origWeight + sG * effectiveStyleWeight).toInt().coerceIn(0, 255)
            val outB = (oB * origWeight + sB * effectiveStyleWeight).toInt().coerceIn(0, 255)

            outPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }
}
