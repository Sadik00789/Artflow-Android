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

    lateinit var mediaStoreWriter: MediaStoreWriter
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Clean up any old legacy single-token cache files
        cacheDir.listFiles()?.filter { it.name.startsWith("artflow_opencl_cache") }?.forEach { it.delete() }

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
            context = this,
            dispatchers = dispatchers
        )

        mediaStoreWriter = MediaStoreWriter(this)
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
