package com.example.picturegrid.core.images

import com.example.picturegrid.core.network.RetrofitNetworkClient
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OriginalImageLoaderRetrofitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @get:Rule
    val server = MockWebServer()

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val validPng = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
    )

    // Проверка содержимого подменяется заглушкой: тесты не зависят от
    // декодера Android.
    private val imageChecker = ImageContentChecker { bytes ->
        runCatching {
            javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes)) != null
        }.getOrDefault(false)
    }

    @Test
    fun `loads and stores image through Retrofit client`() = runTest(testDispatcher) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(validPng))
                .addHeader("Content-Type", "image/png"),
        )
        val storage = ImageStorageImpl(File(tempFolder.root, "orig"), testDispatcher)
        val client = RetrofitNetworkClient(baseUrl = server.url("/").toString())
        val loader = OriginalImageLoader(client, storage, testDispatcher, imageChecker)
        val url = server.url("/img.png").toString()
        val result = loader.load(url)
        assertTrue(result is ImageLoadResult.Success)
        val file = (result as ImageLoadResult.Success).file
        assertTrue(file.exists())
        assertTrue(validPng.contentEquals(file.readBytes()))
    }

    @Test
    fun `returns failure on http error through Retrofit client`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val storage = ImageStorageImpl(File(tempFolder.root, "orig"), testDispatcher)
        val client = RetrofitNetworkClient(baseUrl = server.url("/").toString())
        val loader = OriginalImageLoader(client, storage, testDispatcher, imageChecker)
        val url = server.url("/missing.png").toString()
        val result = loader.load(url)
        assertTrue(result is ImageLoadResult.Failure)
        org.junit.Assert.assertNull(storage.cachedFile(url))
    }
}
