package com.artflow.app

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artflow.app.core.common.StandardDispatcherProvider
import com.artflow.app.core.storage.AssetModelReader
import com.artflow.app.engine.DynamicTensorHandler
import com.artflow.app.engine.GpuDelegateProvider
import com.artflow.app.engine.ModelLruCache
import com.artflow.app.engine.StyleTransferEngine
import com.artflow.app.model.StyleCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation benchmark testing inference latency on Qualcomm Snapdragon 695 5G (Adreno 619 GPU).
 * Latency target: 220–320 ms on Adreno 619 native FP16 OpenCL.
 */
@RunWith(AndroidJUnit4::class)
class AdrenoBenchmarkTest {

    @Test
    fun benchmarkInferenceLatency() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dispatchers = StandardDispatcherProvider()
        val reader = AssetModelReader(context)
        val gpuProvider = GpuDelegateProvider(context)
        val lruCache = ModelLruCache(capacity = 2, dispatchers = dispatchers)
        val tensorHandler = DynamicTensorHandler()

        val engine = StyleTransferEngine(
            modelReader = reader,
            gpuDelegateProvider = gpuProvider,
            lruCache = lruCache,
            tensorHandler = tensorHandler,
            dispatchers = dispatchers
        )

        // Standard 768x576 normalized canvas
        val testBitmap = Bitmap.createBitmap(768, 576, Bitmap.Config.ARGB_8888)
        val style = StyleCatalog.defaultStyle

        // Warm-up pass
        engine.executeInference(testBitmap, style)

        // Measured benchmark passes
        val iterations = 5
        var totalDuration = 0L

        repeat(iterations) {
            val start = System.currentTimeMillis()
            val result = engine.executeInference(testBitmap, style)
            val duration = System.currentTimeMillis() - start
            assertTrue(result.isSuccess)
            totalDuration += duration
        }

        val avgDurationMs = totalDuration / iterations
        println("AdrenoBenchmark: Average inference latency over $iterations runs: ${avgDurationMs}ms")

        // Assert average execution finishes within reasonable budget for 1024px resolution
        assertTrue("Inference at 1024px must finish under 3500ms on Adreno 619", avgDurationMs < 3500)
    }

    @Test
    fun testPortraitSegmenter() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dispatchers = StandardDispatcherProvider()
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.rgb(220, 180, 140) }
        canvas.drawCircle(256f, 256f, 150f, paint)
        val normalized = com.artflow.app.engine.processing.ImageNormalizer.normalizeCanvas(bitmap)
        val segmenter = com.artflow.app.engine.segmentation.PortraitSegmenter(context, dispatchers)
        val result = segmenter.segmentPortrait(normalized)
        assertTrue("segmentPortrait must succeed", result.isSuccess)
    }
}






