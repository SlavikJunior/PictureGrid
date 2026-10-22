package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkError
import com.example.picturegrid.core.network.NetworkResult
import com.example.picturegrid.feature.gallery.api.domain.GalleryError
import com.example.picturegrid.feature.gallery.api.domain.GalleryRefreshResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun storage(): GalleryListStorage =
        GalleryListStorage(File(tempFolder.root, "list.txt"))

    private fun repo(client: NetworkClient): GalleryRepositoryImpl =
        GalleryRepositoryImpl(
            networkClient = client,
            storage = storage(),
            url = "https://it-link.ru/test/images.txt",
            ioDispatcher = testDispatcher,
        )

    @Test
    fun `getCached returns null when nothing stored`() = runTest {
        assertNull(repo(FakeNetworkClient(NetworkResult.Success(""))).getCached())
    }

    @Test
    fun `getCached parses stored text without network`() = runTest {
        storage().write("https://example.com/a.png\nnot a url")
        val cached = repo(FakeNetworkClient(NetworkResult.Success(""))).getCached()
        assertEquals(2, cached?.size)
    }

    @Test
    fun `refresh success updates storage and returns items`() = runTest {
        val client = FakeNetworkClient(
            NetworkResult.Success("https://example.com/a.png\nhttps://example.com/b.jpg")
        )
        val result = repo(client).refresh()
        assertTrue(result is GalleryRefreshResult.Success)
        assertEquals(2, (result as GalleryRefreshResult.Success).items.size)
        assertEquals("https://example.com/a.png\nhttps://example.com/b.jpg", storage().read())
    }

    @Test
    fun `refresh success leaves no temp file`() = runTest {
        repo(FakeNetworkClient(NetworkResult.Success("https://example.com/a.png"))).refresh()
        val remaining = tempFolder.root.listFiles()?.map { it.name } ?: emptyList()
        assertEquals(listOf("list.txt"), remaining)
    }

    @Test
    fun `refresh network failure without cached returns failure with null cached`() = runTest {
        val result = repo(FakeNetworkClient(NetworkResult.Failure(NetworkError.Io))).refresh()
        assertTrue(result is GalleryRefreshResult.Failure)
        val failure = result as GalleryRefreshResult.Failure
        assertTrue(failure.error is GalleryError.Network)
        assertTrue((failure.error as GalleryError.Network).retryable)
        assertNull(failure.cached)
    }

    @Test
    fun `refresh network failure with cached returns failure with cached items`() = runTest {
        storage().write("https://example.com/saved.png")
        val result = repo(FakeNetworkClient(NetworkResult.Failure(NetworkError.Timeout))).refresh()
        val failure = result as GalleryRefreshResult.Failure
        assertTrue(failure.error is GalleryError.Network)
        assertTrue((failure.error as GalleryError.Network).retryable)
        assertEquals(1, failure.cached?.size)
        // сохранённый файл не пострадал
        assertEquals("https://example.com/saved.png", storage().read())
    }

    @Test
    fun `refresh http error returns network failure`() = runTest {
        val result = repo(FakeNetworkClient(NetworkResult.Failure(NetworkError.Http(404)))).refresh()
        val failure = result as GalleryRefreshResult.Failure
        assertTrue(failure.error is GalleryError.Network)
        assertFalse((failure.error as GalleryError.Network).retryable)
    }

    @Test
    fun `refresh empty body returns network failure`() = runTest {
        val result = repo(FakeNetworkClient(NetworkResult.Failure(NetworkError.EmptyBody))).refresh()
        val failure = result as GalleryRefreshResult.Failure
        assertTrue(failure.error is GalleryError.Network)
        assertTrue((failure.error as GalleryError.Network).retryable)
    }

    @Test
    fun `refresh write failure via occupied tmp path returns storage failure with cached`() = runTest {
        storage().write("https://example.com/old.png")
        // Занимаем путь временного файла каталогом, чтобы записать не удалось.
        File(tempFolder.root, "list.txt.tmp").mkdirs()
        val result = repo(
            FakeNetworkClient(NetworkResult.Success("https://example.com/new.png"))
        ).refresh()
        val failure = result as GalleryRefreshResult.Failure
        assertTrue(failure.error is GalleryError.Storage)
        assertFalse((failure.error as GalleryError.Storage).retryable)
        assertEquals(1, failure.cached?.size)
        assertEquals("https://example.com/old.png", storage().read())
    }


    @Test
    fun `refresh propagates cancellation`() = runTest {
        val client = object : NetworkClient {
            override suspend fun fetchText(url: String): NetworkResult<String> {
                throw CancellationException("cancelled")
            }
        }
        try {
            repo(client).refresh()
            org.junit.Assert.fail("Expected CancellationException")
        } catch (e: CancellationException) {
            // expected
        }
    }

    @Test
    fun `unusable cache is treated as missing`() = runTest {
        File(tempFolder.root, "list.txt").writeBytes(ByteArray(0))
        assertNull(repo(FakeNetworkClient(NetworkResult.Success(""))).getCached())
    }

    @Test
    fun `successful refresh replaces unusable cache`() = runTest {
        File(tempFolder.root, "list.txt").writeBytes(ByteArray(0))
        val result = repo(
            FakeNetworkClient(NetworkResult.Success("https://example.com/new.png"))
        ).refresh()
        assertTrue(result is GalleryRefreshResult.Success)
        assertEquals("https://example.com/new.png", storage().read())
    }

    @Test
    fun `unusable cache is not deleted by failed refresh`() = runTest {
        val file = File(tempFolder.root, "list.txt")
        file.writeBytes(ByteArray(0))
        repo(FakeNetworkClient(NetworkResult.Failure(NetworkError.Io))).refresh()
        assertTrue("повреждённый кэш не должен удаляться", file.exists())
    }

    private class FakeNetworkClient(
        private val result: NetworkResult<String>,
    ) : NetworkClient {
        override suspend fun fetchText(url: String): NetworkResult<String> = result
    }
}
