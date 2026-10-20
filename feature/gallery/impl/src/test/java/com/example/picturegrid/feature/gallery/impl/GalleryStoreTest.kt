package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.network.NetworkError
import com.example.picturegrid.core.network.NetworkResult
import com.example.picturegrid.feature.gallery.api.GalleryError
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryStoreTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun image(id: String, source: String = "https://example.com/$id.png") =
        GalleryItem.Image(id = id, source = source)

    private fun repo(
        cached: List<GalleryItem>? = null,
        refreshResult: GalleryRefreshResult = GalleryRefreshResult.Success(emptyList()),
    ): GalleryRepository = object : GalleryRepository {
        override suspend fun getCached(): List<GalleryItem>? = cached
        override suspend fun refresh(): GalleryRefreshResult = refreshResult
    }

    private fun store(repository: GalleryRepository): GalleryStore {
        val factory = GalleryStoreFactory(
            storeFactory = com.arkivanov.mvikotlin.main.store.DefaultStoreFactory(),
            repository = repository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
        )
        return factory.create()
    }

    @Test
    fun `initial loading state`() = runTest {
        val s = store(repo(cached = null, refreshResult = GalleryRefreshResult.Success(listOf(image("a")))))
        // autoInit запускает bootstrapper; до advance состояние начальное
        assertTrue(s.state.isLoading || s.state.items.isEmpty())
        s.dispose()
    }

    @Test
    fun `successful initial load shows items`() = runTest {
        val items = listOf(image("a"), image("b"))
        val s = store(repo(cached = null, refreshResult = GalleryRefreshResult.Success(items)))
        s.init()
        advanceUntilIdle()
        assertEquals(items, s.state.items)
        assertFalse(s.state.isLoading)
        assertNull(s.state.error)
        s.dispose()
    }

    @Test
    fun `cached data shown then refreshed`() = runTest {
        val cached = listOf(image("old"))
        val fresh = listOf(image("new1"), image("new2"))
        val s = store(repo(cached = cached, refreshResult = GalleryRefreshResult.Success(fresh)))
        s.init()
        advanceUntilIdle()
        assertEquals(fresh, s.state.items)
        assertFalse(s.state.isRefreshing)
        s.dispose()
    }

    @Test
    fun `error without cache sets fatal error`() = runTest {
        val s = store(
            repo(
                cached = null,
                refreshResult = GalleryRefreshResult.Failure(
                    GalleryError.Network("boom", retryable = true),
                    null,
                ),
            ),
        )
        s.init()
        advanceUntilIdle()
        assertTrue(s.state.error != null)
        assertTrue(s.state.items.isEmpty())
        s.dispose()
    }

    @Test
    fun `refresh error with cache keeps items and sets refreshError`() = runTest {
        val cached = listOf(image("old"))
        val s = store(
            repo(
                cached = cached,
                refreshResult = GalleryRefreshResult.Failure(
                    GalleryError.Network("timeout", retryable = true),
                    cached,
                ),
            ),
        )
        s.init()
        advanceUntilIdle()
        assertEquals(cached, s.state.items)
        assertNotNull(s.state.refreshError)
        assertNull(s.state.error)
        s.dispose()
    }

    @Test
    fun `empty list is not an error`() = runTest {
        val s = store(repo(cached = null, refreshResult = GalleryRefreshResult.Success(emptyList())))
        s.init()
        advanceUntilIdle()
        assertTrue(s.state.items.isEmpty())
        assertFalse(s.state.isLoading)
        assertNull(s.state.error)
        s.dispose()
    }

    @Test
    fun `retry intent triggers reload`() = runTest {
        var refreshCount = 0
        val repository = object : GalleryRepository {
            override suspend fun getCached(): List<GalleryItem>? = null
            override suspend fun refresh(): GalleryRefreshResult {
                refreshCount++
                return GalleryRefreshResult.Success(listOf(image("item$refreshCount")))
            }
        }
        val s = store(repository)
        s.init()
        advanceUntilIdle()
        assertEquals(1, refreshCount)
        s.accept(GalleryStore.Intent.Retry)
        advanceUntilIdle()
        assertEquals(2, refreshCount)
        assertEquals(listOf(image("item2")), s.state.items)
        s.dispose()
    }
}
