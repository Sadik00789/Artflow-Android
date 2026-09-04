package com.artflow.app

import android.graphics.Bitmap
import com.artflow.app.engine.processing.StylePostProcessor
import com.artflow.app.model.StyleCatalog
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

/**
 * Unit tests verifying that all 50 style presets in [StyleCatalog] execute cleanly
 * via [StylePostProcessor.applyAestheticGrading], produce visually distinct pixel signatures
 * across shared base neural models, and meet the <50ms processing latency requirement.
 */
class StylePostProcessorTest {

    private val bitmapPixelStorage = ConcurrentHashMap<Bitmap, IntArray>()

    @Before
    fun setUp() {
        mockkStatic(Bitmap::class)

        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) } answers {
            val w = firstArg<Int>()
            val h = secondArg<Int>()
            createMockBitmap(w, h)
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        bitmapPixelStorage.clear()
    }

    private fun createMockBitmap(width: Int, height: Int, initialPixels: IntArray? = null): Bitmap {
        val bmp = mockk<Bitmap>(relaxed = true)
        val buffer = initialPixels?.copyOf() ?: IntArray(width * height)
        bitmapPixelStorage[bmp] = buffer

        every { bmp.width } returns width
        every { bmp.height } returns height
        every { bmp.getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val dest = firstArg<IntArray>()
            val offset = secondArg<Int>()
            val srcBuf = bitmapPixelStorage[bmp] ?: buffer
            System.arraycopy(srcBuf, 0, dest, offset, dest.size.coerceAtMost(srcBuf.size))
        }
        every { bmp.setPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val src = firstArg<IntArray>()
            val offset = secondArg<Int>()
            val targetBuf = bitmapPixelStorage[bmp] ?: buffer
            System.arraycopy(src, offset, targetBuf, 0, targetBuf.size.coerceAtMost(src.size - offset))
        }
        return bmp
    }

    private fun generateGradientPixels(width: Int, height: Int): IntArray {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255) / width
                val g = (y * 255) / height
                val b = ((x + y) * 255) / (width + height)
                pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return pixels
    }

    @Test
    fun testAll50StylesExecuteWithoutException() {
        val allStyles = StyleCatalog.allStyles
        assertEquals("Catalog must contain exactly 50 styles", 50, allStyles.size)

        val testWidth = 256
        val testHeight = 256
        val testPixels = generateGradientPixels(testWidth, testHeight)

        for (style in allStyles) {
            val srcBitmap = createMockBitmap(testWidth, testHeight, testPixels)
            val gradedBitmap = StylePostProcessor.applyAestheticGrading(srcBitmap, style.id)

            assertNotNull("Graded bitmap for ${style.id} must not be null", gradedBitmap)
            val outputPixels = bitmapPixelStorage[gradedBitmap]
            assertNotNull("Output pixel buffer for ${style.id} must be populated", outputPixels)
            assertEquals("Output buffer size must match canvas", testWidth * testHeight, outputPixels!!.size)
        }
    }

    @Test
    fun testSharedBaseModelsProduceDistinctPixelSignatures() {
        val testWidth = 128
        val testHeight = 128
        val testPixels = generateGradientPixels(testWidth, testHeight)

        // Pairs of styles sharing identical underlying model weights
        val sharedModelPairs = listOf(
            // mosaic.pth
            "starry_night" to "guernica",
            "starry_night" to "cezanne_mont_sainte_victoire",
            "guernica" to "seurat_la_grande_jatte",
            // udnie.pth
            "the_scream" to "great_wave",
            "the_scream" to "hokusai_red_fuji",
            "great_wave" to "munch_madonna",
            // candy.pth
            "kandinsky_composition" to "klimt_the_kiss",
            "klimt_the_kiss" to "van_gogh_sunflowers",
            "matisse_dance" to "gauguin_tahitian_women",
            // rain_princess.pth
            "monet_water_lilies" to "turner_rain_steam_speed",
            "degas_ballet" to "renoir_boating_party",
            // paprika.pt
            "ghibli_pastoral" to "manga_ink_wash",
            "lofi_chill" to "fantasy_isekai",
            // face_paint_512_v1.pt
            "shinkai_sky" to "cyberpunk_neo_tokyo",
            "shinkai_sky" to "mecha_cel_shade",
            "cyberpunk_neo_tokyo" to "trigger_action",
            // face_paint_512_v2.pt
            "kyoto_bloom" to "ufotable_digital",
            "chibi_pastel" to "shoujo_sparkle",
            // celeba_distill.pt
            "retro_80s_anime" to "dark_fantasy_berserk",
            "vaporwave_sunset" to "city_pop_1984"
        )

        for ((styleA, styleB) in sharedModelPairs) {
            val srcA = createMockBitmap(testWidth, testHeight, testPixels)
            val gradedA = StylePostProcessor.applyAestheticGrading(srcA, styleA)
            val pixelsA = bitmapPixelStorage[gradedA]!!

            val srcB = createMockBitmap(testWidth, testHeight, testPixels)
            val gradedB = StylePostProcessor.applyAestheticGrading(srcB, styleB)
            val pixelsB = bitmapPixelStorage[gradedB]!!

            assertFalse(
                "Styles '$styleA' and '$styleB' share base model weights but must produce distinct graded pixels",
                pixelsA.contentEquals(pixelsB)
            )
        }
    }

    @Test
    fun testGradingPerformanceUnder50msOnTestCanvas() {
        val width = 512
        val height = 512
        val testPixels = generateGradientPixels(width, height)

        val srcBitmap = createMockBitmap(width, height, testPixels)
        val outBitmap = createMockBitmap(width, height)
        every { Bitmap.createBitmap(512, 512, any<Bitmap.Config>()) } returns outBitmap

        for (style in StyleCatalog.allStyles) {
            val t0 = System.nanoTime()
            val gradedBitmap = StylePostProcessor.applyAestheticGrading(srcBitmap, style.id)
            val t1 = System.nanoTime()
            val elapsedMs = (t1 - t0) / 1_000_000

            assertNotNull(gradedBitmap)
            assertTrue(
                "Style '${style.id}' processing took ${elapsedMs}ms, exceeding 50ms requirement",
                elapsedMs < 50
            )
        }
    }
}
