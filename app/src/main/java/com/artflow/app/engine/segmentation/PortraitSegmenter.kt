package com.artflow.app.engine.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter.ImageSegmenterOptions
import kotlinx.coroutines.withContext

/**
 * Executes on-device portrait selfie segmentation using official Google MediaPipe ImageSegmenter.
 * Uses the authentic selfie_segmenter.tflite model with native MediaPipe custom op support.
 */
class PortraitSegmenter(
    private val context: Context,
    private val dispatchers: DispatcherProvider
) : AutoCloseable {

    companion object {
        private const val TAG = "PortraitSegmenter"
        private const val MODEL_PATH = "models/vision/selfie_segmenter.tflite"
    }

    private var segmenter: ImageSegmenter? = null

    /**
     * Generates a 1-channel raw probability mask (values between 0.0 and 1.0) with identical dimensions to [bitmap].
     */
    suspend fun segmentPortrait(bitmap: Bitmap): Result<FloatArray> = withContext(dispatchers.ml) {
        try {
            val activeSegmenter = getOrInitSegmenter()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val segmentationResult = activeSegmenter.segment(mpImage)

            val confidenceMasksOpt = segmentationResult.confidenceMasks()
            if (!confidenceMasksOpt.isPresent || confidenceMasksOpt.get().isEmpty()) {
                return@withContext Result.Error(IllegalStateException("No confidence mask produced"))
            }

            val maskImage = confidenceMasksOpt.get()[0]
            val maskBuffer = ByteBufferExtractor.extract(maskImage).asFloatBuffer()
            val totalPixels = bitmap.width * bitmap.height
            val maskArray = FloatArray(totalPixels)

            maskBuffer.rewind()
            val copyCount = minOf(maskBuffer.remaining(), totalPixels)
            maskBuffer.get(maskArray, 0, copyCount)

            Result.Success(maskArray)
        } catch (e: Throwable) {
            Log.e(TAG, "Portrait segmentation failed: ${e.message}", e)
            Result.Error(e, "Segmentation failed: ${e.localizedMessage}")
        }
    }

    private fun getOrInitSegmenter(): ImageSegmenter {
        if (segmenter == null) {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = ImageSegmenterOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setOutputConfidenceMasks(true)
                .setOutputCategoryMask(false)
                .build()

            segmenter = ImageSegmenter.createFromOptions(context, options)
        }
        return segmenter!!
    }

    override fun close() {
        try {
            segmenter?.close()
            segmenter = null
        } catch (e: Throwable) {
            Log.e(TAG, "Error closing PortraitSegmenter: ${e.message}")
        }
    }
}
