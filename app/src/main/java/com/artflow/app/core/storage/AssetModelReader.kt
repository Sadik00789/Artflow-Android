package com.artflow.app.core.storage

import android.content.Context
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Utility to memory-map uncompressed models directly from the APK assets directory (zero-copy).
 */
class AssetModelReader(private val context: Context) {

    /**
     * Loads a TFLite model from assets as a zero-copy [MappedByteBuffer].
     *
     * @param assetPath Path within the assets directory (e.g. "models/fine_art/starry_night.tflite")
     * @return MappedByteBuffer pointing directly to the APK memory offset.
     */
    @Throws(IOException::class)
    fun loadModelFile(assetPath: String): MappedByteBuffer {
        val assetFd = context.assets.openFd(assetPath)
        assetFd.use { fd ->
            FileInputStream(fd.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                val startOffset = fd.startOffset
                val declaredLength = fd.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }
}
