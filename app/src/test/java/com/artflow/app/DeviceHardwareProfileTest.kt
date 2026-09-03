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
    fun testOptimalThreadCalculation() {
        val threads = DeviceHardwareProfile.calculateOptimalCpuThreads()
        assertTrue("Optimal thread count should be at least 2", threads >= 2)
        assertTrue("Optimal thread count should not exceed 4 to preserve UI responsiveness", threads <= 4)
    }
}
