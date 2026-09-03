package com.artflow.app.engine.processing

import android.graphics.Bitmap

object StylePostProcessor {

    /**
     * Applies tailored color-space grading, tone curves, and posterization to match specific style identities.
     * Uses fast integer fixed-point arithmetic and single-buffer mutation to maximize mobile CPU throughput.
     */
    fun applyAestheticGrading(src: Bitmap, styleId: String): Bitmap {
        // Fast-path bypass for styles that don't need secondary grading
        when (styleId) {
            "blueprint_cyanotype", "pop_art_warhol", "charcoal_sketch",
            "manga_ink_wash", "linocut_print", "cyberpunk_neo_tokyo",
            "synthwave_neon", "cyber_glitch", "shinkai_sky", "kyoto_bloom",
            "ufotable_digital", "fantasy_isekai" -> Unit
            else -> return src
        }

        val width = src.width
        val height = src.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        when (styleId) {
            // Graphic: Cyanotype Blueprint
            "blueprint_cyanotype" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    // Fast integer fixed-point luma (ITU-R BT.601)
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8
                    val r = (luma * 10) / 255
                    val g = ((luma * 120 + 7650) / 255).coerceIn(0, 255)
                    val b = ((luma * 210 + 11475) / 255).coerceIn(0, 255)
                    pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            // Graphic: Pop Art Warhol (4-tone posterization)
            "pop_art_warhol" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8
                    pixels[i] = when {
                        luma < 64 -> (0xFF shl 24) or (0x1D shl 16) or (0x11 shl 8) or 0x45 // Deep Navy
                        luma < 128 -> (0xFF shl 24) or (0xEA shl 16) or (0x1E shl 8) or 0x63 // Hot Pink
                        luma < 192 -> (0xFF shl 24) or (0x00 shl 16) or (0xEB shl 8) or 0xC7 // Bright Teal
                        else -> (0xFF shl 24) or (0xFF shl 16) or (0xDF shl 8) or 0x00 // Canary Yellow
                    }
                }
            }
            // Graphic: Charcoal Sketch / Manga Ink Wash / Linocut
            "charcoal_sketch", "manga_ink_wash", "linocut_print" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8
                    // High-contrast S-curve
                    val contrast = if (luma > 128) {
                        (128 + ((luma - 128) * 14) / 10).coerceAtMost(255)
                    } else {
                        (luma * 100) / 128
                    }
                    pixels[i] = (0xFF shl 24) or (contrast shl 16) or (contrast shl 8) or contrast
                }
            }
            // Graphic: Cyberpunk / Synthwave (Neon Cyan & Magenta split-toning)
            "cyberpunk_neo_tokyo", "synthwave_neon", "cyber_glitch" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val newR = ((pr * 125) / 100 + 20).coerceIn(0, 255)
                    val newG = ((pg * 85) / 100).coerceIn(0, 255)
                    val newB = ((pb * 135) / 100 + 30).coerceIn(0, 255)
                    pixels[i] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                }
            }
            // Anime: Shinkai Sky / Kyoto Bloom (Boosted Saturation & Cel Vibrancy)
            "shinkai_sky", "kyoto_bloom", "ufotable_digital", "fantasy_isekai" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val gray = (77 * pr + 150 * pg + 29 * pb) shr 8
                    val newR = (gray + (135 * (pr - gray)) / 100).coerceIn(0, 255)
                    val newG = (gray + (135 * (pg - gray)) / 100).coerceIn(0, 255)
                    val newB = (gray + (135 * (pb - gray)) / 100).coerceIn(0, 255)
                    pixels[i] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
                }
            }
        }

        val graded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        graded.setPixels(pixels, 0, width, 0, 0, width, height)
        return graded
    }
}
