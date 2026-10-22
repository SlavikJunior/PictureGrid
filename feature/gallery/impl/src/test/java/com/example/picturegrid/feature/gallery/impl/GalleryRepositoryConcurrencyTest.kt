package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkResult
import com.example.picturegrid.feature.gallery.api.domain.GalleryRefreshResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryRepositoryConcurrencyTest {

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

    @Test
    fun `parallel refresh performs a single network request`() = runTest(testDispatcher) {
        val storage = GalleryListStorage(File(tempFolder.root, "list.txt"))
        val calls = AtomicInteger(0)
        val requestStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val client = object : NetworkClient {
            override suspend fun fetchText(url: String): NetworkResult<String> {
                calls.incrementAndGet()
                requestStarted.complete(Unit)
                release.await()
                return NetworkResult.Success("content")
            }
        }
        val repo = GalleryRepositoryImpl(
            networkClient = client,
            storage = storage,
            url = "https://example.com/list.txt",
            ioDispatcher = testDispatcher,
        )

        coroutineScope {
            val first = async { repo.refresh() }
            val second = async { repo.refresh() }
            requestStarted.await()
            // Дать обоим вызовам присоединиться к общей загрузке.
            testScheduler.advanceUntilIdle()
            release.complete(Unit)
            val firstResult = first.await()
            val secondResult = second.await()
            assertTrue(firstResult is GalleryRefreshResult.Success)
            assertTrue(secondResult is GalleryRefreshResult.Success)
        }

        assertEquals("параллельный refresh должен выполнять один запрос", 1, calls.get())
        assertEquals("content", storage.read())
    }

    @Test
    fun `sequential refresh performs a request each time`() = runTest(testDispatcher) {
        val storage = GalleryListStorage(File(tempFolder.root, "list.txt"))
        val calls = AtomicInteger(0)
        val client = object : NetworkClient {
            override suspend fun fetchText(url: String): NetworkResult<String> {
                calls.incrementAndGet()
                return NetworkResult.Success("content $calls")
            }
        }
        val repo = GalleryRepositoryImpl(
            networkClient = client,
            storage = storage,
            url = "https://example.com/list.txt",
            ioDispatcher = testDispatcher,
        )

        repo.refresh()
        repo.refresh()

        assertEquals("последовательные вызовы должны выполнять запрос каждый раз", 2, calls.get())
    }
}
