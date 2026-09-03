package com.artflow.app

import com.artflow.app.engine.processing.ImageNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying canvas dimension normalization to 1024px baseline and even integer snapping.
 */
class ImageNormalizerTest {

    @Test
    fun testLandscapeNormalization() {
        // Standard 4:3 12MP camera photo with 1024px baseline
        val (width, height) = ImageNormalizer.calculateDimensions(4032, 3024, maxDim = 1024)
        assertEquals(1024, width)
        assertEquals(768, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testPortraitNormalization() {
        // Standard 3:4 portrait photo with 1024px baseline
        val (width, height) = ImageNormalizer.calculateDimensions(3024, 4032, maxDim = 1024)
        assertEquals(768, width)
        assertEquals(1024, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testSquareNormalization() {
        val (width, height) = ImageNormalizer.calculateDimensions(1080, 1080, maxDim = 1024)
        assertEquals(1024, width)
        assertEquals(1024, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testOddDimensionSnapping() {
        // Arbitrary dimensions resulting in odd numbers
        val (width, height) = ImageNormalizer.calculateDimensions(1920, 1081, maxDim = 1024)
        assertEquals(1024, width)
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
        // Landscape 1024x768 (4:3)
        val sWidth = 1024
        val sHeight = 768
        val padLeft = (1024 - sWidth) / 2
        val padTop = (1024 - sHeight) / 2
        assertEquals(0, padLeft)
        assertEquals(128, padTop)

        // Portrait 768x1024 (3:4)
        val pWidth = 768
        val pHeight = 1024
        val pPadLeft = (1024 - pWidth) / 2
        val pPadTop = (1024 - pHeight) / 2
        assertEquals(128, pPadLeft)
        assertEquals(0, pPadTop)
    }
}
