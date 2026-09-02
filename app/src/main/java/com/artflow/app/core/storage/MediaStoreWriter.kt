package com.artflow.app.core.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.artflow.app.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles exporting rendered artworks to the device's public media gallery with EXIF metadata.
 */
class MediaStoreWriter(private val context: Context) {

    /**
     * Saves a [Bitmap] to the public Pictures/ArtFlow folder with EXIF metadata.
     */
    suspend fun saveArtwork(
        bitmap: Bitmap,
        title: String,
        styleName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "ArtFlow_${styleName.replace(" ", "_")}_$timestamp.jpg"
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ArtFlow")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.Error(IllegalStateException("Failed to create MediaStore entry"))

            resolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            } ?: return@withContext Result.Error(IllegalStateException("Failed to open output stream for $imageUri"))

            // Inject EXIF Metadata
            try {
                resolver.openFileDescriptor(imageUri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    exif.setAttribute(ExifInterface.TAG_SOFTWARE, "ArtFlow On-Device Neural Art Studio")
                    exif.setAttribute(ExifInterface.TAG_ARTIST, "ArtFlow AI Engine ($styleName)")
                    exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "Created with ArtFlow Neural Art Studio - Style: $styleName")
                    exif.setAttribute(ExifInterface.TAG_DATETIME, SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date()))
                    exif.saveAttributes()
                }
            } catch (e: Exception) {
                // Non-fatal if EXIF write fails on specific OEM devices
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Result.Success(imageUri)
        } catch (e: Throwable) {
            Result.Error(e, "Failed to save artwork to MediaStore: ${e.localizedMessage}")
        }
    }
}
