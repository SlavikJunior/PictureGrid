package com.example.picturegrid.feature.gallery.impl.presentation.state

import com.example.picturegrid.feature.gallery.api.domain.GalleryError
import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.domain.GalleryRepository
import com.example.picturegrid.feature.gallery.api.domain.NetworkFailure
import com.example.picturegrid.feature.gallery.impl.presentation.ui.shouldRefreshOnNetworkChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Проверки обновления списка по интенту [GalleryStore.Intent.Refresh].
 *
 * Интент нужен экрану, чтобы показать новый список после восстановления
 * сети: фоновая синхронизация меняет только сохранённый список.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GalleryStoreRefreshTest {

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
    fun `refresh loads the list again`() = runTest {
        var refreshCalls = 0
        val repository = object : GalleryRepository {
            override suspend fun getCached(): List<GalleryItem>? = listOf(image("old"))
            override suspend fun refresh(): GalleryRefreshResult {
                refreshCalls++
                return GalleryRefreshResult.Success(listOf(image("new")))
            }
        }
        val store = GalleryStoreFactory(
            storeFactory = com.arkivanov.mvikotlin.main.store.DefaultStoreFactory(),
            repository = repository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
        ).create()
        store.init()
        advanceUntilIdle()
        assertEquals(listOf(image("new")), store.state.items)

        store.accept(GalleryStore.Intent.Refresh)
        advanceUntilIdle()
        assertEquals("список должен быть запрошен повторно", 2, refreshCalls)
        assertEquals(listOf(image("new")), store.state.items)
        store.dispose()
    }

    @Test
    fun `refresh failure keeps the list on the screen`() = runTest {
        val cached = listOf(image("cached"))
        val repository = object : GalleryRepository {
            override suspend fun getCached(): List<GalleryItem>? = cached
            override suspend fun refresh(): GalleryRefreshResult =
                GalleryRefreshResult.Failure(GalleryError.Network(NetworkFailure.IO, retryable = false), cached)
        }
        val store = GalleryStoreFactory(
            storeFactory = com.arkivanov.mvikotlin.main.store.DefaultStoreFactory(),
            repository = repository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
        ).create()
        store.init()
        advanceUntilIdle()
        store.accept(GalleryStore.Intent.Refresh)
        advanceUntilIdle()
        assertEquals("показанный список не должен исчезать", cached, store.state.items)
        assertEquals("ошибка должна быть видна", true, store.state.refreshError != null)
        store.dispose()
    }

    @Test
    fun `network change triggers refresh only when the network is restored`() {
        assertEquals(
            "первое значение — текущее состояние, обновлять список не нужно",
            false,
            shouldRefreshOnNetworkChange(previous = null, current = true),
        )
        assertEquals(
            "сеть пропала — обновление не требуется",
            false,
            shouldRefreshOnNetworkChange(previous = true, current = false),
        )
        assertEquals(
            "сеть восстановилась — список нужно обновить",
            true,
            shouldRefreshOnNetworkChange(previous = false, current = true),
        )
    }
}
