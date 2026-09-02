package com.artflow.app.engine

import android.graphics.Bitmap
import android.util.Log
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.artflow.app.core.storage.AssetModelReader
import com.artflow.app.model.StylePreset
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * Core neural style transfer execution engine.
 * Thread-safe and strictly bound to [DispatcherProvider.ml] to prevent OpenCL context collision on the Adreno 619.
 */
class StyleTransferEngine(
    private val modelReader: AssetModelReader,
    private val gpuDelegateProvider: GpuDelegateProvider,
    private val lruCache: ModelLruCache,
    private val tensorHandler: DynamicTensorHandler,
    private val dispatchers: DispatcherProvider
) {

    companion object {
        private const val TAG = "StyleTransferEngine"
    }

    /**
     * Executes neural style transfer on the provided bitmap using the specified [StylePreset].
     * Target resolution is typically 768px (normalized canvas).
     */
    suspend fun executeInference(
        bitmap: Bitmap,
        style: StylePreset
    ): Result<Bitmap> = withContext(dispatchers.ml) {
        try {
            val width = bitmap.width
            val height = bitmap.height

            // 1. Fetch or load Interpreter through the 2-slot ModelLruCache
            val cachedModel = getOrLoadModel(style.modelAssetPath)
            val interpreter = cachedModel.interpreter

            // 2. Reshape dynamic input tensor [1, height, width, 3]
            tensorHandler.reshapeInput(interpreter, height, width, 3)

            // 3. Prepare direct native ByteBuffers
            val inputBuffer = tensorHandler.createFloatBuffer(height, width, 3)
            val outputBuffer = tensorHandler.createFloatBuffer(height, width, 3)

            // 4. Fill input buffer with [0.0, 255.0] RGB floats
            tensorHandler.bitmapToFloatBuffer(bitmap, inputBuffer)

            // 5. Execute inference
            val startTime = System.currentTimeMillis()
            interpreter.run(inputBuffer, outputBuffer)
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Inference completed for style '${style.name}' (${width}x${height}) in ${duration}ms")

            // 6. Convert output float buffer to Bitmap
            val stylizedBitmap = tensorHandler.floatBufferToBitmap(outputBuffer, width, height)

            Result.Success(stylizedBitmap)
        } catch (e: Throwable) {
            Log.e(TAG, "Inference failed for style '${style.name}': ${e.message}", e)
            Result.Error(e, "Inference failed: ${e.localizedMessage}")
        }
    }

    private fun getOrLoadModel(assetPath: String): CachedModel {
        var cached = lruCache.get(assetPath)
        if (cached != null) {
            return cached
        }

        Log.d(TAG, "Loading model from assets: $assetPath")
        val modelBuffer = modelReader.loadModelFile(assetPath)
        val optionsHolder = gpuDelegateProvider.createInterpreterOptions()
        val interpreter = Interpreter(modelBuffer, optionsHolder.options)

        cached = CachedModel(interpreter, optionsHolder)
        lruCache.put(assetPath, cached)
        return cached
    }
}
