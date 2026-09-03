package com.artflow.app.engine.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.roundToInt

/**
 * Padding parameters describing where an image was placed inside the 768x768 static canvas.
 */
data class CanvasPadding(
    val padLeft: Int,
    val padTop: Int,
    val originalWidth: Int,
    val originalHeight: Int
)

/**
 * Normalizes input image dimensions to the target canvas baseline (768px on longest side, snapped to even integers),
 * and handles symmetric padding and cropping for static 768x768 TFLite OpenCL inference.
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

    /**
     * Scales [source] so its longest edge is 768px, then pads symmetrically to a 768x768 square.
     * Uses black letterboxing.
     */
    fun padToSquare768(source: Bitmap): Pair<Bitmap, CanvasPadding> {
        val scaled = normalizeCanvas(source, DEFAULT_MAX_DIMENSION)
        val sWidth = scaled.width
        val sHeight = scaled.height

        if (sWidth == DEFAULT_MAX_DIMENSION && sHeight == DEFAULT_MAX_DIMENSION) {
            return Pair(scaled, CanvasPadding(0, 0, sWidth, sHeight))
        }

        val padLeft = (DEFAULT_MAX_DIMENSION - sWidth) / 2
        val padTop = (DEFAULT_MAX_DIMENSION - sHeight) / 2

        val paddedBitmap = Bitmap.createBitmap(
            DEFAULT_MAX_DIMENSION,
            DEFAULT_MAX_DIMENSION,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(paddedBitmap)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(scaled, padLeft.toFloat(), padTop.toFloat(), null)

        val padding = CanvasPadding(
            padLeft = padLeft,
            padTop = padTop,
            originalWidth = sWidth,
            originalHeight = sHeight
        )
        return Pair(paddedBitmap, padding)
    }

    /**
     * Restores original aspect ratio from the 768x768 square padded bitmap using [CanvasPadding].
     */
    fun cropFromSquare768(padded: Bitmap, padding: CanvasPadding): Bitmap {
        if (padding.padLeft == 0 && padding.padTop == 0 &&
            padding.originalWidth == padded.width && padding.originalHeight == padded.height
        ) {
            return padded
        }
        return Bitmap.createBitmap(
            padded,
            padding.padLeft,
            padding.padTop,
            padding.originalWidth,
            padding.originalHeight
        )
    }

    private fun snapToEven(value: Int): Int {
        return if (value % 2 != 0) value - 1 else value
    }
}
