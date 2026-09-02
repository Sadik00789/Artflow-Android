package com.artflow.app.engine

import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

/**
 * Manages TensorFlow Lite execution delegates:
 * Configures OpenCL GPU delegate with FP16 precision loss allowed and sustained speed preference
 * on supported hardware (such as Snapdragon 695 / Adreno 619).
 * Automatically falls back to a 4-thread CPU XNNPACK configuration.
 */
class GpuDelegateProvider {

    companion object {
        private const val TAG = "GpuDelegateProvider"
        private const val CPU_FALLBACK_THREADS = 4
    }

    private val compatList = CompatibilityList()
    val isGpuSupported: Boolean = compatList.isDelegateSupportedOnThisDevice

    /**
     * Creates an [Interpreter.Options] configured for best performance on the target hardware.
     */
    fun createInterpreterOptions(): InterpreterOptionsHolder {
        val options = Interpreter.Options()
        var gpuDelegate: GpuDelegate? = null

        if (isGpuSupported) {
            try {
                val delegateOptions = compatList.bestOptionsForThisDevice.apply {
                    setPrecisionLossAllowed(true)
                    setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED)
                }
                gpuDelegate = GpuDelegate(delegateOptions)
                options.addDelegate(gpuDelegate)
                Log.i(TAG, "OpenCL GPU Delegate initialized successfully (FP16 enabled, Sustained Speed).")
            } catch (e: Throwable) {
                Log.w(TAG, "GPU Delegate initialization failed, falling back to CPU XNNPACK: ${e.message}")
                gpuDelegate = null
                configureCpuFallback(options)
            }
        } else {
            Log.i(TAG, "GPU Delegate not supported on this device. Using CPU XNNPACK.")
            configureCpuFallback(options)
        }

        return InterpreterOptionsHolder(options, gpuDelegate)
    }

    private fun configureCpuFallback(options: Interpreter.Options) {
        options.setNumThreads(CPU_FALLBACK_THREADS)
        options.setUseXNNPACK(true)
    }

    data class InterpreterOptionsHolder(
        val options: Interpreter.Options,
        val gpuDelegate: GpuDelegate?
    ) : AutoCloseable {
        override fun close() {
            gpuDelegate?.close()
        }
    }
}
