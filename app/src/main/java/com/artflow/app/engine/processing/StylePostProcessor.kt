package com.artflow.app.engine.processing

import android.graphics.Bitmap
import kotlin.math.roundToInt

object StylePostProcessor {

    /**
     * Applies tailored color-space grading, tone curves, and posterization to match specific style identities.
     */
    fun applyAestheticGrading(src: Bitmap, styleId: String): Bitmap {
        val width = src.width
        val height = src.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        val outPixels = IntArray(totalPixels)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        when (styleId) {
            // Graphic: Cyanotype Blueprint
            "blueprint_cyanotype" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val luma = (0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)) / 255f
                    val r = (luma * 10f).toInt().coerceIn(0, 255)
                    val g = (luma * 120f + 30f).toInt().coerceIn(0, 255)
                    val b = (luma * 210f + 45f).toInt().coerceIn(0, 255)
                    outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            // Graphic: Pop Art Warhol (4-tone posterization)
            "pop_art_warhol" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val luma = (0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF))
                    when {
                        luma < 64 -> outPixels[i] = (0xFF shl 24) or (0x1D shl 16) or (0x11 shl 8) or 0x45 // Deep Navy
                        luma < 128 -> outPixels[i] = (0xFF shl 24) or (0xEA shl 16) or (0x1E shl 8) or 0x63 // Hot Pink
                        luma < 192 -> outPixels[i] = (0xFF shl 24) or (0x00 shl 16) or (0xEB shl 8) or 0xC7 // Bright Teal
                        else -> outPixels[i] = (0xFF shl 24) or (0xFF shl 16) or (0xDF shl 8) or 0x00 // Canary Yellow
                    }
                }
            }
            // Graphic: Charcoal Sketch / Manga Ink Wash / Linocut
            "charcoal_sketch", "manga_ink_wash", "linocut_print" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val luma = (0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF))
                    // High-contrast S-curve
                    val contrast = if (luma > 128) {
                        (128 + ((luma - 128) * 1.4f)).coerceAtMost(255f).toInt()
                    } else {
                        ((luma / 128f) * 100f).toInt()
                    }
                    outPixels[i] = (0xFF shl 24) or (contrast shl 16) or (contrast shl 8) or contrast
                }
            }
            // Graphic: Cyberpunk / Synthwave (Neon Cyan & Magenta split-toning)
            "cyberpunk_neo_tokyo", "synthwave_neon", "cyber_glitch" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val r = ((p shr 16) and 0xFF).toFloat()
                    val g = ((p shr 8) and 0xFF).toFloat()
                    val b = (p and 0xFF).toFloat()
                    val newR = (r * 1.25f + 20f).toInt().coerceIn(0, 255)
                    val newG = (g * 0.85f).toInt().coerceIn(0, 255)
                    val newB = (b * 1.35f + 30f).toInt().coerceIn(0, 255)
                    outPixels[i] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                }
            }
            // Anime: Shinkai Sky / Kyoto Bloom (Boosted Saturation & Cel Vibrancy)
            "shinkai_sky", "kyoto_bloom", "ufotable_digital", "fantasy_isekai" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val r = ((p shr 16) and 0xFF).toFloat()
                    val g = ((p shr 8) and 0xFF).toFloat()
                    val b = (p and 0xFF).toFloat()
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    val newR = (gray + 1.35f * (r - gray)).roundToInt().coerceIn(0, 255)
                    val newG = (gray + 1.35f * (g - gray)).roundToInt().coerceIn(0, 255)
                    val newB = (gray + 1.35f * (b - gray)).roundToInt().coerceIn(0, 255)
                    outPixels[i] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                }
            }
            // Default pass-through
            else -> return src
        }

        val graded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        graded.setPixels(outPixels, 0, width, 0, 0, width, height)
        return graded
    }
}
