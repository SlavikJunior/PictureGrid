package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkResult
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
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
    fun `parallel refresh keeps storage valid`() = runTest(testDispatcher) {
        val storage = GalleryListStorage(File(tempFolder.root, "list.txt"))
        val calls = AtomicInteger(0)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val client = object : NetworkClient {
            override suspend fun fetchText(url: String): NetworkResult<String> {
                val index = calls.incrementAndGet()
                if (index == 2) started.complete(Unit)
                started.await()
                release.await()
                return if (index == 1) {
                    NetworkResult.Success("first content")
                } else {
                    NetworkResult.Success("second content")
                }
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
            started.await()
            release.complete(Unit)
            val firstResult = first.await()
            val secondResult = second.await()
            assertTrue(firstResult is GalleryRefreshResult.Success)
            assertTrue(secondResult is GalleryRefreshResult.Success)
        }

        val stored = (storage.read() as? GalleryListCache.Available)?.content
        assertTrue(stored == "first content" || stored == "second content")
    }
}
