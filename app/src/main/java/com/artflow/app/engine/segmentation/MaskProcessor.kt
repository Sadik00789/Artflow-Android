package com.artflow.app.engine.segmentation

import kotlin.math.max
import kotlin.math.min

/**
 * Post-processes portrait alpha masks to produce clean, natural transitions without halos or bleeding.
 * Applies 1–2px morphological erosion followed by a 5–7px separable box blur for edge feathering.
 */
object MaskProcessor {

    /**
     * Refines the raw segmentation mask:
     * 1. Morphological erosion (radius [erosionRadius], default 2px) to eliminate background bleeding at edges.
     * 2. Separable box blur (radius [blurRadius], default 3px -> 7px kernel) for smooth feathering.
     */
    fun processMask(
        rawMask: FloatArray,
        width: Int,
        height: Int,
        erosionRadius: Int = 2,
        blurRadius: Int = 3
    ): FloatArray {
        val eroded = applyErosion(rawMask, width, height, erosionRadius)
        return applySeparableBoxBlur(eroded, width, height, blurRadius)
    }

    /**
     * Morphological grayscale erosion: replaces each pixel with the minimum in its neighborhood.
     */
    private fun applyErosion(
        src: FloatArray,
        width: Int,
        height: Int,
        radius: Int
    ): FloatArray {
        if (radius <= 0) return src.clone()
        val intermediate = FloatArray(width * height)
        val result = FloatArray(width * height)

        // Horizontal min pass
        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                var minVal = 1.0f
                val startX = max(0, x - radius)
                val endX = min(width - 1, x + radius)
                for (kx in startX..endX) {
                    val v = src[yOffset + kx]
                    if (v < minVal) minVal = v
                }
                intermediate[yOffset + x] = minVal
            }
        }

        // Vertical min pass
        for (y in 0 until height) {
            val yOffset = y * width
            val startY = max(0, y - radius)
            val endY = min(height - 1, y + radius)
            for (x in 0 until width) {
                var minVal = 1.0f
                for (ky in startY..endY) {
                    val v = intermediate[ky * width + x]
                    if (v < minVal) minVal = v
                }
                result[yOffset + x] = minVal
            }
        }

        return result
    }

    /**
     * 2-pass separable box blur for feathering edges in O(1) running window per pixel.
     */
    private fun applySeparableBoxBlur(
        src: FloatArray,
        width: Int,
        height: Int,
        radius: Int
    ): FloatArray {
        if (radius <= 0) return src.clone()
        val temp = FloatArray(width * height)
        val dst = FloatArray(width * height)

        // Horizontal pass
        for (y in 0 until height) {
            val rowOffset = y * width
            var windowSum = 0f
            var windowCount = 0

            // Initialize window for x = 0
            val initialRight = min(width - 1, radius)
            for (x in 0..initialRight) {
                windowSum += src[rowOffset + x]
                windowCount++
            }

            for (x in 0 until width) {
                temp[rowOffset + x] = windowSum / windowCount

                // Remove left pixel exiting window
                val left = x - radius
                if (left >= 0) {
                    windowSum -= src[rowOffset + left]
                    windowCount--
                }

                // Add right pixel entering window
                val right = x + radius + 1
                if (right < width) {
                    windowSum += src[rowOffset + right]
                    windowCount++
                }
            }
        }

        // Vertical pass
        for (x in 0 until width) {
            var windowSum = 0f
            var windowCount = 0

            val initialBottom = min(height - 1, radius)
            for (y in 0..initialBottom) {
                windowSum += temp[y * width + x]
                windowCount++
            }

            for (y in 0 until height) {
                dst[y * width + x] = (windowSum / windowCount).coerceIn(0f, 1f)

                val top = y - radius
                if (top >= 0) {
                    windowSum -= temp[top * width + x]
                    windowCount--
                }

                val bottom = y + radius + 1
                if (bottom < height) {
                    windowSum += temp[bottom * width + x]
                    windowCount++
                }
            }
        }

        return dst
    }
}
