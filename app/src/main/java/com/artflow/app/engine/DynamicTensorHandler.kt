package com.artflow.app.engine

import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handles dynamic tensor reshaping and native direct [ByteBuffer] conversions between Bitmaps and Float32 tensors.
 */
class DynamicTensorHandler {

    companion object {
        const val STATIC_DIMENSION = 768
        const val CHANNELS = 3
        const val BYTES_PER_FLOAT = 4
        const val STATIC_BUFFER_CAPACITY = 1 * STATIC_DIMENSION * STATIC_DIMENSION * CHANNELS * BYTES_PER_FLOAT
    }

    /** Pre-allocated direct buffer for static 768x768x3 float input tensor */
    val staticInputBuffer: ByteBuffer = ByteBuffer.allocateDirect(STATIC_BUFFER_CAPACITY).apply {
        order(ByteOrder.nativeOrder())
    }

    /** Pre-allocated direct buffer for static 768x768x3 float output tensor */
    val staticOutputBuffer: ByteBuffer = ByteBuffer.allocateDirect(STATIC_BUFFER_CAPACITY).apply {
        order(ByteOrder.nativeOrder())
    }

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
     */
    fun bitmapToFloatBuffer(bitmap: Bitmap, targetBuffer: ByteBuffer) {
        targetBuffer.rewind()
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var idx = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[idx++]
                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                targetBuffer.putFloat(r)
                targetBuffer.putFloat(g)
                targetBuffer.putFloat(b)
            }
        }
        targetBuffer.rewind()
    }

    /**
     * Converts an RGB Float32 [ByteBuffer] with pixel values in range [0.0, 255.0] into a [Bitmap].
     */
    fun floatBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        buffer.rewind()
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        var idx = 0
        for (i in 0 until width * height) {
            val r = buffer.float.coerceIn(0f, 255f).toInt()
            val g = buffer.float.coerceIn(0f, 255f).toInt()
            val b = buffer.float.coerceIn(0f, 255f).toInt()

            pixels[idx++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    /**
     * Converts a 1-channel Float32 mask [ByteBuffer] into an alpha [Bitmap] or FloatArray.
     */
    fun maskBufferToBitmap(buffer: ByteBuffer, width: Int, height: Int): Bitmap {
        buffer.rewind()
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        var idx = 0
        for (i in 0 until width * height) {
            val alpha = (buffer.float.coerceIn(0f, 1f) * 255f).toInt()
            pixels[idx++] = (alpha shl 24) or (255 shl 16) or (255 shl 8) or 255
        }

        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }
}
