package com.artflow.app.engine.processing

import android.graphics.Bitmap

object StylePostProcessor {

    @Suppress("NOTHING_TO_INLINE")
    private inline fun clamp255(v: Int): Int = if (v < 0) 0 else if (v > 255) 255 else v

    fun applyAestheticGrading(src: Bitmap, styleId: String): Bitmap {
        // Fast-path bypass: Leave all natural Fine Art and Anime neural styles untouched
        when (styleId) {
            "blueprint_cyanotype", "pop_art_warhol", "charcoal_sketch",
            "manga_ink_wash", "linocut_print", "swiss_typographic",
            "synthwave_neon", "cyber_glitch" -> Unit
            else -> return src
        }

        val width = src.width
        val height = src.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        gradePixels(pixels, styleId)

        val graded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        graded.setPixels(pixels, 0, width, 0, 0, width, height)
        return graded
    }

    fun gradePixels(pixels: IntArray, styleId: String) {
        val totalPixels = pixels.size

        when (styleId) {
            // Graphic: Cyanotype Blueprint
            "blueprint_cyanotype" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8
                    val r = (luma * 12) shr 8
                    val g = (luma * 128) shr 8
                    val b = ((luma * 204) shr 8) + 45
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
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
                        luma < 64 -> (0xFF shl 24) or (0x1D shl 16) or (0x11 shl 8) or 0x45
                        luma < 128 -> (0xFF shl 24) or (0xEA shl 16) or (0x1E shl 8) or 0x63
                        luma < 192 -> (0xFF shl 24) or (0x00 shl 16) or (0xEB shl 8) or 0xC7
                        else -> (0xFF shl 24) or (0xFF shl 16) or (0xDF shl 8) or 0x00
                    }
                }
            }

            // Graphic: Monochrome S-Curves (Manga, Charcoal, Linocut)
            "charcoal_sketch", "manga_ink_wash", "linocut_print" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8
                    val v = if (luma > 128) {
                        128 + (((luma - 128) * 358) shr 8)
                    } else {
                        (luma * 200) shr 8
                    }
                    val cv = clamp255(v)
                    pixels[i] = (0xFF shl 24) or (cv shl 16) or (cv shl 8) or cv
                }
            }

            // Graphic: Swiss Typography (High contrast with selective red)
            "swiss_typographic" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8
                    val v = if (pr > 140 && pr > pg + 40 && pr > pb + 40) {
                        (0xFF shl 24) or (225 shl 16) or (20 shl 8) or 20
                    } else {
                        val mono = if (luma > 128) 245 else 20
                        (0xFF shl 24) or (mono shl 16) or (mono shl 8) or mono
                    }
                    pixels[i] = v
                }
            }

            // Graphic: Synthwave Neon & Cyber Glitch
            "synthwave_neon", "cyber_glitch" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val r = clamp255(pr + 20)
                    val g = (pg * 218) shr 8
                    val b = clamp255(pb + 25)
                    pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            else -> Unit
        }
    }
}
