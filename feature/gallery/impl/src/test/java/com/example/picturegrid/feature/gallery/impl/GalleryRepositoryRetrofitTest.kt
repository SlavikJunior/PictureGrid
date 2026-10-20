package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.network.RetrofitNetworkClient
import com.example.picturegrid.feature.gallery.api.GalleryError
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GalleryRepositoryRetrofitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @get:Rule
    val server = MockWebServer()

    private val testDispatcher = StandardTestDispatcher()

    private fun storage() = GalleryListStorage(File(tempFolder.root, "list.txt"))

    @Test
    fun `refresh succeeds through Retrofit client`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("https://example.com/a.png\nhttps://example.com/b.jpg"))
        val storage = storage()
        val repo = GalleryRepositoryImpl(
            networkClient = RetrofitNetworkClient(baseUrl = server.url("/").toString()),
            storage = storage,
            url = server.url("/images.txt").toString(),
            ioDispatcher = testDispatcher,
        )
        val result = repo.refresh()
        assertTrue(result is GalleryRefreshResult.Success)
        assertEquals(2, (result as GalleryRefreshResult.Success).items.size)
        assertEquals("https://example.com/a.png\nhttps://example.com/b.jpg", storage.read())
    }

    @Test
    fun `refresh returns failure with cached on http error through Retrofit client`() = runTest(testDispatcher) {
        storage().write("https://example.com/saved.png")
        server.enqueue(MockResponse().setResponseCode(500))
        val storage = storage()
        val repo = GalleryRepositoryImpl(
            networkClient = RetrofitNetworkClient(baseUrl = server.url("/").toString()),
            storage = storage,
            url = server.url("/images.txt").toString(),
            ioDispatcher = testDispatcher,
        )
        val result = repo.refresh()
        assertTrue(result is GalleryRefreshResult.Failure)
        val failure = result as GalleryRefreshResult.Failure
        assertEquals(1, failure.cached?.size)
        assertTrue((failure.error as GalleryError.Network).retryable)
        assertEquals("https://example.com/saved.png", storage.read())
    }

    @Test
    fun `refresh returns non retryable failure on 404 through Retrofit client`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val storage = storage()
        val repo = GalleryRepositoryImpl(
            networkClient = RetrofitNetworkClient(baseUrl = server.url("/").toString()),
            storage = storage,
            url = server.url("/images.txt").toString(),
            ioDispatcher = testDispatcher,
        )
        val result = repo.refresh()
        assertTrue(result is GalleryRefreshResult.Failure)
        val failure = result as GalleryRefreshResult.Failure
        assertFalse((failure.error as GalleryError.Network).retryable)
    }
}
