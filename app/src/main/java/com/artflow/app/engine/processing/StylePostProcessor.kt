package com.artflow.app.engine.processing

import android.graphics.Bitmap

object StylePostProcessor {

    @Suppress("NOTHING_TO_INLINE")
    private inline fun clamp255(v: Int): Int = if (v < 0) 0 else if (v > 255) 255 else v

    /**
     * Applies tailored color-space grading, tone curves, and split-toning rules to match specific style identities.
     * Overhauled for all 50 style presets in ArtFlow to guarantee distinct visual output even when sharing base model weights.
     * Uses fast ITU-R BT.601 integer fixed-point arithmetic (shr 8) and single-buffer mutation with zero inner-loop heap allocations.
     */
    fun applyAestheticGrading(src: Bitmap, styleId: String): Bitmap {
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

    /**
     * Mutates [pixels] buffer in-place according to [styleId]'s dedicated aesthetic grading algorithm.
     * All operations strictly use 8-bit integer fixed-point math (shr 8) and zero allocations for sub-25ms execution on 1024px canvases.
     */
    fun gradePixels(pixels: IntArray, styleId: String) {
        val totalPixels = pixels.size

        when (styleId) {
            // ==========================================
            // 1. FINE ART PRESETS (18 Styles)
            // ==========================================

            // Starry Night (base: mosaic): Night sky split-tone, blue boost, Van Gogh cadmium yellow highlights
            "starry_night" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r: Int
                    var g: Int
                    var b = ((pb * 346) shr 8) + 25
                    if (luma > 128) {
                        r = pr + 20
                        g = pg + 15
                    } else {
                        r = (pr * 218) shr 8
                        g = (pg * 218) shr 8
                        b = (b * 243) shr 8
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // The Scream (base: udnie): Fiery apocalyptic sky, intense red push, warm yellow highlights
            "the_scream" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = ((pr * 358) shr 8) + 30
                    var g = pg
                    var b = pb
                    if (luma > 128) {
                        r += 25
                        g += 10
                    } else {
                        g = (g * 218 + luma * 38) shr 8
                        b = (b * 192 + luma * 38) shr 8
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // The Great Wave (base: udnie): Oceanic Prussian blue, deep indigo shadows, foam white highlights
            "great_wave" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r: Int
                    var g: Int
                    var b: Int
                    if (luma > 175) {
                        r = pr + 30
                        g = pg + 30
                        b = pb + 30
                    } else if (luma <= 130) {
                        r = (pr * 192) shr 8
                        g = (pg * 225) shr 8
                        b = pb + 35
                    } else {
                        r = (pr * 205) shr 8
                        g = pg
                        b = ((pb * 307) shr 8) + 15
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Guernica (base: mosaic): Stark cubist monochrome, crushed blacks, stark chalk whites
            "guernica" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val v = if (luma > 128) {
                        128 + (((luma - 128) * 384) shr 8)
                    } else {
                        (luma * luma) shr 7
                    }
                    val clampedV = clamp255(v)
                    pixels[i] = (0xFF shl 24) or (clampedV shl 16) or (clampedV shl 8) or clampedV
                }
            }

            // Monet Water Lilies (base: rain_princess): Soft emerald teal pond tone, warm lilac highlights
            "monet_water_lilies" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = pr
                    var g = ((pg * 307) shr 8) + 15
                    var b = ((pb * 282) shr 8) + 10
                    if (luma > 140) {
                        r = pr + 20
                        b += 15
                        g -= 5
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Kandinsky Composition VII (base: candy): Primary color saturation, 1.4x vibrance on primaries
            "kandinsky_composition" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = luma + (((pr - luma) * 358) shr 8)
                    var g = luma + (((pg - luma) * 358) shr 8)
                    var b = luma + (((pb - luma) * 358) shr 8)
                    if (pr > pg && pr > pb) r += 25
                    else if (pb > pr && pb > pg) b += 25
                    else if (pr > 120 && pg > 120 && pb < 100) { r += 18; g += 18 }

                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Klimt The Kiss (base: candy): Gilded gold leaf, rich amber luster
            "klimt_the_kiss" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = ((pr * 333) shr 8) + 30
                    val g = ((pg * 294) shr 8) + 15
                    val b = (pb * 192) shr 8
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Van Gogh Sunflowers (base: candy): Sun-drenched sunflower yellow, intense golden push
            "van_gogh_sunflowers" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    var r = pr + 35
                    var g = pg + 20
                    val b = pb - 25
                    if (r > b) {
                        r += 10
                        g += 10
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Cezanne Mont Sainte-Victoire (base: mosaic): Provençal earth, olive green, sandstone warmth
            "cezanne_mont_sainte_victoire" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = pr + 20
                    val g = pg + 15
                    val b = pb - 10
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Turner Rain Steam Speed (base: rain_princess): Sepia-gold mist, softened shadow contrast
            "turner_rain_steam_speed" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    var r = pr + 25
                    var g = pg + 15
                    var b = pb - 15
                    if (r < 40) r = 40 + (r shr 1)
                    if (g < 35) g = 35 + (g shr 1)
                    if (b < 30) b = 30 + (b shr 1)
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Hokusai Red Fuji (base: udnie): Crimson sunrise peak, cool cyan morning shadows
            "hokusai_red_fuji" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = ((pr * 371) shr 8) + 30
                    var g = pg
                    var b = pb
                    if (luma < 110) {
                        b += 25
                        g += 15
                        r = (r * 205) shr 8
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Degas Ballet Rehearsal (base: rain_princess): Warm champagne glow, blush pinks, lifted velvet blacks
            "degas_ballet" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    var r = pr + 20
                    var g = pg + 5
                    var b = pb + 10
                    if (r < 25) r = 25
                    if (g < 25) g = 25
                    if (b < 25) b = 25
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Renoir Boating Party (base: rain_princess): Peach warmth, radiant warm skin tones
            "renoir_boating_party" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = pr + 15
                    val g = pg + 10
                    val b = ((pb * 243) shr 8) + 5
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Munch Madonna (base: udnie): Brooding gothic crimson halo, midnight charcoal shadows
            "munch_madonna" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma > 140) {
                        r = pr + 30
                        g = pg - 10
                        b = pb - 10
                    } else {
                        r = (pr * 218) shr 8
                        g = pg - 15
                        b = pb + 10
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Picasso Weeping Woman (base: mosaic): Discordant cubist contrast, acid chartreuse vs tear lavenders
            "picasso_weeping_woman" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    var r: Int
                    var g: Int
                    var b: Int
                    if (pg > pr) {
                        r = (pr * 218) shr 8
                        g = ((pg * 320) shr 8) + 10
                        b = (pb * 218) shr 8
                    } else {
                        r = pr + 10
                        g = (pg * 230) shr 8
                        b = pb + 20
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Matisse Dance (base: candy): Minimalist Fauvist terracotta red, ultramarine blue, emerald earth
            "matisse_dance" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    var r: Int
                    var g: Int
                    var b: Int
                    if (pr >= pg && pr >= pb) {
                        r = pr + 40
                        g = (pg * 218) shr 8
                        b = (pb * 218) shr 8
                    } else if (pb >= pr && pb >= pg) {
                        r = (pr * 205) shr 8
                        g = (pg * 230) shr 8
                        b = ((pb * 346) shr 8) + 20
                    } else {
                        r = (pr * 218) shr 8
                        g = ((pg * 333) shr 8) + 15
                        b = (pb * 218) shr 8
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Gauguin Tahitian Women (base: candy): Saturated mango yellow, hibiscus red, jungle warmth
            "gauguin_tahitian_women" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = ((pr * 294) shr 8) + 25
                    val g = ((pg * 282) shr 8) + 15
                    val b = ((pb * 205) shr 8) - 10
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Seurat La Grande Jatte (base: mosaic): Golden lawn yellow-greens, shimmering afternoon sun
            "seurat_la_grande_jatte" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = pr + 10
                    var g = pg + 20
                    val b = (pb * 230) shr 8
                    if (luma > 155) {
                        r += 15
                        g += 15
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // ==========================================
            // 2. ANIME PRESETS (16 Styles)
            // ==========================================

            // Shinkai Sky (base: face_paint_v1): Vivid cobalt azure sky, luminous white cloud highlights
            "shinkai_sky" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = pr
                    var g = (pg * 269) shr 8
                    var b = ((pb * 346) shr 8) + 25
                    if (luma > 175) {
                        r += 25
                        g += 25
                        b += 30
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Ghibli Pastoral (base: paprika): Studio Ghibli lush moss/meadow greens, warm nostalgic shadows
            "ghibli_pastoral" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = pr + 10
                    val g = ((pg * 320) shr 8) + 15
                    val b = ((pb * 230) shr 8) - 10
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Cyberpunk Neo-Tokyo (base: face_paint_v1): Electric cyan highlights, deep magenta shadows
            "cyberpunk_neo_tokyo" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma > 128) {
                        r = (pr * 205) shr 8
                        g = pg + 15
                        b = pb + 35
                    } else {
                        r = pr + 30
                        g = pg - 10
                        b = pb + 15
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Retro 80s Anime (base: celeba_distill): Hand-cel warm magenta/amber tint, softened matte contrast
            "retro_80s_anime" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val rawR = pr + 15
                    val rawB = pb + 10

                    val r = 20 + ((rawR * 215) shr 8)
                    val g = 20 + ((pg * 215) shr 8)
                    val b = 20 + ((rawB * 215) shr 8)
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Kyoto Bloom (base: face_paint_v2): Sakura cherry blossom pinks, lifted shadows
            "kyoto_bloom" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = ((pr * 307) shr 8) + 25
                    var g = pg + 10
                    var b = pb + 15
                    if (luma < 45) {
                        r += 20
                        g += 15
                        b += 20
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Manga Ink Wash (base: paprika): Screentone sumi-e high-contrast printed manga ink curve
            "manga_ink_wash" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val v = when {
                        luma < 85 -> (luma * 90) shr 8
                        luma > 185 -> 235 + (((luma - 185) * 73) shr 8)
                        else -> 30 + (((luma - 85) * 524) shr 8)
                    }
                    val clampedV = clamp255(v)
                    pixels[i] = (0xFF shl 24) or (clampedV shl 16) or (clampedV shl 8) or clampedV
                }
            }

            // Ufotable Digital (base: face_paint_v2): Fiery ember saturation, digital blooming highlights
            "ufotable_digital" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = ((pr * 346) shr 8) + 25
                    var g = ((pg * 282) shr 8) + 10
                    val b = (pb * 218) shr 8
                    if (luma > 165) {
                        r += 30
                        g += 20
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Chibi Pastel (base: face_paint_v2): Milky low-contrast pastels, lifted blacks (+30 floor)
            "chibi_pastel" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = 40 + ((pr * 195) shr 8)
                    val g = 45 + ((pg * 195) shr 8)
                    val b = 35 + ((pb * 195) shr 8)
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Mecha Cel-Shade (base: face_paint_v1): Cold industrial steel cyan/gray, crisp edge separation
            "mecha_cel_shade" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = (pr * 218) shr 8
                    var g = ((pg * 243) shr 8) + 5
                    var b = ((pb * 294) shr 8) + 10
                    if (luma > 128) {
                        r += 12
                        g += 12
                        b += 18
                    } else {
                        r -= 15
                        g -= 15
                        b -= 15
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Lo-Fi Beats (base: paprika): Dusty lavender/plum wash, faded matte blacks, vinyl warmth
            "lofi_chill" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = 40 + ((pr * 218) shr 8)
                    val g = 20 + ((pg * 205) shr 8)
                    val b = 50 + ((pb * 218) shr 8)
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Fantasy Isekai (base: paprika): High-vibrance emerald-cyan flora, twilight violet shadows
            "fantasy_isekai" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma > 110) {
                        r = (pr * 230) shr 8
                        g = pg + 25
                        b = pb + 20
                    } else {
                        r = pr + 18
                        g = pg - 10
                        b = pb + 25
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Shoujo Sparkle (base: face_paint_v2): Romantic champagne pinks, dreamy highlight bloom
            "shoujo_sparkle" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = pr + 25
                    var g = pg + 15
                    var b = pb + 15
                    if (luma > 150) {
                        r += 20
                        g += 15
                        b += 12
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Dark Fantasy Berserk (base: celeba_distill): Eclipse dark fantasy, desaturated shadows, blood red highlights
            "dark_fantasy_berserk" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma > 135) {
                        r = pr + 45
                        g = pg - 20
                        b = pb - 20
                    } else {
                        r = (pr * 179) shr 8
                        g = (pg * 154) shr 8
                        b = (pb * 154) shr 8
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Vaporwave Sunset (base: celeba_distill): 1980s synth sunset, flamingo pink highlights, violet-cyan shadows
            "vaporwave_sunset" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma > 120) {
                        r = pr + 35
                        g = pg - 10
                        b = pb + 25
                    } else {
                        r = pr + 10
                        g = pg - 15
                        b = pb + 35
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // City Pop 1984 (base: celeba_distill): Resort Tokyo pop, Pacific turquoise, vibrant pastel carmine
            "city_pop_1984" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val g = pg + 15
                    val b = pb + 25
                    val r = if (pr > pg && pr > pb) pr + 25 else (pr * 230) shr 8
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Trigger High-Action (base: face_paint_v1): Extreme contrast, fiery saturated reds, acid yellows
            "trigger_action" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r: Int
                    val g: Int
                    val b: Int
                    if (pr > 140 && pg > 140) {
                        r = pr + 35
                        g = pg + 30
                        b = (pb * 179) shr 8
                    } else if (pr > 120) {
                        r = ((pr * 358) shr 8) + 20
                        g = (pg * 205) shr 8
                        b = (pb * 205) shr 8
                    } else {
                        r = (pr * 205) shr 8
                        g = (pg * 205) shr 8
                        b = (pb * 230) shr 8
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // ==========================================
            // 3. GRAPHIC PRESETS (16 Styles)
            // ==========================================

            // Bauhaus Geometry: Constructivist primary red/blue emphasis with muted ivory paper background
            "bauhaus_geometry" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (pr > pg + 30 && pr > pb + 30) {
                        r = 220
                        g = 30
                        b = 30
                    } else if (pb > pr + 30 && pb > pg + 30) {
                        r = 20
                        g = 60
                        b = 210
                    } else if (luma > 150) {
                        r = 245
                        g = 240
                        b = 225
                    } else {
                        val v = (luma * 205) shr 8
                        r = v
                        g = v
                        b = v
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Pop Art Warhol: 4-tone screen-print posterization (Deep Navy, Hot Pink, Bright Teal, Canary Yellow)
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

            // Risograph Print: Two-drum spot ink emulation (Fluorescent Pink and Aqua Teal)
            "risograph_print" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma < 128) {
                        r = pr + 45
                        g = (pg * 154) shr 8
                        b = pb + 20
                    } else {
                        r = (pr * 128) shr 8
                        g = pg + 30
                        b = pb + 35
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Synthwave Neon: Outrun laser magenta highlights, deep midnight purple shadow gradient
            "synthwave_neon" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = ((pr * 294) shr 8) + 35
                    val g = (pg * 166) shr 8
                    val b = ((pb * 333) shr 8) + 40
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Comic Halftone: Golden Age comic print saturation, newsprint yellow tint, bold ink lines
            "comic_halftone" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma < 50) {
                        r = 15
                        g = 15
                        b = 20
                    } else {
                        r = pr + 18
                        g = pg + 12
                        b = ((pb * 218) shr 8) - 10
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Swiss International: Stark high-contrast black/white with iconic Swiss Cadmium Red accentuation
            "swiss_typographic" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (pr > 130 && pr > ((pg * 307) shr 8) && pr > ((pb * 307) shr 8)) {
                        r = 225
                        g = 20
                        b = 20
                    } else {
                        val v = if (luma > 125) 245 else 20
                        r = v
                        g = v
                        b = v
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Art Deco Gold: Metallic gold highlights set against dense velvet black shadows
            "art_deco_gold" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma > 105) {
                        r = ((pr * 333) shr 8) + 30
                        g = ((pg * 294) shr 8) + 15
                        b = (pb * 166) shr 8
                    } else {
                        r = (pr * 90) shr 8
                        g = (pg * 90) shr 8
                        b = (pb * 102) shr 8
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Cyber Glitch: Digital chromatic aberration channel split
            "cyber_glitch" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r = pr + 25
                    val g = (pg * 218) shr 8
                    val b = pb + 35
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Cyanotype Blueprint: Pure Prussian blue architectural blueprint monochrome
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

            // Vector Flat: Saturated clean primary tones, quantized color banding
            "vector_flat" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val qR = (pr and 0xE0) or 0x10
                    val qG = (pg and 0xE0) or 0x10
                    val qB = (pb and 0xE0) or 0x10

                    val r = luma + (((qR - luma) * 333) shr 8)
                    val g = luma + (((qG - luma) * 333) shr 8)
                    val b = luma + (((qB - luma) * 333) shr 8)
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Street Art Stencil: Gritty pavement monochrome with high-contrast thresholding and spray tag accent
            "stencil_street_art" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (pr > 160 && pg < 120) {
                        r = 255
                        g = 20
                        b = 120
                    } else if (luma > 135) {
                        r = 230
                        g = 230
                        b = 225
                    } else {
                        r = 35
                        g = 35
                        b = 40
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Linocut Print: Heavy relief ink with warm aged paper highlights
            "linocut_print" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    val r: Int
                    val g: Int
                    val b: Int
                    if (luma < 115) {
                        r = 20
                        g = 18
                        b = 22
                    } else {
                        r = 255
                        g = 242
                        b = 215
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Psychedelic 60s: Liquid light show, fluorescent lime green and psychedelic purple/orange
            "psychedelic_60s" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r: Int
                    val g: Int
                    val b: Int
                    if (pg > pr && pg > pb) {
                        r = (pr * 205) shr 8
                        g = pg + 35
                        b = (pb * 205) shr 8
                    } else if (pr > pb) {
                        r = pr + 35
                        g = pg + 15
                        b = (pb * 154) shr 8
                    } else {
                        r = pr + 30
                        g = (pg * 154) shr 8
                        b = pb + 40
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Holographic: Shifting opalescent foil, elevated cyan-violet reflections
            "holographic_iridescent" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF
                    val luma = (77 * pr + 150 * pg + 29 * pb) shr 8

                    var r = ((pr * 243) shr 8) + 10
                    var g = pg + 15
                    var b = pb + 30
                    if (luma > 130) {
                        r += 10
                        g += 10
                        b += 15
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            // Charcoal Sketch: Willow charcoal deep carbon shadows, textured paper S-curve, pure monochrome
            "charcoal_sketch" -> {
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
                    val clampedV = clamp255(v)
                    pixels[i] = (0xFF shl 24) or (clampedV shl 16) or (clampedV shl 8) or clampedV
                }
            }

            // Woodblock Ukiyo-e: Traditional Japanese Ukiyo-e washi cream, mineral indigo, persimmon orange
            "woodblock_ukiyoe" -> {
                for (i in 0 until totalPixels) {
                    val p = pixels[i]
                    val pr = (p shr 16) and 0xFF
                    val pg = (p shr 8) and 0xFF
                    val pb = p and 0xFF

                    val r: Int
                    val g: Int
                    val b: Int
                    if (pb > pr && pb > pg) {
                        r = (pr * 218) shr 8
                        g = (pg * 230) shr 8
                        b = pb + 25
                    } else if (pr > pb + 20) {
                        r = pr + 25
                        g = pg + 10
                        b = (pb * 205) shr 8
                    } else {
                        r = pr + 20
                        g = pg + 15
                        b = pb - 5
                    }
                    pixels[i] = (0xFF shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
                }
            }

            else -> Unit
        }
    }
}
