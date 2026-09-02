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
        val (width, height) = ImageNormalizer.calculateDimensions(4032, 3024, maxDim = 768)
        assertEquals(768, width)
        assertEquals(576, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testPortraitNormalization() {
        // Standard 3:4 portrait photo
        val (width, height) = ImageNormalizer.calculateDimensions(3024, 4032, maxDim = 768)
        assertEquals(576, width)
        assertEquals(768, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testSquareNormalization() {
        val (width, height) = ImageNormalizer.calculateDimensions(1080, 1080, maxDim = 768)
        assertEquals(768, width)
        assertEquals(768, height)
        assertTrue("Width must be even", width % 2 == 0)
        assertTrue("Height must be even", height % 2 == 0)
    }

    @Test
    fun testOddDimensionSnapping() {
        // Arbitrary dimensions resulting in odd numbers
        val (width, height) = ImageNormalizer.calculateDimensions(1920, 1081, maxDim = 768)
        assertEquals(768, width)
        assertTrue("Calculated height must be snapped to even integer", height % 2 == 0)
    }

    @Test
    fun testCustomMaxDimension() {
        val (width, height) = ImageNormalizer.calculateDimensions(1000, 500, maxDim = 512)
        assertEquals(512, width)
        assertEquals(256, height)
        assertTrue(width % 2 == 0)
        assertTrue(height % 2 == 0)
    }
}
