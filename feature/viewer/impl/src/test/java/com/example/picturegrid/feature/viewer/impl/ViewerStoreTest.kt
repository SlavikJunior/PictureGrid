package com.example.picturegrid.feature.viewer.impl

import com.example.picturegrid.core.images.ImageContentChecker
import com.example.picturegrid.core.images.ImageStorageImpl
import com.example.picturegrid.core.images.OriginalImageLoader
import com.example.picturegrid.core.network.BinaryResponse
import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkError
import com.example.picturegrid.core.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ViewerStoreTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val url = "https://example.com/img.png"
    private val imageChecker = ImageContentChecker { bytes ->
        runCatching {
            javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes)) != null
        }.getOrDefault(false)
    }
    private val validPng = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
    )

    private fun mutableClient(initial: NetworkResult<BinaryResponse>) =
        object : NetworkClient {
            @Volatile
            var result: NetworkResult<BinaryResponse> = initial
            override suspend fun fetchText(url: String) = NetworkResult.Failure(NetworkError.Io)
            override suspend fun fetchBytes(url: String) = result
        }

    private fun store(client: NetworkClient): ViewerStore {
        val storage = ImageStorageImpl(File(tempFolder.root, "orig"), testDispatcher)
        val loader = OriginalImageLoader(client, storage, testDispatcher, imageChecker)
        return ViewerStoreFactory(
            storeFactory = com.arkivanov.mvikotlin.main.store.DefaultStoreFactory(),
            loader = loader,
            url = url,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
        ).create()
    }

    @Test
    fun `initial state is loading`() = runTest(testDispatcher) {
        val client = mutableClient(NetworkResult.Success(BinaryResponse(validPng, "image/png")))
        val s = store(client)
        assertTrue(s.state.isLoading)
        assertNull(s.state.file)
        s.dispose()
    }

    @Test
    fun `successful load sets file`() = runTest(testDispatcher) {
        val client = mutableClient(NetworkResult.Success(BinaryResponse(validPng, "image/png")))
        val s = store(client)
        s.init()
        advanceUntilIdle()
        assertFalse(s.state.isLoading)
        assertNotNull(s.state.file)
        assertNull(s.state.error)
        s.dispose()
    }

    @Test
    fun `failed load sets error`() = runTest(testDispatcher) {
        val client = mutableClient(NetworkResult.Failure(NetworkError.Http(500)))
        val s = store(client)
        s.init()
        advanceUntilIdle()
        assertFalse(s.state.isLoading)
        assertNull(s.state.file)
        assertNotNull(s.state.error)
        s.dispose()
    }

    @Test
    fun `retry triggers new load`() = runTest(testDispatcher) {
        val client = mutableClient(NetworkResult.Failure(NetworkError.Io))
        val s = store(client)
        s.init()
        advanceUntilIdle()
        assertNotNull(s.state.error)
        // Теперь сеть «восстановилась».
        client.result = NetworkResult.Success(BinaryResponse(validPng, "image/png"))
        s.accept(ViewerStore.Intent.Retry)
        advanceUntilIdle()
        assertNull(s.state.error)
        assertNotNull(s.state.file)
        s.dispose()
    }

    @Test
    fun `fullscreen toggles on tap`() = runTest(testDispatcher) {
        val client = mutableClient(NetworkResult.Success(BinaryResponse(validPng, "image/png")))
        val s = store(client)
        s.init()
        advanceUntilIdle()
        assertFalse(s.state.isFullscreen)
        s.accept(ViewerStore.Intent.ToggleFullscreen)
        advanceUntilIdle()
        assertTrue(s.state.isFullscreen)
        s.accept(ViewerStore.Intent.ToggleFullscreen)
        advanceUntilIdle()
        assertFalse(s.state.isFullscreen)
        s.dispose()
    }

    @Test
    fun `new image resets zoom`() = runTest(testDispatcher) {
        val client = mutableClient(NetworkResult.Success(BinaryResponse(validPng, "image/png")))
        val s = store(client)
        s.init()
        advanceUntilIdle()
        s.accept(
            ViewerStore.Intent.ZoomGesture(
                zoomChange = 2f,
                panX = 0f,
                panY = 0f,
                centroidX = 0f,
                centroidY = 0f,
                containerWidth = 100f,
                containerHeight = 100f,
                imageWidth = 100f,
                imageHeight = 100f,
            ),
        )
        advanceUntilIdle()
        assertTrue(s.state.zoom.scale > 1f)
        // Повторная загрузка показывает оригинал в исходном масштабе.
        s.accept(ViewerStore.Intent.Retry)
        advanceUntilIdle()
        assertEquals(1f, s.state.zoom.scale, 0.001f)
        s.dispose()
    }
}
