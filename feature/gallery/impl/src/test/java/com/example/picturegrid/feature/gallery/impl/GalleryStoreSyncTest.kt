package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.feature.gallery.api.GalleryError
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class GalleryStoreSyncTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun image(id: String) = GalleryItem.Image(id = id, source = "https://example.com/$id.png")

    @Test
    fun `network failure schedules sync`() = runTest {
        var scheduled = 0
        val cached = listOf(image("old"))
        val repository = object : GalleryRepository {
            override suspend fun getCached(): List<GalleryItem>? = cached
            override suspend fun refresh(): GalleryRefreshResult =
                GalleryRefreshResult.Failure(GalleryError.Network("io", retryable = true), cached)
        }
        val store = GalleryStoreFactory(
            storeFactory = com.arkivanov.mvikotlin.main.store.DefaultStoreFactory(),
            repository = repository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
            syncScheduler = { scheduled++ },
        ).create()
        store.init()
        advanceUntilIdle()
        assertEquals(1, scheduled)
        store.dispose()
    }

    @Test
    fun `reload cached updates state without network`() = runTest {
        val cached = listOf(image("cached"))
        val repository = object : GalleryRepository {
            override suspend fun getCached(): List<GalleryItem>? = cached
            override suspend fun refresh(): GalleryRefreshResult =
                GalleryRefreshResult.Success(emptyList())
        }
        val store = GalleryStoreFactory(
            storeFactory = com.arkivanov.mvikotlin.main.store.DefaultStoreFactory(),
            repository = repository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
        ).create()
        store.init()
        advanceUntilIdle()
        store.accept(GalleryStore.Intent.ReloadCached)
        advanceUntilIdle()
        assertEquals(cached, store.state.items)
        store.dispose()
    }

    @Test
    fun `successful refresh does not schedule sync`() = runTest {
        var scheduled = 0
        val repository = object : GalleryRepository {
            override suspend fun getCached(): List<GalleryItem>? = null
            override suspend fun refresh(): GalleryRefreshResult =
                GalleryRefreshResult.Success(listOf(image("new")))
        }
        val store = GalleryStoreFactory(
            storeFactory = com.arkivanov.mvikotlin.main.store.DefaultStoreFactory(),
            repository = repository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
            syncScheduler = { scheduled++ },
        ).create()
        store.init()
        advanceUntilIdle()
        assertEquals(0, scheduled)
        store.dispose()
    }

    @Test
    fun `network failure without cached list shows the error and schedules sync`() = runTest {
        var scheduled = 0
        val repository = object : GalleryRepository {
            override suspend fun getCached(): List<GalleryItem>? = null
            override suspend fun refresh(): GalleryRefreshResult =
                GalleryRefreshResult.Failure(GalleryError.Network("Ошибка сети", retryable = true), null)
        }
        val store = GalleryStoreFactory(
            storeFactory = com.arkivanov.mvikotlin.main.store.DefaultStoreFactory(),
            repository = repository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
            syncScheduler = { scheduled++ },
        ).create()
        store.init()
        advanceUntilIdle()
        assertEquals(1, scheduled)
        assertTrue("пользователь должен видеть причину", store.state.error?.contains("Сеть") == true)
        store.dispose()
    }
}
