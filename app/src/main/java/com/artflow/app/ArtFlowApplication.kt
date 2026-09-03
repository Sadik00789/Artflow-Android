package com.artflow.app

import android.app.Application
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.StandardDispatcherProvider
import com.artflow.app.core.storage.AssetModelReader
import com.artflow.app.core.storage.MediaStoreWriter
import com.artflow.app.engine.DynamicTensorHandler
import com.artflow.app.engine.GpuDelegateProvider
import com.artflow.app.engine.ModelLruCache
import com.artflow.app.engine.StyleTransferEngine
import com.artflow.app.engine.export.FsrcnnUpscaler
import com.artflow.app.engine.export.HighResExportPipeline
import com.artflow.app.engine.segmentation.PortraitSegmenter

/**
 * Main application class initializing ArtFlow runtime dependencies and engine singletons.
 */
class ArtFlowApplication : Application() {

    lateinit var dispatchers: DispatcherProvider
        private set

    lateinit var assetModelReader: AssetModelReader
        private set

    lateinit var gpuDelegateProvider: GpuDelegateProvider
        private set

    lateinit var modelLruCache: ModelLruCache
        private set

    lateinit var tensorHandler: DynamicTensorHandler
        private set

    lateinit var styleTransferEngine: StyleTransferEngine
        private set

    lateinit var portraitSegmenter: PortraitSegmenter
        private set

    lateinit var fsrcnnUpscaler: FsrcnnUpscaler
        private set

    lateinit var mediaStoreWriter: MediaStoreWriter
        private set

    lateinit var highResExportPipeline: HighResExportPipeline
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        dispatchers = StandardDispatcherProvider()
        assetModelReader = AssetModelReader(this)
        gpuDelegateProvider = GpuDelegateProvider(this)
        modelLruCache = ModelLruCache(capacity = 2, dispatchers = dispatchers)
        tensorHandler = DynamicTensorHandler()

        styleTransferEngine = StyleTransferEngine(
            modelReader = assetModelReader,
            gpuDelegateProvider = gpuDelegateProvider,
            lruCache = modelLruCache,
            tensorHandler = tensorHandler,
            dispatchers = dispatchers
        )

        portraitSegmenter = PortraitSegmenter(
            modelReader = assetModelReader,
            tensorHandler = tensorHandler,
            dispatchers = dispatchers
        )

        fsrcnnUpscaler = FsrcnnUpscaler(
            modelReader = assetModelReader,
            tensorHandler = tensorHandler,
            dispatchers = dispatchers
        )

        mediaStoreWriter = MediaStoreWriter(this)

        highResExportPipeline = HighResExportPipeline(
            fsrcnnUpscaler = fsrcnnUpscaler,
            mediaStoreWriter = mediaStoreWriter,
            dispatchers = dispatchers
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            modelLruCache.close()
        }
    }

    companion object {
        lateinit var instance: ArtFlowApplication
            private set
    }
}
