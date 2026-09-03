package com.artflow.app

import com.artflow.app.engine.processing.ImageNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying canvas dimension normalization to 768px and even integer snapping.
 */
class ImageNormalizerTest {

    @Test
    fun testLandscapeNormalization() {
        // Standard 4:3 12MP camera photo
        val (width, height) = ImageNormalizer.calculateDimensions(4032, 3024, maxDim = 512)
        assertEquals(512, width)
        assertEquals(384, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testPortraitNormalization() {
        // Standard 3:4 portrait photo
        val (width, height) = ImageNormalizer.calculateDimensions(3024, 4032, maxDim = 512)
        assertEquals(384, width)
        assertEquals(512, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testSquareNormalization() {
        val (width, height) = ImageNormalizer.calculateDimensions(1080, 1080, maxDim = 512)
        assertEquals(512, width)
        assertEquals(512, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testOddDimensionSnapping() {
        // Arbitrary dimensions resulting in odd numbers
        val (width, height) = ImageNormalizer.calculateDimensions(1920, 1081, maxDim = 512)
        assertEquals(512, width)
        assertTrue("Calculated height must be snapped to even integer", height % 2 == 0)
    }

    @Test
    fun testCustomMaxDimension() {
        val (width, height) = ImageNormalizer.calculateDimensions(1000, 500, maxDim = 256)
        assertEquals(256, width)
        assertEquals(128, height)
        assertTrue(width % 2 == 0)
        assertTrue(height % 2 == 0)
    }

    @Test
    fun testSymmetricPaddingOffsets() {
        // Landscape 512x384 (4:3)
        val sWidth = 512
        val sHeight = 384
        val padLeft = (512 - sWidth) / 2
        val padTop = (512 - sHeight) / 2
        assertEquals(0, padLeft)
        assertEquals(64, padTop)

        // Portrait 384x512 (3:4)
        val pWidth = 384
        val pHeight = 512
        val pPadLeft = (512 - pWidth) / 2
        val pPadTop = (512 - pHeight) / 2
        assertEquals(64, pPadLeft)
        assertEquals(0, pPadTop)
    }
}
