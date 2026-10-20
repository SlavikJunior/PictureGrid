package com.example.picturegrid.core.images

import com.example.picturegrid.core.network.BinaryResponse
import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkError
import com.example.picturegrid.core.network.NetworkResult
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OriginalImageLoaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val validPng = createMinimalPng()

    // Проверка содержимого подменяется заглушкой: тесты не зависят от
    // декодера Android.
    private val imageChecker = ImageContentChecker { bytes ->
        runCatching {
            javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes)) != null
        }.getOrDefault(false)
    }

    private fun storage() = ImageStorageImpl(File(tempFolder.root, "orig"), testDispatcher)
    private fun loader(client: NetworkClient) = OriginalImageLoader(
        networkClient = client,
        storage = storage(),
        ioDispatcher = testDispatcher,
        imageChecker = imageChecker,
    )

    @Test
    fun `successful download stores and returns file`() = runTest(testDispatcher) {
        val client = FakeBytesClient(NetworkResult.Success(BinaryResponse(validPng, "image/png")))
        val l = loader(client)
        val result = l.load("https://example.com/img.png")
        assertTrue(result.isSuccess)
        val file = result.getOrNull()!!
        assertTrue(file.exists())
        assertEquals(validPng.size, file.readBytes().size)
    }

    @Test
    fun `http error returns failure`() = runTest(testDispatcher) {
        val client = FakeBytesClient(NetworkResult.Failure(NetworkError.Http(404)))
        val result = loader(client).load("https://example.com/img.png")
        assertTrue(result.isFailure)
    }

    @Test
    fun `invalid bytes return failure and do not store`() = runTest(testDispatcher) {
        val client = FakeBytesClient(NetworkResult.Success(BinaryResponse("not an image".toByteArray(), "image/png")))
        val storage = storage()
        val l = OriginalImageLoader(client, storage, testDispatcher, imageChecker)
        val result = l.load("https://example.com/img.png")
        assertTrue(result.isFailure)
        assertNull(storage.cachedFile("https://example.com/img.png"))
    }

    @Test
    fun `empty body is rejected and not stored`() = runTest(testDispatcher) {
        val client = FakeBytesClient(NetworkResult.Success(BinaryResponse(ByteArray(0), "image/png")))
        val storage = storage()
        val l = OriginalImageLoader(client, storage, testDispatcher, imageChecker)
        val result = l.load("https://example.com/img.png")
        assertTrue(result.isFailure)
        assertNull(storage.cachedFile("https://example.com/img.png"))
    }

    @Test
    fun `valid image is stored unchanged`() = runTest(testDispatcher) {
        val client = FakeBytesClient(NetworkResult.Success(BinaryResponse(validPng, "image/png")))
        val storage = storage()
        val l = OriginalImageLoader(client, storage, testDispatcher, imageChecker)
        val result = l.load("https://example.com/img.png")
        assertTrue(result.isSuccess)
        // Оригинал сохраняется без изменений.
        assertTrue(validPng.contentEquals(result.getOrNull()!!.readBytes()))
    }

    @Test
    fun `cached file is reused without network`() = runTest(testDispatcher) {
        val client = FakeBytesClient(NetworkResult.Success(BinaryResponse(validPng, "image/png")))
        val storage = storage()
        val l = OriginalImageLoader(client, storage, testDispatcher, imageChecker)
        l.load("https://example.com/img.png")
        // Второй вызов не должен обращаться к сети.
        val counting = CountingBytesClient(client)
        val l2 = OriginalImageLoader(counting, storage, testDispatcher, imageChecker)
        val result = l2.load("https://example.com/img.png")
        assertTrue(result.isSuccess)
        assertEquals(0, counting.calls)
    }

    @Test
    fun `stored original survives new storage instance`() = runTest(testDispatcher) {
        val url = "https://example.com/img.png"
        val dir = File(tempFolder.root, "persisted")
        val storage1 = ImageStorageImpl(dir, testDispatcher)
        val client1 = FakeBytesClient(NetworkResult.Success(BinaryResponse(validPng, "image/png")))
        val loader1 = OriginalImageLoader(client1, storage1, testDispatcher, imageChecker)
        val firstResult = loader1.load(url)
        assertTrue(firstResult.isSuccess)
        val storedFile = firstResult.getOrNull()!!

        val counting = CountingBytesClient(
            FakeBytesClient(NetworkResult.Failure(NetworkError.Io)),
        )
        val storage2 = ImageStorageImpl(dir, testDispatcher)
        val loader2 = OriginalImageLoader(counting, storage2, testDispatcher, imageChecker)
        val secondResult = loader2.load(url)

        assertTrue(secondResult.isSuccess)
        assertEquals(0, counting.calls)
        assertEquals(storedFile.absolutePath, secondResult.getOrNull()!!.absolutePath)
        assertTrue(secondResult.getOrNull()!!.exists())
    }

    @Test
    fun `content type text html is rejected`() = runTest(testDispatcher) {
        val client = FakeBytesClient(NetworkResult.Success(BinaryResponse(validPng, "text/html")))
        val result = loader(client).load("https://example.com/img.png")
        assertTrue(result.isFailure)
    }

    private fun assertNull(f: File?) = org.junit.Assert.assertNull(f)

    private class FakeBytesClient(private val result: NetworkResult<BinaryResponse>) : NetworkClient {
        override suspend fun fetchText(url: String): NetworkResult<String> = NetworkResult.Failure(NetworkError.Io)
        override suspend fun fetchBytes(url: String): NetworkResult<BinaryResponse> = result
    }

    private class CountingBytesClient(private val delegate: NetworkClient) : NetworkClient {
        var calls = 0
        override suspend fun fetchText(url: String): NetworkResult<String> = delegate.fetchText(url)
        override suspend fun fetchBytes(url: String): NetworkResult<BinaryResponse> {
            calls++
            return delegate.fetchBytes(url)
        }
    }

    companion object {
        fun createMinimalPng(): ByteArray =
            java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
            )
    }
}
