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
        val optimalThreads = calculateOptimalCpuThreads(vendor)

        // All premier mobile GPU architectures (Qualcomm Adreno, ARM Mali on Google Tensor / MediaTek /
        // Samsung Exynos, and AMD Xclipse) deliver optimal FP16 matrix throughput via OpenCL.
        // If an OS policy blocks OpenCL, Tier 2 automatically falls back to OpenGL ES compute.
        val preferOpenCl = true

        // OpenCL disk shader binary serialization is supported across Adreno, Mali, and Xclipse
        val supportsShaderCaching = when (vendor) {
            SocVendor.QUALCOMM -> true
            SocVendor.MEDIATEK -> true
            SocVendor.GOOGLE_TENSOR -> true
            SocVendor.SAMSUNG_EXYNOS -> true
            else -> false
        }

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
                "Threads=${profile.optimalCpuThreads}, PreferOpenCL=${profile.preferOpenClFirst}, " +
                "DiskShaderCache=${profile.supportsDiskShaderCaching}"
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
        val hw = hardware.lowercase()
        val bd = board.lowercase()
        val mfg = manufacturer.lowercase()
        val socMfg = socManufacturer.lowercase()
        val socMdl = socModel.lowercase()
        val combined = "$hw $bd $mfg $socMfg $socMdl"

        return when {
            // 1. Google Tensor (Pixel 6 through Pixel 9 Pro Fold and beyond)
            combined.contains("tensor") || combined.contains("gs101") ||
                combined.contains("gs201") || combined.contains("gs301") ||
                combined.contains("gs401") || combined.contains("gs501") ||
                combined.contains("zuma") || combined.contains("laguna") ||
                combined.contains("whitefin") || combined.contains("cloudripper") ||
                (mfg == "google" && (
                    bd.contains("oriole") || bd.contains("raven") || bd.contains("bluejay") ||
                    bd.contains("panther") || bd.contains("cheetah") || bd.contains("lynx") ||
                    bd.contains("felix") || bd.contains("tangorpro") ||
                    bd.contains("shiba") || bd.contains("husky") || bd.contains("akita") ||
                    bd.contains("tokay") || bd.contains("caiman") || bd.contains("komodo") ||
                    bd.contains("comet") || bd.contains("frankel") ||
                    hw.contains("oriole") || hw.contains("raven") || hw.contains("bluejay") ||
                    hw.contains("panther") || hw.contains("cheetah") || hw.contains("lynx") ||
                    hw.contains("shiba") || hw.contains("husky") || hw.contains("akita") ||
                    hw.contains("tokay") || hw.contains("caiman") || hw.contains("komodo") ||
                    hw.contains("comet")
                )) -> SocVendor.GOOGLE_TENSOR

            // 2. Qualcomm Snapdragon (8 Elite, 8 Gen 1/2/3, 7/6/4 series)
            combined.contains("qcom") || combined.contains("qualcomm") ||
                combined.contains("snapdragon") || combined.contains("holi") ||
                combined.contains("sm6") || combined.contains("sm7") ||
                combined.contains("sm8") || combined.contains("sm8750") ||
                combined.contains("kona") || combined.contains("lahaina") ||
                combined.contains("taro") || combined.contains("kalama") ||
                combined.contains("pineapple") || bd.contains("sun") ||
                hw.contains("sun") || combined.contains("cliffs") ||
                combined.contains("lanai") -> SocVendor.QUALCOMM

            // 3. MediaTek Dimensity & Helio
            combined.contains("mt") || combined.contains("mediatek") ||
                combined.contains("dimensity") || combined.contains("helio") ||
                combined.contains("k68") || combined.contains("k69") -> SocVendor.MEDIATEK

            // 4. Samsung Exynos & Xclipse
            combined.contains("exynos") || combined.contains("s5e") ||
                combined.contains("universal") || combined.contains("erd") -> SocVendor.SAMSUNG_EXYNOS

            // 5. Unisoc
            combined.contains("unisoc") || combined.contains("sprd") ||
                combined.contains("ums") || combined.contains("sc98") ||
                combined.contains("t606") || combined.contains("t612") ||
                combined.contains("t616") || combined.contains("t760") ||
                combined.contains("t820") -> SocVendor.UNISOC

            else -> SocVendor.GENERIC
        }
    }

    internal fun detectGpuFamily(vendor: SocVendor, hardware: String, board: String): GpuFamily {
        val hw = hardware.lowercase()
        val bd = board.lowercase()
        return when (vendor) {
            SocVendor.QUALCOMM -> GpuFamily.ADRENO
            SocVendor.GOOGLE_TENSOR -> {
                if (hw.contains("laguna") || bd.contains("laguna") || hw.contains("frankel")) {
                    GpuFamily.POWERVR
                } else {
                    GpuFamily.MALI
                }
            }
            SocVendor.MEDIATEK -> GpuFamily.MALI
            SocVendor.SAMSUNG_EXYNOS -> {
                if (hw.contains("sgpu") || bd.contains("sgpu") || hw.contains("xclipse")) {
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
     * On tri-cluster architectures (Google Tensor, Snapdragon 8-series, Dimensity 9-series),
     * setting 4 threads perfectly saturates the Cortex-X super cores and Cortex-A7xx big cores
     * without thrashing onto little efficiency cores.
     */
    internal fun calculateOptimalCpuThreads(vendor: SocVendor = SocVendor.GENERIC): Int {
        val totalCores = Runtime.getRuntime().availableProcessors()
        return when {
            totalCores <= 2 -> totalCores
            totalCores <= 4 -> totalCores - 1
            else -> 4
        }
    }
}
