package com.example.picturegrid.feature.gallery.impl.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import androidx.compose.runtime.CompositionLocalProvider
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.example.picturegrid.core.images.LocalImageLoader
import com.example.picturegrid.core.images.NotImageContentException
import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast
import java.io.IOException

/**
 * Клик по ячейке с неудачной загрузкой.
 *
 * Требование: такая ячейка не открывает просмотр — иначе лист покажет ошибку
 * вместо изображения. Вместо перехода пользователь видит сообщение об ошибке,
 * а активным остаётся только действие «Повторить».
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30])
class ImageCellClickTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        ShadowToast.reset()
    }

    @Test
    fun `failed image shows toast and does not open viewer`() {
        val opened = openViewerWhenClicked(error = IOException("boom"))

        assertEquals("клик не должен открывать просмотр", 0, opened.value)
        assertEquals(
            "пользователь должен видеть причину отказа",
            "Не удалось загрузить изображение",
            ShadowToast.getTextOfLatestToast(),
        )
    }

    @Test
    fun `not an image stays a placeholder without toast`() {
        val opened = openViewerWhenClicked(error = NotImageContentException())

        assertEquals("повтор для не-изображения бессмысленен", 0, opened.value)
        assertNull("для заглушки не нужен toast", ShadowToast.getTextOfLatestToast())
    }

    /**
     * Рендерит ячейку и кликает по её левому верхнему углу — в стороне от
     * кнопки «Повторить», чтобы проверять именно клик по самой ячейке.
     */
    private fun openViewerWhenClicked(error: Throwable): OpenedCounter {
        val opened = OpenedCounter()
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                CompositionLocalProvider(
                    LocalImageLoader provides failingImageLoader(error),
                ) {
                    Box(Modifier.size(200.dp)) {
                        GalleryCell(
                            item = GalleryItem.Image(id = "1", source = "https://example.com/1.png"),
                            onImageClick = { opened.value++ },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(Offset(12f, 12f)) }
        composeRule.waitForIdle()
        return opened
    }

    private fun failingImageLoader(error: Throwable): ImageLoader =
        ImageLoader.Builder(RuntimeEnvironment.getApplication())
            .components {
                add(
                    Fetcher.Factory<Uri> { _, _, _ ->
                        object : Fetcher {
                            override suspend fun fetch(): FetchResult = throw error
                        }
                    },
                )
            }
            .build()

    private class OpenedCounter {
        var value: Int = 0
    }
}
