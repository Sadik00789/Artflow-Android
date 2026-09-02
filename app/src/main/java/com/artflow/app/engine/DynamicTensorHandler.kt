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

    /**
     * Reshapes input tensor 0 of the interpreter to [1, height, width, 3] and reallocates tensors.
     */
    fun reshapeInput(interpreter: Interpreter, height: Int, width: Int, channels: Int = 3) {
        val currentShape = interpreter.getInputTensor(0).shape()
        val targetShape = intArrayOf(1, height, width, channels)
        if (!currentShape.contentEquals(targetShape)) {
            interpreter.resizeInput(0, targetShape)
            interpreter.allocateTensors()
        }
    }

    /**
     * Allocates a native direct [ByteBuffer] formatted for float32 elements.
     */
    fun createFloatBuffer(height: Int, width: Int, channels: Int = 3): ByteBuffer {
        val bytesPerFloat = 4
        val capacity = 1 * height * width * channels * bytesPerFloat
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
