package com.artflow.app.engine

import android.content.Context
import android.util.Log
import com.artflow.app.engine.hardware.DeviceHardwareProfile
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory

class GpuDelegateProvider(private val context: Context) {

    companion object {
        private const val TAG = "GpuDelegateProvider"
    }

    val hardwareProfile = DeviceHardwareProfile.getProfile(context)

    fun createInterpreterOptions(modelAssetPath: String? = null): InterpreterOptionsHolder {
        val options = Interpreter.Options()
        var gpuDelegate: GpuDelegate? = null
        val vendorTag = hardwareProfile.vendor.name.lowercase()
        val gpuTag = hardwareProfile.gpuFamily.name.lowercase()
        val token = if (modelAssetPath != null) {
            "artflow_${vendorTag}_${gpuTag}_" + modelAssetPath.substringAfterLast("/").substringBefore(".tflite")
        } else {
            "artflow_${vendorTag}_${gpuTag}_default"
        }
        val cacheDir = DeviceHardwareProfile.getShaderCacheDirectory(context).absolutePath

        val primaryBackend = if (hardwareProfile.preferOpenClFirst) {
            GpuDelegateFactory.Options.GpuBackend.OPENCL
        } else {
            GpuDelegateFactory.Options.GpuBackend.OPENGL
        }
        val secondaryBackend = if (primaryBackend == GpuDelegateFactory.Options.GpuBackend.OPENCL) {
            GpuDelegateFactory.Options.GpuBackend.OPENGL
        } else {
            GpuDelegateFactory.Options.GpuBackend.OPENCL
        }

        // Tier 1: Primary Vendor-Tuned GPU Backend (Direct Driver Probe, Bypassing Static Whitelist)
        try {
            val delegateOptions = GpuDelegateFactory.Options().apply {
                setPrecisionLossAllowed(true)
                setInferencePreference(GpuDelegateFactory.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER)
                setForceBackend(primaryBackend)
                if (primaryBackend == GpuDelegateFactory.Options.GpuBackend.OPENCL && hardwareProfile.supportsDiskShaderCaching) {
                    setSerializationParams(cacheDir, token)
                }
            }
            gpuDelegate = GpuDelegate(delegateOptions)
            options.addDelegate(gpuDelegate)
            Log.i(TAG, "Tier 1: Successfully bound ${primaryBackend.name} GPU Delegate for $token")
            return InterpreterOptionsHolder(options, gpuDelegate, executionBackend = "GPU_${primaryBackend.name}")
        } catch (e: Throwable) {
            Log.w(TAG, "Tier 1 (${primaryBackend.name}) failed: ${e.message}. Attempting Tier 2 (${secondaryBackend.name})...")
            gpuDelegate?.close()
            gpuDelegate = null
        }

        // Tier 2: Secondary GPU Backend (OpenGL ES 3.1 Compute Shaders)
        try {
            val fallbackOptions = GpuDelegateFactory.Options().apply {
                setPrecisionLossAllowed(true)
                setInferencePreference(GpuDelegateFactory.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER)
                setForceBackend(secondaryBackend)
            }
            gpuDelegate = GpuDelegate(fallbackOptions)
            options.addDelegate(gpuDelegate)
            Log.i(TAG, "Tier 2: Successfully bound ${secondaryBackend.name} GPU Delegate")
            return InterpreterOptionsHolder(options, gpuDelegate, executionBackend = "GPU_${secondaryBackend.name}")
        } catch (e: Throwable) {
            Log.w(TAG, "Tier 2 (${secondaryBackend.name}) failed: ${e.message}. Bypassing NNAPI trap -> Falling to Tier 3 (XNNPACK CPU)...")
            gpuDelegate?.close()
            gpuDelegate = null
        }

        // Tier 3: Multi-Threaded SIMD XNNPACK CPU (Bypasses Single-Threaded NNAPI Reference Trap)
        configureCpuFallback(options)
        return InterpreterOptionsHolder(options, gpuDelegate = null, executionBackend = "CPU_XNNPACK")
    }

    private fun configureCpuFallback(options: Interpreter.Options) {
        options.setNumThreads(hardwareProfile.optimalCpuThreads)
        options.setUseXNNPACK(true)
        Log.i(TAG, "Tier 3: Configured XNNPACK CPU fallback with ${hardwareProfile.optimalCpuThreads} threads.")
    }

    data class InterpreterOptionsHolder(
        val options: Interpreter.Options,
        val gpuDelegate: GpuDelegate?,
        val executionBackend: String = "UNKNOWN"
    ) : AutoCloseable {
        override fun close() {
            gpuDelegate?.close()
        }
    }
}
