package com.artflow.app.engine

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handles dynamic tensor reshaping and native direct [ByteBuffer] conversions between Bitmaps and Float32 tensors.
 * Employs pre-allocated pinned memory and fast integer math to eliminate heap allocations and ART GC pauses.
 */
class DynamicTensorHandler {

    companion object {
        const val STATIC_DIMENSION = 1024
        const val CHANNELS = 3
        const val BYTES_PER_FLOAT = 4
        const val STATIC_BUFFER_CAPACITY = 1 * STATIC_DIMENSION * STATIC_DIMENSION * CHANNELS * BYTES_PER_FLOAT
    }

    /** Pre-allocated direct buffer for static 1024x1024x3 float input tensor (~12.58 MB) */
    val staticInputBuffer: ByteBuffer = ByteBuffer.allocateDirect(STATIC_BUFFER_CAPACITY).apply {
        order(ByteOrder.nativeOrder())
    }

    /** Pre-allocated direct buffer for static 1024x1024x3 float output tensor (~12.58 MB) */
    val staticOutputBuffer: ByteBuffer = ByteBuffer.allocateDirect(STATIC_BUFFER_CAPACITY).apply {
        order(ByteOrder.nativeOrder())
    }

    // Pre-allocated pinned working arrays to eliminate ~33.5 MB of Large Object Space heap allocations per pass
    private val reusablePixels = IntArray(STATIC_DIMENSION * STATIC_DIMENSION)
    private val reusableFloatArray = FloatArray(STATIC_DIMENSION * STATIC_DIMENSION * CHANNELS)

    /**
     * Allocates a native direct [ByteBuffer] formatted for float32 elements (for auxiliary CPU models).
     */
    fun createFloatBuffer(height: Int, width: Int, channels: Int = 3): ByteBuffer {
        val capacity = 1 * height * width * channels * BYTES_PER_FLOAT
        return ByteBuffer.allocateDirect(capacity).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    /**
     * Converts a [Bitmap] into an RGB Float32 [ByteBuffer] with pixel values in range [0.0, 255.0].
     * Uses pre-allocated working buffers to achieve zero runtime heap allocation.
     */
    @Synchronized
    fun bitmapToFloatBuffer(bitmap: Bitmap, targetBuffer: ByteBuffer) {
        targetBuffer.rewind()
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        val pixels = if (totalPixels <= reusablePixels.size) reusablePixels else IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val totalFloats = totalPixels * CHANNELS
        val floatArray = if (totalFloats <= reusableFloatArray.size) reusableFloatArray else FloatArray(totalFloats)

        var floatIdx = 0
        for (i in 0 until totalPixels) {
            val pixel = pixels[i]
            floatArray[floatIdx++] = ((pixel shr 16) and 0xFF).toFloat()
            floatArray[floatIdx++] = ((pixel shr 8) and 0xFF).toFloat()
            floatArray[floatIdx++] = (pixel and 0xFF).toFloat()
        }
        targetBuffer.asFloatBuffer().put(floatArray, 0, totalFloats)
        targetBuffer.rewind()
    }

    /**
     * Converts an RGB Float32 [ByteBuffer] with pixel values in range [0.0, 255.0] into a [Bitmap].
     * Uses fast integer clamping and reusable working buffers to eliminate GC pauses.
     */
    @Synchronized
    fun floatBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        buffer.rewind()
        val totalPixels = width * height
        val totalFloats = totalPixels * CHANNELS

        val floatArray = if (totalFloats <= reusableFloatArray.size) reusableFloatArray else FloatArray(totalFloats)
        buffer.asFloatBuffer().get(floatArray, 0, totalFloats)

        val pixels = if (totalPixels <= reusablePixels.size) reusablePixels else IntArray(totalPixels)
        var floatIdx = 0
        for (i in 0 until totalPixels) {
            val rf = floatArray[floatIdx++]
            val gf = floatArray[floatIdx++]
            val bf = floatArray[floatIdx++]

            val r = if (rf <= 0f) 0 else if (rf >= 255f) 255 else (rf + 0.5f).toInt()
            val g = if (gf <= 0f) 0 else if (gf >= 255f) 255 else (gf + 0.5f).toInt()
            val b = if (bf <= 0f) 0 else if (bf >= 255f) 255 else (bf + 0.5f).toInt()

            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    /**
     * Converts a 1-channel Float32 mask [ByteBuffer] into an alpha [Bitmap] or FloatArray.
     */
    @Synchronized
    fun maskBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        buffer.rewind()
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val totalPixels = width * height
        val pixels = if (totalPixels <= reusablePixels.size) reusablePixels else IntArray(totalPixels)

        var idx = 0
        for (i in 0 until totalPixels) {
            val alpha = (buffer.float.coerceIn(0f, 1f) * 255f).toInt()
            pixels[idx++] = (alpha shl 24) or (255 shl 16) or (255 shl 8) or 255
        }

        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }
}
