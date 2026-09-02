package com.artflow.app.engine.export

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Step 3 of Studio Export Pipeline:
 * Edge-aware thresholded 3x3 unsharp masking.
 * Sharpening is applied only when edge magnitude |Δ| > 8, and the sharpening correction
 * is strictly clamped to ±12 to eliminate harsh white ringing halos.
 */
object ThresholdedUnsharpMask {

    private const val EDGE_THRESHOLD = 8.0f
    private const val MAX_INTENSITY_CLAMP = 12.0f
    private const val SHARPEN_FACTOR = 1.25f

    /**
     * Applies thresholded unsharp masking to [bitmap].
     */
    fun applySharpening(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        val inPixels = IntArray(totalPixels)
        val outPixels = IntArray(totalPixels)

        bitmap.getPixels(inPixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            val yOffset = y * width
            val yPrevOffset = (if (y > 0) y - 1 else y) * width
            val yNextOffset = (if (y < height - 1) y + 1 else y) * width

            for (x in 0 until width) {
                val xPrev = if (x > 0) x - 1 else x
                val xNext = if (x < width - 1) x + 1 else x

                val centerColor = inPixels[yOffset + x]
                val cR = (centerColor shr 16) and 0xFF
                val cG = (centerColor shr 8) and 0xFF
                val cB = centerColor and 0xFF

                // Compute 3x3 box average for each channel
                var sumR = 0
                var sumG = 0
                var sumB = 0

                val neighbors = intArrayOf(
                    inPixels[yPrevOffset + xPrev], inPixels[yPrevOffset + x], inPixels[yPrevOffset + xNext],
                    inPixels[yOffset + xPrev],     centerColor,              inPixels[yOffset + xNext],
                    inPixels[yNextOffset + xPrev], inPixels[yNextOffset + x], inPixels[yNextOffset + xNext]
                )

                for (p in neighbors) {
                    sumR += (p shr 16) and 0xFF
                    sumG += (p shr 8) and 0xFF
                    sumB += p and 0xFF
                }

                val avgR = sumR / 9.0f
                val avgG = sumG / 9.0f
                val avgB = sumB / 9.0f

                val deltaR = cR - avgR
                val deltaG = cG - avgG
                val deltaB = cB - avgB

                val newR = sharpenChannel(cR, deltaR)
                val newG = sharpenChannel(cG, deltaG)
                val newB = sharpenChannel(cB, deltaB)

                outPixels[yOffset + x] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun sharpenChannel(originalValue: Int, delta: Float): Int {
        if (abs(delta) > EDGE_THRESHOLD) {
            val correction = (delta * SHARPEN_FACTOR).coerceIn(-MAX_INTENSITY_CLAMP, MAX_INTENSITY_CLAMP)
            return (originalValue + correction).roundToInt().coerceIn(0, 255)
        }
        return originalValue
    }
}
