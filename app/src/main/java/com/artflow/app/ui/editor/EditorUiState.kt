package com.artflow.app.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import com.artflow.app.engine.export.ExportStage
import com.artflow.app.model.EditorSettings
import com.artflow.app.model.StylePreset

/**
 * UI State for the ArtFlow Studio Editor.
 */
sealed interface EditorUiState {

    /**
     * Initial state when no photo has been chosen.
     */
    object Idle : EditorUiState

    /**
     * Inference or segmentation processing is underway.
     */
    data class Processing(
        val canvasBitmap: Bitmap,
        val style: StylePreset,
        val previousStylizedBitmap: Bitmap? = null,
        val statusMessage: String = "Rendering neural style…"
    ) : EditorUiState

    /**
     * Art rendering is active and displayed on the interactive WYSIWYG canvas.
     */
    data class Success(
        val originalPhoto: Bitmap,
        val canvasOriginal: Bitmap,
        val stylizedCanvas: Bitmap,
        val compositePreview: Bitmap,
        val segmentationMask: FloatArray?,
        val selectedStyle: StylePreset,
        val settings: EditorSettings,
        val exportedUri: Uri? = null
    ) : EditorUiState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            return originalPhoto == other.originalPhoto &&
                   canvasOriginal == other.canvasOriginal &&
                   stylizedCanvas == other.stylizedCanvas &&
                   compositePreview == other.compositePreview &&
                   java.util.Arrays.equals(segmentationMask, other.segmentationMask) &&
                   selectedStyle == other.selectedStyle &&
                   settings == other.settings &&
                   exportedUri == other.exportedUri
        }

        override fun hashCode(): Int {
            var result = originalPhoto.hashCode()
            result = 31 * result + canvasOriginal.hashCode()
            result = 31 * result + stylizedCanvas.hashCode()
            result = 31 * result + compositePreview.hashCode()
            result = 31 * result + (segmentationMask?.contentHashCode() ?: 0)
            result = 31 * result + selectedStyle.hashCode()
            result = 31 * result + settings.hashCode()
            result = 31 * result + (exportedUri?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * Multi-stage 1536px high-resolution export pipeline is executing.
     */
    data class Exporting(
        val stage: ExportStage,
        val progressFraction: Float,
        val previewBitmap: Bitmap
    ) : EditorUiState

    /**
     * Failure state displaying a user-friendly error message.
     */
    data class Error(val message: String) : EditorUiState
}
