package com.artflow.app.engine.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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

class PortraitSegmenter(
    private val context: Context,
    private val dispatchers: DispatcherProvider
) : AutoCloseable {

    companion object {
        private const val TAG = "PortraitSegmenter"
        private const val MODEL_PATH = "models/vision/selfie_segmenter.tflite"
    }

    private var segmenter: ImageSegmenter? = null

    suspend fun segmentPortrait(bitmap: Bitmap): Result<FloatArray> = withContext(dispatchers.default) {
        try {
            val activeSegmenter = getOrInitSegmenter()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val segmentationResult = activeSegmenter.segment(mpImage)

            val confidenceMasksOpt = segmentationResult.confidenceMasks()
            if (!confidenceMasksOpt.isPresent || confidenceMasksOpt.get().isEmpty()) {
                return@withContext Result.Error(IllegalStateException("No confidence mask produced"))
            }

            val maskList = confidenceMasksOpt.get()
            // MediaPipe ImageSegmenter: Index 0 is Background, Index 1 is Person
            val maskImage = if (maskList.size > 1) maskList[1] else maskList[0]
            val maskWidth = maskImage.width
            val maskHeight = maskImage.height
            val maskBuffer = ByteBufferExtractor.extract(maskImage).asFloatBuffer()
            maskBuffer.rewind()

            // 1. Pack MediaPipe confidence mask into a native grayscale Bitmap
            val intermediatePixels = IntArray(maskWidth * maskHeight)
            for (i in 0 until (maskWidth * maskHeight)) {
                val confidence = maskBuffer.get().coerceIn(0.0f, 1.0f)
                val gray = (confidence * 255f).toInt()
                intermediatePixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            }

            val rawMaskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
            rawMaskBitmap.setPixels(intermediatePixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

            // 2. Hardware bicubic rescale to match normalized canvas dimensions
            val targetWidth = bitmap.width
            val targetHeight = bitmap.height
            val scaledMaskBitmap = if (maskWidth == targetWidth && maskHeight == targetHeight) {
                rawMaskBitmap
            } else {
                Bitmap.createScaledBitmap(rawMaskBitmap, targetWidth, targetHeight, true)
            }

            // 3. Extract 2D rescaled float array
            val totalPixels = targetWidth * targetHeight
            val rescaledPixels = IntArray(totalPixels)
            scaledMaskBitmap.getPixels(rescaledPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

            val finalMask = FloatArray(totalPixels)
            for (i in 0 until totalPixels) {
                finalMask[i] = (rescaledPixels[i] and 0xFF) / 255.0f
            }

            Result.Success(finalMask)
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
