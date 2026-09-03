package com.artflow.app.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.artflow.app.ArtFlowApplication
import com.artflow.app.ui.editor.EditorScreen
import com.artflow.app.ui.editor.EditorViewModel
import com.artflow.app.ui.theme.ArtFlowTheme
import com.artflow.app.ui.theme.StudioDark

/**
 * Main application activity hosting the ArtFlow Compose Studio.
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: EditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val app = application as ArtFlowApplication
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(
                    styleTransferEngine = app.styleTransferEngine,
                    portraitSegmenter = app.portraitSegmenter,
                    exportPipeline = app.highResExportPipeline,
                    dispatchers = app.dispatchers
                ) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[EditorViewModel::class.java]

        setContent {
            ArtFlowTheme {
                // Photo Picker Launcher
                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    if (uri != null) {
                        loadBitmapFromUri(uri)?.let { bitmap ->
                            viewModel.loadImage(bitmap)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioDark
                ) {
                    EditorScreen(
                        viewModel = viewModel,
                        onPickImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val maxAllowedDimension = 2560

            // 1. Decode bounds only first
            val boundsOptions = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val outWidth = boundsOptions.outWidth
            val outHeight = boundsOptions.outHeight
            if (outWidth <= 0 || outHeight <= 0) return null

            // 2. Calculate inSampleSize to cap maximum dimension at 2560px
            var inSampleSize = 1
            val longestDim = maxOf(outWidth, outHeight)
            while (longestDim / (inSampleSize * 2) >= maxAllowedDimension) {
                inSampleSize *= 2
            }

            // 3. Decode actual bitmap with calculated downsampling
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }

            contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Throwable) {
            null
        }
    }
}
