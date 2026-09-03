package com.artflow.app.engine

import android.graphics.Bitmap
import android.util.Log
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.artflow.app.core.storage.AssetModelReader
import com.artflow.app.engine.processing.ImageNormalizer
import com.artflow.app.engine.processing.StylePostProcessor
import com.artflow.app.model.StylePreset
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * Core neural style transfer execution engine.
 * Thread-safe and strictly bound to [DispatcherProvider.ml] to prevent OpenCL context collision on the Adreno 619.
 * Operates on static 768x768 tensors to satisfy OpenCL delegate requirements.
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
     * Uses static 512x512 canvas with symmetric padding and cropping to preserve aspect ratio.
     */
    suspend fun executeInference(
        bitmap: Bitmap,
        style: StylePreset
    ): Result<Bitmap> = withContext(dispatchers.ml) {
        try {
            // 1. Fetch or load Interpreter through the 2-slot ModelLruCache
            val cachedModel = getOrLoadModel(style.modelAssetPath)
            val interpreter = cachedModel.interpreter

            val t0 = System.currentTimeMillis()

            // 2. Pad to static 1024x1024 square
            val (paddedInput, padding) = ImageNormalizer.padToSquare1024(bitmap)

            // 3. Populate pre-allocated static 1024x1024 input buffer
            val inputBuffer = tensorHandler.staticInputBuffer
            val outputBuffer = tensorHandler.staticOutputBuffer
            tensorHandler.bitmapToFloatBuffer(paddedInput, inputBuffer)

            val t1 = System.currentTimeMillis()

            // 4. Execute inference on static 1024x1024 canvas
            interpreter.run(inputBuffer, outputBuffer)

            val t2 = System.currentTimeMillis()

            // 5. Convert output float buffer to Bitmap
            val paddedOutput = tensorHandler.floatBufferToBitmap(
                outputBuffer,
                DynamicTensorHandler.STATIC_DIMENSION,
                DynamicTensorHandler.STATIC_DIMENSION
            )

            // 6. Crop back to original normalized dimensions
            val croppedOutput = ImageNormalizer.cropFromSquare1024(paddedOutput, padding)

            // 7. Apply tailored aesthetic color and tone grading per style preset
            val finalizedArt = StylePostProcessor.applyAestheticGrading(croppedOutput, style.id)

            val t3 = System.currentTimeMillis()
            Log.i(
                TAG,
                "[Profiling] '${style.name}': Preprocess=${t1 - t0}ms, GPU Run=${t2 - t1}ms, " +
                    "Postprocess=${t3 - t2}ms, Total=${t3 - t0}ms"
            )

            Result.Success(finalizedArt)
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
        val optionsHolder = gpuDelegateProvider.createInterpreterOptions(assetPath)

        val cachedModel = try {
            val gpuInterpreter = Interpreter(modelBuffer, optionsHolder.options)
            CachedModel(gpuInterpreter, optionsHolder)
        } catch (e: Throwable) {
            Log.w(TAG, "GPU delegate application failed for $assetPath, falling back to CPU XNNPACK", e)
            try {
                optionsHolder.close()
            } catch (ignored: Throwable) {}

            val cpuOptions = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            val cpuInterpreter = Interpreter(modelBuffer, cpuOptions)
            CachedModel(cpuInterpreter, null)
        }

        lruCache.put(assetPath, cachedModel)
        return cachedModel
    }
}
