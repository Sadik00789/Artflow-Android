package com.artflow.app.engine.processing

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * Normalizes input image dimensions to the target canvas baseline (768px on longest side, snapped to even integers).
 */
object ImageNormalizer {

    const val DEFAULT_MAX_DIMENSION = 768

    /**
     * Scales [source] so its longest dimension is [maxDim] (snapped to the nearest even integer),
     * ensuring aspect ratio is strictly preserved and dimensions are even to prevent tensor stride mismatch.
     */
    fun normalizeCanvas(source: Bitmap, maxDim: Int = DEFAULT_MAX_DIMENSION): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height

        val (targetWidth, targetHeight) = calculateDimensions(srcWidth, srcHeight, maxDim)

        if (srcWidth == targetWidth && srcHeight == targetHeight) {
            return source
        }

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Calculates width and height snapped to even numbers.
     */
    fun calculateDimensions(srcWidth: Int, srcHeight: Int, maxDim: Int = DEFAULT_MAX_DIMENSION): Pair<Int, Int> {
        val ratio = srcWidth.toFloat() / srcHeight.toFloat()

        var newWidth: Int
        var newHeight: Int

        if (srcWidth >= srcHeight) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).roundToInt()
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).roundToInt()
        }

        // Snap both dimensions to the nearest even integer (at least 2)
        newWidth = snapToEven(newWidth.coerceAtLeast(2))
        newHeight = snapToEven(newHeight.coerceAtLeast(2))

        return Pair(newWidth, newHeight)
    }

    private fun snapToEven(value: Int): Int {
        return if (value % 2 != 0) value - 1 else value
    }
}
