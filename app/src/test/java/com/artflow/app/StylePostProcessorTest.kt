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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

/**
 * Unit tests verifying that all 50 style presets in [StyleCatalog] execute cleanly
 * via [StylePostProcessor.applyAestheticGrading].
 * - Natural Fine Art and Anime styles pass through untouched to protect authentic neural palettes and skin tones.
 * - Graphic print styles (Warhol, Blueprint, Charcoal, Swiss, etc.) apply dedicated palette transformations.
 * - Processing latency strictly stays <50ms.
 */
class StylePostProcessorTest {

    private val bitmapPixelStorage = ConcurrentHashMap<Bitmap, IntArray>()

    private val graphicGradedStyleIds = setOf(
        "blueprint_cyanotype", "pop_art_warhol", "charcoal_sketch",
        "manga_ink_wash", "linocut_print", "swiss_typographic",
        "synthwave_neon", "cyber_glitch"
    )

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
    fun testAll50StylesExecuteWithoutExceptionAndPreserveNeuralStyles() {
        val allStyles = StyleCatalog.allStyles
        assertEquals("Catalog must contain exactly 50 styles", 50, allStyles.size)

        val testWidth = 128
        val testHeight = 128
        val testPixels = generateGradientPixels(testWidth, testHeight)

        for (style in allStyles) {
            val srcBitmap = createMockBitmap(testWidth, testHeight, testPixels)
            val outputBitmap = StylePostProcessor.applyAestheticGrading(srcBitmap, style.id)

            assertNotNull("Output bitmap for ${style.id} must not be null", outputBitmap)

            if (style.id !in graphicGradedStyleIds) {
                // Natural Fine Art & Anime styles must pass through untouched
                assertSame(
                    "Style '${style.id}' should pass through untouched without color contamination",
                    srcBitmap,
                    outputBitmap
                )
            } else {
                // Graphic styles receive aesthetic re-mapping
                val outputPixels = bitmapPixelStorage[outputBitmap]
                assertNotNull("Output pixel buffer for ${style.id} must be populated", outputPixels)
                assertFalse(
                    "Graphic style '${style.id}' should modify pixel buffer",
                    testPixels.contentEquals(outputPixels)
                )
            }
        }
    }

    @Test
    fun testGraphicStylesProduceDistinctSignatures() {
        val testWidth = 128
        val testHeight = 128
        val testPixels = generateGradientPixels(testWidth, testHeight)

        val graphicPairs = listOf(
            "blueprint_cyanotype" to "pop_art_warhol",
            "pop_art_warhol" to "charcoal_sketch",
            "swiss_typographic" to "synthwave_neon",
            "manga_ink_wash" to "synthwave_neon"
        )

        for ((styleA, styleB) in graphicPairs) {
            val srcA = createMockBitmap(testWidth, testHeight, testPixels)
            val gradedA = StylePostProcessor.applyAestheticGrading(srcA, styleA)
            val pixelsA = bitmapPixelStorage[gradedA]!!

            val srcB = createMockBitmap(testWidth, testHeight, testPixels)
            val gradedB = StylePostProcessor.applyAestheticGrading(srcB, styleB)
            val pixelsB = bitmapPixelStorage[gradedB]!!

            assertFalse(
                "Graphic styles '$styleA' and '$styleB' must produce distinct graded pixels",
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
