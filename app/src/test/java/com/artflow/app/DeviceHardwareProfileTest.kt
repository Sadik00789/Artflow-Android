package com.artflow.app

import com.artflow.app.engine.hardware.DeviceHardwareProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceHardwareProfileTest {

    @Test
    fun testQualcommSnapdragonDetection() {
        val vendor = DeviceHardwareProfile.detectSocVendor(
            hardware = "qcom",
            board = "holi",
            manufacturer = "Xiaomi",
            socManufacturer = "QTI",
            socModel = "SM6375"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.QUALCOMM, vendor)

        val gpu = DeviceHardwareProfile.detectGpuFamily(vendor, hardware = "qcom", board = "holi")
        assertEquals(DeviceHardwareProfile.GpuFamily.ADRENO, gpu)
    }

    @Test
    fun testGoogleTensorDetection() {
        val vendor = DeviceHardwareProfile.detectSocVendor(
            hardware = "tensor",
            board = "cloudripper",
            manufacturer = "Google",
            socManufacturer = "Google",
            socModel = "GS201"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.GOOGLE_TENSOR, vendor)

        val gpu = DeviceHardwareProfile.detectGpuFamily(vendor, hardware = "tensor", board = "cloudripper")
        assertEquals(DeviceHardwareProfile.GpuFamily.MALI, gpu)
    }

    @Test
    fun testMediaTekDimensityDetection() {
        val vendor = DeviceHardwareProfile.detectSocVendor(
            hardware = "mt6877",
            board = "k6877v1_64",
            manufacturer = "MediaTek",
            socManufacturer = "MediaTek",
            socModel = "Dimensity 900"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.MEDIATEK, vendor)

        val gpu = DeviceHardwareProfile.detectGpuFamily(vendor, hardware = "mt6877", board = "k6877v1_64")
        assertEquals(DeviceHardwareProfile.GpuFamily.MALI, gpu)
    }

    @Test
    fun testSamsungExynosXclipseDetection() {
        val vendor = DeviceHardwareProfile.detectSocVendor(
            hardware = "s5e9925",
            board = "universal2200",
            manufacturer = "Samsung",
            socManufacturer = "Samsung",
            socModel = "Exynos 2200"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.SAMSUNG_EXYNOS, vendor)

        val gpu = DeviceHardwareProfile.detectGpuFamily(vendor, hardware = "s5e9925_xclipse", board = "universal2200")
        assertEquals(DeviceHardwareProfile.GpuFamily.XCLIPSE, gpu)
    }

    @Test
    fun testGoogleTensorGenerations() {
        // Pixel 6 / 6 Pro / 6a (Tensor G1)
        val tensorG1 = DeviceHardwareProfile.detectSocVendor(
            hardware = "oriole",
            board = "oriole",
            manufacturer = "Google",
            socManufacturer = "Google",
            socModel = "GS101"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.GOOGLE_TENSOR, tensorG1)
        assertEquals(DeviceHardwareProfile.GpuFamily.MALI, DeviceHardwareProfile.detectGpuFamily(tensorG1, "oriole", "oriole"))

        // Pixel 7 / 7 Pro / 7a (Tensor G2)
        val tensorG2 = DeviceHardwareProfile.detectSocVendor(
            hardware = "panther",
            board = "cheetah",
            manufacturer = "Google",
            socManufacturer = "Google",
            socModel = "GS201"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.GOOGLE_TENSOR, tensorG2)

        // Pixel 8 / 8 Pro / 8a (Tensor G3)
        val tensorG3 = DeviceHardwareProfile.detectSocVendor(
            hardware = "zuma",
            board = "shiba",
            manufacturer = "Google",
            socManufacturer = "Google",
            socModel = "GS301"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.GOOGLE_TENSOR, tensorG3)

        // Pixel 9 / 9 Pro / 9 Pro XL (Tensor G4)
        val tensorG4 = DeviceHardwareProfile.detectSocVendor(
            hardware = "zumapro",
            board = "caiman",
            manufacturer = "Google",
            socManufacturer = "Google",
            socModel = "GS401"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.GOOGLE_TENSOR, tensorG4)

        // Pixel 10 (Tensor G5 - PowerVR)
        val tensorG5 = DeviceHardwareProfile.detectSocVendor(
            hardware = "laguna",
            board = "frankel",
            manufacturer = "Google",
            socManufacturer = "Google",
            socModel = "GS501"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.GOOGLE_TENSOR, tensorG5)
        assertEquals(DeviceHardwareProfile.GpuFamily.POWERVR, DeviceHardwareProfile.detectGpuFamily(tensorG5, "laguna", "frankel"))
    }

    @Test
    fun testSnapdragonEliteAndFlagships() {
        // Snapdragon 8 Elite (SM8750 / Sun)
        val snapdragonElite = DeviceHardwareProfile.detectSocVendor(
            hardware = "qcom",
            board = "sun",
            manufacturer = "Samsung",
            socManufacturer = "QTI",
            socModel = "SM8750"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.QUALCOMM, snapdragonElite)

        // Snapdragon 8 Gen 3 (SM8650 / Pineapple)
        val snapdragon8Gen3 = DeviceHardwareProfile.detectSocVendor(
            hardware = "qcom",
            board = "pineapple",
            manufacturer = "OnePlus",
            socManufacturer = "QTI",
            socModel = "SM8650"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.QUALCOMM, snapdragon8Gen3)
    }

    @Test
    fun testExynosXclipseAMD() {
        // Exynos 2400 (Xclipse 940)
        val exynos2400 = DeviceHardwareProfile.detectSocVendor(
            hardware = "s5e9945",
            board = "universal2400",
            manufacturer = "Samsung",
            socManufacturer = "Samsung",
            socModel = "Exynos 2400"
        )
        assertEquals(DeviceHardwareProfile.SocVendor.SAMSUNG_EXYNOS, exynos2400)
        assertEquals(DeviceHardwareProfile.GpuFamily.XCLIPSE, DeviceHardwareProfile.detectGpuFamily(exynos2400, "s5e9945_xclipse", "universal2400"))
    }

    @Test
    fun testOptimalThreadCalculation() {
        val threads = DeviceHardwareProfile.calculateOptimalCpuThreads()
        assertTrue("Optimal thread count should be at least 2", threads >= 2)
        assertTrue("Optimal thread count should not exceed 4 to preserve UI responsiveness", threads <= 4)
    }
}
