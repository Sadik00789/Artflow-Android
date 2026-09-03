package com.artflow.app.engine.export

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.artflow.app.core.storage.MediaStoreWriter
import com.artflow.app.model.StylePreset
import kotlinx.coroutines.withContext

/**
 * Stages of the studio export pipeline for progress HUD reporting.
 */
sealed class ExportStage(val description: String, val progressFraction: Float) {
    object SuperResolution : ExportStage("Upscaling with FSRCNN 2x…", 0.25f)
    object DetailInjection : ExportStage("Injecting high-frequency luminance detail…", 0.55f)
    object EdgeSharpening : ExportStage("Applying edge-aware thresholded sharpening…", 0.80f)
    object Saving : ExportStage("Writing artwork with EXIF tags to Gallery…", 0.95f)
    data class Complete(val uri: Uri) : ExportStage("Artwork successfully exported!", 1.0f)
}

/**
 * Coordinates the full multi-stage high-resolution export pipeline.
 */
class HighResExportPipeline(
    private val fsrcnnUpscaler: FsrcnnUpscaler,
    private val mediaStoreWriter: MediaStoreWriter,
    private val dispatchers: DispatcherProvider
) {

    companion object {
        private const val TAG = "HighResExportPipeline"
    }

    /**
     * Executes the complete studio export workflow:
     * 768px stylized image -> 1536px FSRCNN 2x -> Luminance Detail Injection -> Thresholded Unsharp Mask -> Subject Preservation -> MediaStore.
     */
    suspend fun executeExport(
        originalHighResPhoto: Bitmap,
        canvasStylizedImage: Bitmap,
        style: StylePreset,
        subjectBlend: Float = 0.0f,
        segmentationMask: FloatArray? = null,
        maskWidth: Int = 0,
        maskHeight: Int = 0,
        onProgress: (ExportStage) -> Unit = {}
    ): Result<Uri> = withContext(dispatchers.io) {
        try {
            // Stage 1: FSRCNN 2x Super-Resolution
            onProgress(ExportStage.SuperResolution)
            Log.d(TAG, "Stage 1: Beginning FSRCNN 2x super-resolution...")
            val upscaleResult = fsrcnnUpscaler.upscale2x(canvasStylizedImage)
            if (upscaleResult is Result.Error) {
                return@withContext Result.Error(upscaleResult.exception, "Super-resolution stage failed: ${upscaleResult.message}")
            }
            val upscaled = (upscaleResult as Result.Success).data

            // Stage 2: Micro-Detail Frequency Transfer
            onProgress(ExportStage.DetailInjection)
            Log.d(TAG, "Stage 2: Injecting high-frequency luminance detail...")
            val detailRestored = LuminanceDetailInjector.injectDetail(originalHighResPhoto, upscaled)

            // Stage 3: Thresholded Unsharp Masking
            onProgress(ExportStage.EdgeSharpening)
            Log.d(TAG, "Stage 3: Applying thresholded edge-aware unsharp mask...")
            val sharpened = ThresholdedUnsharpMask.applySharpening(detailRestored)

            // Stage 3b: Subject Sharpness Preservation
            val finalArtwork = if (subjectBlend > 0.0f && segmentationMask != null && maskWidth > 0 && maskHeight > 0) {
                Log.d(TAG, "Preserving original high-res subject with blend: $subjectBlend")
                compositeSubjectAtExport(
                    original = originalHighResPhoto,
                    stylized = sharpened,
                    rawMask = segmentationMask,
                    maskWidth = maskWidth,
                    maskHeight = maskHeight,
                    subjectBlend = subjectBlend
                )
            } else {
                sharpened
            }

            // Stage 4: MediaStore Export with EXIF tags
            onProgress(ExportStage.Saving)
            Log.d(TAG, "Stage 4: Writing artwork to device MediaStore...")
            val saveResult = mediaStoreWriter.saveArtwork(finalArtwork, "ArtFlow_${style.name}", style.name)
            if (saveResult is Result.Success) {
                onProgress(ExportStage.Complete(saveResult.data))
            }

            saveResult
        } catch (e: Throwable) {
            Log.e(TAG, "High-resolution export pipeline failed: ${e.message}", e)
            Result.Error(e, "Export pipeline failed: ${e.localizedMessage}")
        }
    }

    private fun compositeSubjectAtExport(
        original: Bitmap,
        stylized: Bitmap,
        rawMask: FloatArray,
        maskWidth: Int,
        maskHeight: Int,
        subjectBlend: Float
    ): Bitmap {
        val outWidth = stylized.width
        val outHeight = stylized.height

        val scaledOriginal = if (original.width != outWidth || original.height != outHeight) {
            Bitmap.createScaledBitmap(original, outWidth, outHeight, true)
        } else {
            original
        }

        val totalPixels = outWidth * outHeight
        val origPixels = IntArray(totalPixels)
        val stylePixels = IntArray(totalPixels)
        val compositePixels = IntArray(totalPixels)

        scaledOriginal.getPixels(origPixels, 0, outWidth, 0, 0, outWidth, outHeight)
        stylized.getPixels(stylePixels, 0, outWidth, 0, 0, outWidth, outHeight)

        val scaleX = maskWidth.toFloat() / outWidth.toFloat()
        val scaleY = maskHeight.toFloat() / outHeight.toFloat()

        for (y in 0 until outHeight) {
            val maskY = ((y * scaleY).toInt()).coerceIn(0, maskHeight - 1)
            val maskRowOffset = maskY * maskWidth
            val outRowOffset = y * outWidth

            for (x in 0 until outWidth) {
                val maskX = ((x * scaleX).toInt()).coerceIn(0, maskWidth - 1)
                val alpha = (rawMask[maskRowOffset + maskX] * subjectBlend).coerceIn(0.0f, 1.0f)

                if (alpha <= 0.001f) {
                    compositePixels[outRowOffset + x] = stylePixels[outRowOffset + x]
                } else if (alpha >= 0.999f) {
                    compositePixels[outRowOffset + x] = origPixels[outRowOffset + x]
                } else {
                    val oPixel = origPixels[outRowOffset + x]
                    val sPixel = stylePixels[outRowOffset + x]

                    val oR = (oPixel shr 16) and 0xFF
                    val oG = (oPixel shr 8) and 0xFF
                    val oB = oPixel and 0xFF

                    val sR = (sPixel shr 16) and 0xFF
                    val sG = (sPixel shr 8) and 0xFF
                    val sB = sPixel and 0xFF

                    val invAlpha = 1.0f - alpha
                    val r = (oR * alpha + sR * invAlpha).toInt().coerceIn(0, 255)
                    val g = (oG * alpha + sG * invAlpha).toInt().coerceIn(0, 255)
                    val b = (oB * alpha + sB * invAlpha).toInt().coerceIn(0, 255)

                    compositePixels[outRowOffset + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        val result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        result.setPixels(compositePixels, 0, outWidth, 0, 0, outWidth, outHeight)
        return result
    }
}
