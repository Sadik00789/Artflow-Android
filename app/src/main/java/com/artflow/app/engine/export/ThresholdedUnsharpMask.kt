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
     * Applies thresholded unsharp masking to [bitmap] strictly on the Luminance (Y) channel.
     * Prevents chromatic fringing / color halos and uses direct indexing without inner array allocations.
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

                // Center luminance Y: 0.299*R + 0.587*G + 0.114*B
                val yCenter = 0.299f * cR + 0.587f * cG + 0.114f * cB

                // 3x3 neighbor pixels via direct indexing (zero allocations)
                val p00 = inPixels[yPrevOffset + xPrev]
                val p01 = inPixels[yPrevOffset + x]
                val p02 = inPixels[yPrevOffset + xNext]
                val p10 = inPixels[yOffset + xPrev]
                val p11 = centerColor
                val p12 = inPixels[yOffset + xNext]
                val p20 = inPixels[yNextOffset + xPrev]
                val p21 = inPixels[yNextOffset + x]
                val p22 = inPixels[yNextOffset + xNext]

                val sumY = luminanceOf(p00) + luminanceOf(p01) + luminanceOf(p02) +
                           luminanceOf(p10) + yCenter          + luminanceOf(p12) +
                           luminanceOf(p20) + luminanceOf(p21) + luminanceOf(p22)

                val avgY = sumY / 9.0f
                val deltaY = yCenter - avgY

                if (abs(deltaY) > EDGE_THRESHOLD) {
                    val correction = (deltaY * SHARPEN_FACTOR).coerceIn(-MAX_INTENSITY_CLAMP, MAX_INTENSITY_CLAMP)
                    val newR = (cR + correction).roundToInt().coerceIn(0, 255)
                    val newG = (cG + correction).roundToInt().coerceIn(0, 255)
                    val newB = (cB + correction).roundToInt().coerceIn(0, 255)
                    outPixels[yOffset + x] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                } else {
                    outPixels[yOffset + x] = centerColor
                }
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun luminanceOf(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
