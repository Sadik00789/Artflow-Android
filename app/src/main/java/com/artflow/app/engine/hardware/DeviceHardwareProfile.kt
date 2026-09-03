package com.artflow.app.engine.hardware

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Detects SoC vendor, GPU architecture family, and computes optimal multi-tier hardware acceleration parameters.
 */
object DeviceHardwareProfile {

    private const val TAG = "DeviceHardwareProfile"

    enum class SocVendor {
        QUALCOMM,
        MEDIATEK,
        SAMSUNG_EXYNOS,
        GOOGLE_TENSOR,
        UNISOC,
        GENERIC
    }

    enum class GpuFamily {
        ADRENO,
        MALI,
        XCLIPSE,
        POWERVR,
        GENERIC
    }

    data class Profile(
        val vendor: SocVendor,
        val gpuFamily: GpuFamily,
        val optimalCpuThreads: Int,
        val preferOpenClFirst: Boolean,
        val supportsDiskShaderCaching: Boolean
    )

    fun getProfile(context: Context): Profile {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.lowercase()
        } else {
            ""
        }
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.lowercase()
        } else {
            ""
        }

        val vendor = detectSocVendor(hardware, board, manufacturer, socManufacturer, socModel)
        val gpuFamily = detectGpuFamily(vendor, hardware, board)
        val optimalThreads = calculateOptimalCpuThreads()

        // Google Tensor devices heavily restrict vendor OpenCL access via SELinux;
        // Qualcomm Adreno and Mali on MediaTek/Exynos prefer OpenCL.
        val preferOpenCl = when (vendor) {
            SocVendor.GOOGLE_TENSOR -> false
            SocVendor.QUALCOMM -> true
            SocVendor.MEDIATEK -> true
            SocVendor.SAMSUNG_EXYNOS -> true
            else -> true
        }

        // OpenCL disk shader serialization is supported on Qualcomm and ARM Mali
        val supportsShaderCaching = (vendor == SocVendor.QUALCOMM || vendor == SocVendor.MEDIATEK)

        val profile = Profile(
            vendor = vendor,
            gpuFamily = gpuFamily,
            optimalCpuThreads = optimalThreads,
            preferOpenClFirst = preferOpenCl,
            supportsDiskShaderCaching = supportsShaderCaching
        )

        Log.i(
            TAG,
            "Detected Hardware Profile: Vendor=${profile.vendor}, GPU=${profile.gpuFamily}, " +
                "Threads=${profile.optimalCpuThreads}, PreferOpenCL=${profile.preferOpenClFirst}"
        )
        return profile
    }

    /**
     * Resolves the safest cache directory for compiled GPU binaries.
     * Prefers codeCacheDir (immune to Android OS storage-purge under memory pressure).
     */
    fun getShaderCacheDirectory(context: Context): File {
        val codeCache = context.codeCacheDir
        return if (codeCache != null && (codeCache.exists() || codeCache.mkdirs())) {
            codeCache
        } else {
            context.cacheDir
        }
    }

    internal fun detectSocVendor(
        hardware: String,
        board: String,
        manufacturer: String,
        socManufacturer: String,
        socModel: String
    ): SocVendor {
        val combined = "$hardware $board $manufacturer $socManufacturer $socModel"

        return when {
            combined.contains("qcom") || combined.contains("qualcomm") ||
                combined.contains("snapdragon") || combined.contains("holi") ||
                combined.contains("sm6") || combined.contains("sm7") ||
                combined.contains("sm8") || combined.contains("kona") ||
                combined.contains("lahaina") || combined.contains("taro") ||
                combined.contains("kalama") || combined.contains("pineapple") -> SocVendor.QUALCOMM

            combined.contains("tensor") || combined.contains("gs101") ||
                combined.contains("gs201") || combined.contains("zuma") -> SocVendor.GOOGLE_TENSOR

            combined.contains("mt") || combined.contains("mediatek") ||
                combined.contains("dimensity") || combined.contains("helio") -> SocVendor.MEDIATEK

            combined.contains("exynos") || combined.contains("s5e") ||
                combined.contains("universal") -> SocVendor.SAMSUNG_EXYNOS

            combined.contains("unisoc") || combined.contains("sprd") ||
                combined.contains("ums") || combined.contains("sc98") -> SocVendor.UNISOC

            else -> SocVendor.GENERIC
        }
    }

    internal fun detectGpuFamily(vendor: SocVendor, hardware: String, board: String): GpuFamily {
        return when (vendor) {
            SocVendor.QUALCOMM -> GpuFamily.ADRENO
            SocVendor.GOOGLE_TENSOR -> GpuFamily.MALI
            SocVendor.MEDIATEK -> GpuFamily.MALI
            SocVendor.SAMSUNG_EXYNOS -> {
                if (hardware.contains("sgpu") || board.contains("sgpu") || hardware.contains("xclipse")) {
                    GpuFamily.XCLIPSE
                } else {
                    GpuFamily.MALI
                }
            }
            SocVendor.UNISOC -> GpuFamily.MALI
            SocVendor.GENERIC -> GpuFamily.GENERIC
        }
    }

    /**
     * Dynamically calculates optimal threads for TFLite CPU/XNNPACK fallback.
     * On big.LITTLE architectures, setting threads equal to total cores saturates little cores
     * and causes thread context switching starvation. Keeping threads to (totalCores - 2) capped at 4
     * keeps performance cores saturated while keeping UI and OS responsive.
     */
    internal fun calculateOptimalCpuThreads(): Int {
        val totalCores = Runtime.getRuntime().availableProcessors()
        return when {
            totalCores <= 2 -> totalCores
            totalCores <= 4 -> totalCores - 1
            else -> (totalCores - 2).coerceIn(2, 4)
        }
    }
}
