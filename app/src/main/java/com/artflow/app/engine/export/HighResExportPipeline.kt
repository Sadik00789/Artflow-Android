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
     * 768px stylized image -> 1536px FSRCNN 2x -> Luminance Detail Injection -> Thresholded Unsharp Mask -> MediaStore.
     */
    suspend fun executeExport(
        originalHighResPhoto: Bitmap,
        canvasStylizedImage: Bitmap,
        style: StylePreset,
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

            // Stage 4: MediaStore Export with EXIF tags
            onProgress(ExportStage.Saving)
            Log.d(TAG, "Stage 4: Writing artwork to device MediaStore...")
            val saveResult = mediaStoreWriter.saveArtwork(sharpened, "ArtFlow_${style.name}", style.name)
            if (saveResult is Result.Success) {
                onProgress(ExportStage.Complete(saveResult.data))
            }

            saveResult
        } catch (e: Throwable) {
            Log.e(TAG, "High-resolution export pipeline failed: ${e.message}", e)
            Result.Error(e, "Export pipeline failed: ${e.localizedMessage}")
        }
    }
}
