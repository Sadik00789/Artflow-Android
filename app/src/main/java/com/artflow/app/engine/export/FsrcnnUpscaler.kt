package com.artflow.app.engine.export

import android.graphics.Bitmap
import android.util.Log
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.artflow.app.core.storage.AssetModelReader
import com.artflow.app.engine.DynamicTensorHandler
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * Step 1 of Studio Export Pipeline:
 * On-device FSRCNN 2x super-resolution upscaler (expanding 768px stylized output to 1536px).
 * Runs strictly on CPU via multi-threaded XNNPACK to preserve GPU context exclusively for style transfer.
 */
class FsrcnnUpscaler(
    private val modelReader: AssetModelReader,
    private val tensorHandler: DynamicTensorHandler,
    private val dispatchers: DispatcherProvider
) : AutoCloseable {

    companion object {
        private const val TAG = "FsrcnnUpscaler"
        private const val MODEL_PATH = "models/vision/fsrcnn_x2_fp16.tflite"
    }

    private var interpreter: Interpreter? = null

    /**
     * Upscales [bitmap] 2x using the FSRCNN neural network.
     */
    suspend fun upscale2x(bitmap: Bitmap): Result<Bitmap> = withContext(dispatchers.ml) {
        try {
            val inWidth = bitmap.width
            val inHeight = bitmap.height
            val outWidth = inWidth * 2
            val outHeight = inHeight * 2

            val activeInterpreter = getOrInitInterpreter()

            // Dynamic reshape on CPU
            val currentShape = activeInterpreter.getInputTensor(0).shape()
            val targetShape = intArrayOf(1, inHeight, inWidth, 3)
            if (!currentShape.contentEquals(targetShape)) {
                activeInterpreter.resizeInput(0, targetShape)
                activeInterpreter.allocateTensors()
            }

            val inputBuffer = tensorHandler.createFloatBuffer(inHeight, inWidth, 3)
            val outputBuffer = tensorHandler.createFloatBuffer(outHeight, outWidth, 3)

            tensorHandler.bitmapToFloatBuffer(bitmap, inputBuffer)

            val startTime = System.currentTimeMillis()
            activeInterpreter.run(inputBuffer, outputBuffer)
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "FSRCNN 2x upscaled ${inWidth}x${inHeight} -> ${outWidth}x${outHeight} in ${duration}ms")

            val upscaledBitmap = tensorHandler.floatBufferToBitmap(outputBuffer, outWidth, outHeight)
            Result.Success(upscaledBitmap)
        } catch (e: Throwable) {
            Log.e(TAG, "FSRCNN 2x upscaling failed: ${e.message}", e)
            Result.Error(e, "FSRCNN 2x upscaling failed: ${e.localizedMessage}")
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
            Log.e(TAG, "Error closing FsrcnnUpscaler interpreter: ${e.message}")
        }
    }
}
