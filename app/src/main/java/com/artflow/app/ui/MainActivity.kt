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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Throwable) {
            null
        }
    }
}
