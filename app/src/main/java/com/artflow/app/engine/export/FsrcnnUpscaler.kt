package com.artflow.app.engine.export

import android.graphics.Bitmap
import android.util.Log
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.artflow.app.core.storage.AssetModelReader
import com.artflow.app.engine.DynamicTensorHandler
import com.artflow.app.engine.GpuDelegateProvider
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * Step 1 of Studio Export Pipeline:
 * On-device FSRCNN 2x super-resolution upscaler (expanding 768px stylized output to 1536px).
 */
class FsrcnnUpscaler(
    private val modelReader: AssetModelReader,
    private val gpuDelegateProvider: GpuDelegateProvider,
    private val tensorHandler: DynamicTensorHandler,
    private val dispatchers: DispatcherProvider
) : AutoCloseable {

    companion object {
        private const val TAG = "FsrcnnUpscaler"
        private const val MODEL_PATH = "models/vision/fsrcnn_x2_fp16.tflite"
    }

    private var interpreter: Interpreter? = null
    private var optionsHolder: GpuDelegateProvider.InterpreterOptionsHolder? = null

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

            tensorHandler.reshapeInput(activeInterpreter, inHeight, inWidth, 3)

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
            val holder = gpuDelegateProvider.createInterpreterOptions()
            optionsHolder = holder
            interpreter = Interpreter(buffer, holder.options)
        }
        return interpreter!!
    }

    override fun close() {
        try {
            interpreter?.close()
        } finally {
            optionsHolder?.close()
        }
    }
}
