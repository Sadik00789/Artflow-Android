package com.artflow.app.engine.export

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * Step 2 of Studio Export Pipeline:
 * Extracts micro-frequency high-pass details from the original photo at 1536px and
 * injects 12% detail strictly into the Luminance (Y) channel of the upscaled stylized artwork.
 */
object LuminanceDetailInjector {

    private const val DETAIL_INJECTION_PERCENT = 0.12f

    /**
     * Injects high-frequency luminance detail from [originalPhoto] into [upscaledArtwork].
     *
     * @param originalPhoto Original full-resolution source photo
     * @param upscaledArtwork 1536px FSRCNN upscaled stylized output
     * @return Reconstructed bitmap with micro-frequencies restored
     */
    fun injectDetail(originalPhoto: Bitmap, upscaledArtwork: Bitmap): Bitmap {
        val width = upscaledArtwork.width
        val height = upscaledArtwork.height
        val totalPixels = width * height

        // 1. Scale original photo to match upscaled dimensions exactly
        val scaledOriginal = if (originalPhoto.width == width && originalPhoto.height == height) {
            originalPhoto
        } else {
            Bitmap.createScaledBitmap(originalPhoto, width, height, true)
        }

        val origPixels = IntArray(totalPixels)
        val stylePixels = IntArray(totalPixels)
        val outputPixels = IntArray(totalPixels)

        scaledOriginal.getPixels(origPixels, 0, width, 0, 0, width, height)
        upscaledArtwork.getPixels(stylePixels, 0, width, 0, 0, width, height)

        // 2. Compute Gaussian Blur (3x3 kernel, sigma ~ 1.0) on original photo luminance
        // Kernel: [1 2 1; 2 4 2; 1 2 1] / 16
        val origLuma = FloatArray(totalPixels)
        for (i in 0 until totalPixels) {
            val p = origPixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            origLuma[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        val blurLuma = FloatArray(totalPixels)
        for (y in 0 until height) {
            val yOffset = y * width
            val yPrevOffset = (if (y > 0) y - 1 else y) * width
            val yNextOffset = (if (y < height - 1) y + 1 else y) * width

            for (x in 0 until width) {
                val xPrev = if (x > 0) x - 1 else x
                val xNext = if (x < width - 1) x + 1 else x

                val sum = 1f * origLuma[yPrevOffset + xPrev] + 2f * origLuma[yPrevOffset + x] + 1f * origLuma[yPrevOffset + xNext] +
                          2f * origLuma[yOffset + xPrev]     + 4f * origLuma[yOffset + x]     + 2f * origLuma[yOffset + xNext] +
                          1f * origLuma[yNextOffset + xPrev] + 2f * origLuma[yNextOffset + x] + 1f * origLuma[yNextOffset + xNext]

                blurLuma[yOffset + x] = sum / 16f
            }
        }

        // 3. Inject 12% detail into Y channel of stylized image
        for (i in 0 until totalPixels) {
            val highPass = origLuma[i] - blurLuma[i]

            val sp = stylePixels[i]
            val sR = ((sp shr 16) and 0xFF).toFloat()
            val sG = ((sp shr 8) and 0xFF).toFloat()
            val sB = (sp and 0xFF).toFloat()

            // Convert stylized RGB to YCbCr
            val yStyle = 0.299f * sR + 0.587f * sG + 0.114f * sB
            val cbStyle = -0.168736f * sR - 0.331264f * sG + 0.5f * sB
            val crStyle = 0.5f * sR - 0.418688f * sG - 0.081312f * sB

            // Inject 12% HighPass
            val yNew = (yStyle + DETAIL_INJECTION_PERCENT * highPass).coerceIn(0f, 255f)

            // Convert back to RGB
            val outR = (yNew + 1.402f * crStyle).roundToInt().coerceIn(0, 255)
            val outG = (yNew - 0.344136f * cbStyle - 0.714136f * crStyle).roundToInt().coerceIn(0, 255)
            val outB = (yNew + 1.772f * cbStyle).roundToInt().coerceIn(0, 255)

            outputPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return result
    }
}
