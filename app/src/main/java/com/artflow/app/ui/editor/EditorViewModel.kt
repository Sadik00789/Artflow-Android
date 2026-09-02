package com.artflow.app.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.artflow.app.engine.StyleTransferEngine
import com.artflow.app.engine.export.ExportStage
import com.artflow.app.engine.export.HighResExportPipeline
import com.artflow.app.engine.processing.Compositor
import com.artflow.app.engine.processing.ImageNormalizer
import com.artflow.app.engine.segmentation.MaskProcessor
import com.artflow.app.engine.segmentation.PortraitSegmenter
import com.artflow.app.model.EditorSettings
import com.artflow.app.model.StyleCatalog
import com.artflow.app.model.StylePreset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State manager for the neural art editor screen.
 * Strictly guarantees that rapid style scrubbing immediately cancels prior in-flight jobs.
 */
class EditorViewModel(
    private val styleTransferEngine: StyleTransferEngine,
    private val portraitSegmenter: PortraitSegmenter,
    private val exportPipeline: HighResExportPipeline,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    companion object {
        private const val TAG = "EditorViewModel"
    }

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Idle)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(EditorSettings())
    val settings: StateFlow<EditorSettings> = _settings.asStateFlow()

    // Active in-flight coroutine jobs for cancellation
    private var activeInferenceJob: Job? = null
    private var activeExportJob: Job? = null

    // Session cache
    private var originalPhoto: Bitmap? = null
    private var normalizedCanvas: Bitmap? = null
    private var segmentationMask: FloatArray? = null
    private var currentStylizedBitmap: Bitmap? = null
    private var currentStyle: StylePreset = StyleCatalog.defaultStyle

    /**
     * Ingests a new photo from the camera or gallery:
     * 1. Stores the full-res original for the 1536px export pipeline.
     * 2. Scales and snaps to 768px even dimensions for the interactive WYSIWYG canvas.
     * 3. Runs background portrait segmentation.
     * 4. Renders the default artistic style.
     */
    fun loadImage(bitmap: Bitmap) {
        originalPhoto = bitmap
        val normalized = ImageNormalizer.normalizeCanvas(bitmap)
        normalizedCanvas = normalized
        segmentationMask = null
        currentStylizedBitmap = null

        // Trigger asynchronous portrait segmentation in parallel
        viewModelScope.launch(dispatchers.default) {
            val segResult = portraitSegmenter.segmentPortrait(normalized)
            if (segResult is Result.Success) {
                val refinedMask = MaskProcessor.processMask(
                    rawMask = segResult.data,
                    width = normalized.width,
                    height = normalized.height
                )
                segmentationMask = refinedMask
                Log.d(TAG, "Portrait segmentation and feathering completed.")

                // If style is already rendered, re-composite with the new mask
                if (currentStylizedBitmap != null && _uiState.value is EditorUiState.Success) {
                    recomposite()
                }
            }
        }

        // Trigger initial style transfer
        selectStyle(currentStyle)
    }

    /**
     * User selected a new style preset from the carousel.
     * Cancels any active in-flight inference job before starting the new style inference.
     */
    fun selectStyle(preset: StylePreset) {
        val canvas = normalizedCanvas ?: return
        currentStyle = preset

        // CANCEL any active in-flight inference job immediately
        activeInferenceJob?.cancel(CancellationException("User switched to style: ${preset.name}"))

        val previousStylized = currentStylizedBitmap
        _uiState.value = EditorUiState.Processing(
            canvasBitmap = canvas,
            style = preset,
            previousStylizedBitmap = previousStylized,
            statusMessage = "Rendering ${preset.name}…"
        )

        activeInferenceJob = viewModelScope.launch(dispatchers.default) {
            try {
                val inferenceResult = styleTransferEngine.executeInference(canvas, preset)
                if (inferenceResult is Result.Success) {
                    val stylized = inferenceResult.data
                    currentStylizedBitmap = stylized

                    // Composite with live settings & segmentation mask
                    val composite = Compositor.composite(
                        original = canvas,
                        stylized = stylized,
                        mask = segmentationMask,
                        intensity = _settings.value.intensity,
                        subjectBlend = _settings.value.subjectBlend
                    )

                    _uiState.value = EditorUiState.Success(
                        originalPhoto = originalPhoto ?: canvas,
                        canvasOriginal = canvas,
                        stylizedCanvas = stylized,
                        compositePreview = composite,
                        segmentationMask = segmentationMask,
                        selectedStyle = preset,
                        settings = _settings.value
                    )
                } else if (inferenceResult is Result.Error) {
                    _uiState.value = EditorUiState.Error(inferenceResult.message)
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Inference job successfully cancelled for style: ${preset.name}")
                // Job cancelled intentionally, do not emit error
            } catch (e: Throwable) {
                Log.e(TAG, "Inference failed: ${e.message}", e)
                _uiState.value = EditorUiState.Error(e.localizedMessage ?: "Style transfer failed.")
            }
        }
    }

    /**
     * Updates blend and intensity sliders with instant WYSIWYG recomposition (no model re-run needed).
     */
    fun updateSettings(newSettings: EditorSettings) {
        _settings.value = newSettings
        recomposite()
    }

    private fun recomposite() {
        val canvas = normalizedCanvas ?: return
        val stylized = currentStylizedBitmap ?: return

        val composite = Compositor.composite(
            original = canvas,
            stylized = stylized,
            mask = segmentationMask,
            intensity = _settings.value.intensity,
            subjectBlend = _settings.value.subjectBlend
        )

        _uiState.value = EditorUiState.Success(
            originalPhoto = originalPhoto ?: canvas,
            canvasOriginal = canvas,
            stylizedCanvas = stylized,
            compositePreview = composite,
            segmentationMask = segmentationMask,
            selectedStyle = currentStyle,
            settings = _settings.value
        )
    }

    /**
     * Executes the studio-grade 3-stage high-resolution 1536px export pipeline.
     */
    fun exportArtwork() {
        val original = originalPhoto ?: return
        val currentSuccess = _uiState.value as? EditorUiState.Success ?: return
        val canvasStylized = currentSuccess.compositePreview

        activeExportJob?.cancel()
        _uiState.value = EditorUiState.Exporting(
            stage = ExportStage.SuperResolution,
            progressFraction = 0.1f
        )

        activeExportJob = viewModelScope.launch(dispatchers.default) {
            val result = exportPipeline.executeExport(
                originalHighResPhoto = original,
                canvasStylizedImage = canvasStylized,
                style = currentStyle,
                onProgress = { stage ->
                    _uiState.value = EditorUiState.Exporting(
                        stage = stage,
                        progressFraction = stage.progressFraction
                    )
                }
            )

            withContext(dispatchers.main) {
                if (result is Result.Success) {
                    _uiState.value = currentSuccess.copy(exportedUri = result.data)
                } else if (result is Result.Error) {
                    _uiState.value = EditorUiState.Error("Export failed: ${result.message}")
                }
            }
        }
    }

    fun dismissExport() {
        val currentState = _uiState.value
        if (currentState is EditorUiState.Success) {
            _uiState.value = currentState.copy(exportedUri = null)
        }
    }

    fun reset() {
        activeInferenceJob?.cancel()
        activeExportJob?.cancel()
        originalPhoto = null
        normalizedCanvas = null
        segmentationMask = null
        currentStylizedBitmap = null
        _settings.value = EditorSettings()
        _uiState.value = EditorUiState.Idle
    }
}
