package com.artflow.app.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Interface providing coroutine dispatchers across the application.
 * Serializes ML inference on a dedicated single-threaded dispatcher to prevent OpenCL race conditions.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val ml: CoroutineDispatcher
}

/**
 * Default production implementation of [DispatcherProvider].
 */
class StandardDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default

    // Dedicated single-threaded executor for ML inference to guarantee serialized GPU/OpenCL driver access
    private val mlExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ArtFlow-ML-Worker").apply {
            priority = Thread.NORM_PRIORITY + 1
        }
    }

    override val ml: CoroutineDispatcher = mlExecutor.asCoroutineDispatcher()
}
