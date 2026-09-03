package com.artflow.app

import android.graphics.Bitmap
import app.cash.turbine.test
import com.artflow.app.core.common.DispatcherProvider
import com.artflow.app.core.common.Result
import com.artflow.app.core.storage.MediaStoreWriter
import com.artflow.app.engine.StyleTransferEngine
import com.artflow.app.engine.segmentation.PortraitSegmenter
import com.artflow.app.model.StyleCatalog
import com.artflow.app.ui.editor.EditorUiState
import com.artflow.app.ui.editor.EditorViewModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val ml: CoroutineDispatcher = testDispatcher
    }

    private val styleTransferEngine: StyleTransferEngine = mockk(relaxed = true)
    private val portraitSegmenter: PortraitSegmenter = mockk(relaxed = true)
    private val mediaStoreWriter: MediaStoreWriter = mockk(relaxed = true)

    private lateinit var viewModel: EditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Bitmap::class)

        viewModel = EditorViewModel(
            styleTransferEngine = styleTransferEngine,
            portraitSegmenter = portraitSegmenter,
            mediaStoreWriter = mediaStoreWriter,
            dispatchers = testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testInitialStateIsIdle() = runTest(testDispatcher) {
        assertEquals(EditorUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun testRapidStyleSwitchingCancelsInFlightInference() = runTest(testDispatcher) {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        coEvery { mockBitmap.width } returns 768
        coEvery { mockBitmap.height } returns 576
        coEvery { Bitmap.createScaledBitmap(any(), any(), any(), any()) } returns mockBitmap
        coEvery { Bitmap.createBitmap(any<Int>(), any<Int>(), any()) } returns mockBitmap

        val style1 = StyleCatalog.fineArtStyles[0] // Starry Night
        val style2 = StyleCatalog.fineArtStyles[1] // The Scream

        val outBitmap1 = mockk<Bitmap>(relaxed = true)
        val outBitmap2 = mockk<Bitmap>(relaxed = true)

        // Style 1 takes 500ms (slow)
        coEvery { styleTransferEngine.executeInference(any(), style1) } coAnswers {
            delay(500)
            Result.Success(outBitmap1)
        }

        // Style 2 is quick
        coEvery { styleTransferEngine.executeInference(any(), style2) } coAnswers {
            Result.Success(outBitmap2)
        }

        viewModel.loadImage(mockBitmap)
        advanceTimeBy(50) // Started style 1

        // User rapidly switches to style 2 before style 1 finishes
        viewModel.selectStyle(style2)

        advanceUntilIdle()

        // State must reflect style2, not style1
        val finalState = viewModel.uiState.value
        assertTrue("State must be Success", finalState is EditorUiState.Success)
        val success = finalState as EditorUiState.Success
        assertEquals(style2.id, success.selectedStyle.id)
    }
}
