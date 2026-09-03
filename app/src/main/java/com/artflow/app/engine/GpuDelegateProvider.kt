package com.artflow.app.engine

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate

class GpuDelegateProvider(private val context: Context) {

    companion object {
        private const val TAG = "GpuDelegateProvider"
        private const val CPU_FALLBACK_THREADS = 4
    }

    private val compatList = CompatibilityList()
    val isGpuSupported: Boolean = compatList.isDelegateSupportedOnThisDevice

    fun createInterpreterOptions(modelAssetPath: String? = null): InterpreterOptionsHolder {
        val options = Interpreter.Options()
        var gpuDelegate: GpuDelegate? = null

        if (isGpuSupported) {
            try {
                val token = if (modelAssetPath != null) {
                    "artflow_ocl_" + modelAssetPath.substringAfterLast("/").substringBefore(".tflite")
                } else {
                    "artflow_ocl_default"
                }

                val delegateOptions = compatList.bestOptionsForThisDevice.apply {
                    setPrecisionLossAllowed(true)
                    setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER)
                    setSerializationParams(context.cacheDir.absolutePath, token)
                }
                gpuDelegate = GpuDelegate(delegateOptions)
                options.addDelegate(gpuDelegate)
                Log.i(TAG, "OpenCL GPU Delegate configured with token: $token")
            } catch (e: Throwable) {
                Log.w(TAG, "GPU Delegate initialization failed, falling back to CPU: ${e.message}")
                gpuDelegate = null
                configureCpuFallback(options)
            }
        } else {
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
