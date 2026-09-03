package com.artflow.app.engine

import android.content.Context
import android.util.Log
import com.artflow.app.engine.hardware.DeviceHardwareProfile
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory

class GpuDelegateProvider(private val context: Context) {

    companion object {
        private const val TAG = "GpuDelegateProvider"
    }

    private val compatList = CompatibilityList()
    val isGpuSupported: Boolean = compatList.isDelegateSupportedOnThisDevice
    val hardwareProfile = DeviceHardwareProfile.getProfile(context)

    fun createInterpreterOptions(modelAssetPath: String? = null): InterpreterOptionsHolder {
        val options = Interpreter.Options()
        var gpuDelegate: GpuDelegate? = null
        val token = if (modelAssetPath != null) {
            "artflow_ocl_" + modelAssetPath.substringAfterLast("/").substringBefore(".tflite")
        } else {
            "artflow_ocl_default"
        }
        val cacheDir = DeviceHardwareProfile.getShaderCacheDirectory(context).absolutePath

        if (isGpuSupported) {
            // Determine backend trial order based on detected SoC vendor
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

            // Tier 1: Primary Vendor-Tuned GPU Backend
            try {
                val delegateOptions = compatList.bestOptionsForThisDevice.apply {
                    setPrecisionLossAllowed(true)
                    setInferencePreference(GpuDelegateFactory.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER)
                    setForceBackend(primaryBackend)
                    if (primaryBackend == GpuDelegateFactory.Options.GpuBackend.OPENCL && hardwareProfile.supportsDiskShaderCaching) {
                        setSerializationParams(cacheDir, token)
                    }
                }
                gpuDelegate = GpuDelegate(delegateOptions)
                options.addDelegate(gpuDelegate)
                Log.i(TAG, "Tier 1: Configured ${primaryBackend.name} GPU Delegate with token: $token")
                return InterpreterOptionsHolder(options, gpuDelegate, executionBackend = "GPU_${primaryBackend.name}")
            } catch (e: Throwable) {
                Log.w(TAG, "Tier 1 (${primaryBackend.name}) failed: ${e.message}. Attempting Tier 2 (${secondaryBackend.name})...")
                gpuDelegate?.close()
                gpuDelegate = null
            }

            // Tier 2: Secondary GPU Backend (e.g. OpenGL compute shaders if OpenCL was blocked by SELinux)
            try {
                val fallbackOptions = compatList.bestOptionsForThisDevice.apply {
                    setPrecisionLossAllowed(true)
                    setInferencePreference(GpuDelegateFactory.Options.INFERENCE_PREFERENCE_FAST_SINGLE_ANSWER)
                    setForceBackend(secondaryBackend)
                }
                gpuDelegate = GpuDelegate(fallbackOptions)
                options.addDelegate(gpuDelegate)
                Log.i(TAG, "Tier 2: Configured ${secondaryBackend.name} GPU Delegate successfully.")
                return InterpreterOptionsHolder(options, gpuDelegate, executionBackend = "GPU_${secondaryBackend.name}")
            } catch (e: Throwable) {
                Log.w(TAG, "Tier 2 (${secondaryBackend.name}) failed: ${e.message}. Attempting Tier 3 (NNAPI)...")
                gpuDelegate?.close()
                gpuDelegate = null
            }
        }

        // Tier 3: Android NNAPI (Hardware NPU Acceleration)
        try {
            options.setUseNNAPI(true)
            Log.i(TAG, "Tier 3: Configured Android NNAPI acceleration.")
            return InterpreterOptionsHolder(options, gpuDelegate = null, executionBackend = "NNAPI")
        } catch (e: Throwable) {
            Log.w(TAG, "Tier 3 NNAPI failed: ${e.message}. Falling back to Tier 4 (XNNPACK CPU)...")
            options.setUseNNAPI(false)
        }

        // Tier 4: Multi-threaded XNNPACK CPU
        configureCpuFallback(options)
        return InterpreterOptionsHolder(options, gpuDelegate = null, executionBackend = "CPU_XNNPACK")
    }

    private fun configureCpuFallback(options: Interpreter.Options) {
        options.setNumThreads(hardwareProfile.optimalCpuThreads)
        options.setUseXNNPACK(true)
        Log.i(TAG, "Tier 4: Configured XNNPACK CPU fallback with ${hardwareProfile.optimalCpuThreads} threads.")
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
