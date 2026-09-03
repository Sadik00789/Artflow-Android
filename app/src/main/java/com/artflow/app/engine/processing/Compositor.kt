package com.artflow.app.engine.processing

import android.graphics.Bitmap

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
        val hasValidMask = mask != null && mask.size == totalPixels && clampedSubjectBlend > 0.0f

        for (i in 0 until totalPixels) {
            val oPixel = origPixels[i]
            val sPixel = stylePixels[i]

            val effectiveStyleWeight = if (hasValidMask) {
                val maskProb = mask!![i]
                if (maskProb > 0.15f) {
                    val normalizedProb = ((maskProb - 0.15f) / 0.85f).coerceIn(0f, 1f)
                    val subjectRetention = normalizedProb * clampedSubjectBlend
                    clampedIntensity * (1.0f - subjectRetention)
                } else {
                    clampedIntensity
                }
            } else {
                clampedIntensity
            }.coerceIn(0f, 1f)

            val origWeight = 1.0f - effectiveStyleWeight

            val oR = (oPixel shr 16) and 0xFF
            val oG = (oPixel shr 8) and 0xFF
            val oB = oPixel and 0xFF

            val sR = (sPixel shr 16) and 0xFF
            val sG = (sPixel shr 8) and 0xFF
            val sB = sPixel and 0xFF

            val r = (oR * origWeight + sR * effectiveStyleWeight).toInt().coerceIn(0, 255)
            val g = (oG * origWeight + sG * effectiveStyleWeight).toInt().coerceIn(0, 255)
            val b = (oB * origWeight + sB * effectiveStyleWeight).toInt().coerceIn(0, 255)

            outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }
}
