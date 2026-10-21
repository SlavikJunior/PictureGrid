package com.example.picturegrid.feature.gallery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.picturegrid.feature.gallery.api.NetworkMonitor
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import com.example.picturegrid.feature.gallery.api.PlaceholderReason
import com.example.picturegrid.feature.gallery.impl.GalleryScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Проверка реакции галереи на восстановление сети.
 *
 * Фоновая синхронизация обновляет сохранённый список, но экран об этом не
 * узнаёт: без реакции на сеть пользователь видел бы старый список до
 * перезапуска приложения, хотя требование задания прямо требует повторной
 * загрузки при появлении сети.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GalleryNetworkRecoveryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `gallery reloads the list when the network is restored`() {
        val repository = FakeGallerySource()
        val monitor = FakeNetworkMonitor()
        composeRule.setContent {
            GalleryScreen(repository = repository, networkMonitor = monitor)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Не ссылка").assertIsDisplayed()

        // Список в хранилище обновился, но экран об этом ещё не знает.
        repository.result = GalleryRefreshResult.Success(listOf(placeholder(PlaceholderReason.NotImage)))
        monitor.setAvailable(false)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Не ссылка").assertIsDisplayed()

        monitor.setAvailable(true)
        awaitUntil("список не обновился после восстановления сети") {
            repository.refreshCalls >= 2
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Не изображение").assertIsDisplayed()
    }

    /**
     * Ждёт условия, прокручивая main-лопер.
     *
     * `waitUntil` из compose-тестов в Robolectric не прокручивает лопер, а
     * обновление списка приходит из корутины главного потока.
     */
    private fun awaitUntil(message: String, condition: () -> Boolean) {
        repeat(100) {
            composeRule.waitForIdle()
            if (condition()) return
            Thread.sleep(50)
        }
        throw AssertionError(message)
    }

    private fun placeholder(reason: PlaceholderReason) =
        GalleryItem.Placeholder(id = reason.name, source = "https://example.com/x", reason = reason)

    private class FakeGallerySource : GalleryRepository {
        var result: GalleryRefreshResult = GalleryRefreshResult.Success(
            listOf(
                GalleryItem.Placeholder(
                    id = "not-url",
                    source = "not a url",
                    reason = PlaceholderReason.NotUrl,
                ),
            ),
        )
        var refreshCalls: Int = 0

        override suspend fun getCached(): List<GalleryItem>? = null

        override suspend fun refresh(): GalleryRefreshResult {
            refreshCalls++
            return result
        }
    }

    private class FakeNetworkMonitor : NetworkMonitor {
        private val state = MutableStateFlow(true)
        override val isAvailable: Flow<Boolean> = state

        fun setAvailable(available: Boolean) {
            state.value = available
        }
    }
}
