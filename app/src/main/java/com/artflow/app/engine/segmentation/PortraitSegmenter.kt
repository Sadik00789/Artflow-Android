package com.artflow.app.engine.segmentation

import android.graphics.Bitmap
import android.util.Log
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.artflow.app.core.storage.AssetModelReader
import com.artflow.app.engine.DynamicTensorHandler
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * Executes on-device portrait selfie segmentation to detect subjects and produce a raw alpha mask.
 * Runs strictly on CPU via multi-threaded XNNPACK to prevent OpenCL GPU context contention.
 */
class PortraitSegmenter(
    private val modelReader: AssetModelReader,
    private val tensorHandler: DynamicTensorHandler,
    private val dispatchers: DispatcherProvider
) : AutoCloseable {

    companion object {
        private const val TAG = "PortraitSegmenter"
        private const val MODEL_PATH = "models/vision/selfie_segmenter.tflite"
    }

    private var interpreter: Interpreter? = null

    /**
     * Generates a 1-channel raw probability mask (values between 0.0 and 1.0) with identical dimensions to [bitmap].
     */
    suspend fun segmentPortrait(bitmap: Bitmap): Result<FloatArray> = withContext(dispatchers.ml) {
        try {
            val width = bitmap.width
            val height = bitmap.height

            val activeInterpreter = getOrInitInterpreter()

            // Reshape dynamic input tensor [1, height, width, 3] on CPU
            val currentShape = activeInterpreter.getInputTensor(0).shape()
            val targetShape = intArrayOf(1, height, width, 3)
            if (!currentShape.contentEquals(targetShape)) {
                activeInterpreter.resizeInput(0, targetShape)
                activeInterpreter.allocateTensors()
            }

            val inputBuffer = tensorHandler.createFloatBuffer(height, width, 3)
            val outputBuffer = tensorHandler.createFloatBuffer(height, width, 1)

            tensorHandler.bitmapToFloatBuffer(bitmap, inputBuffer)

            activeInterpreter.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val mask = FloatArray(width * height)
            for (i in 0 until width * height) {
                mask[i] = outputBuffer.float.coerceIn(0f, 1f)
            }

            Result.Success(mask)
        } catch (e: Throwable) {
            Log.e(TAG, "Portrait segmentation failed: ${e.message}", e)
            Result.Error(e, "Segmentation failed: ${e.localizedMessage}")
        }
    }

    private fun getOrInitInterpreter(): Interpreter {
        if (interpreter == null) {
            val buffer = modelReader.loadModelFile(MODEL_PATH)
            val cpuOptions = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(buffer, cpuOptions)
        }
        return interpreter!!
    }

    override fun close() {
        try {
            interpreter?.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Error closing PortraitSegmenter interpreter: ${e.message}")
        }
    }
}
