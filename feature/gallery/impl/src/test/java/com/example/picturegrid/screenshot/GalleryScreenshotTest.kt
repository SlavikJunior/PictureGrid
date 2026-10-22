package com.example.picturegrid.screenshot

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.fetch.FetchResult
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.example.picturegrid.core.images.NotImageContentException
import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.PlaceholderReason
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import java.io.IOException
import com.example.picturegrid.feature.gallery.impl.presentation.state.GalleryStore
import com.example.picturegrid.feature.gallery.impl.presentation.ui.GalleryContent

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30])
class GalleryScreenshotTest {

    private lateinit var imageLoader: ImageLoader

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * URL, который заведомо не является изображением.
     *
     * Тестовый загрузчик отвечает на него ошибкой `NotImageContentException`,
     * поэтому ячейка показывает заглушку «Не изображение».
     */
    private val notImageUrl = "https://example.com/not-an-image.png"

    private val fakeState = GalleryStore.State(
        items = listOf(
            GalleryItem.Image("1", "https://example.com/1.png"),
            GalleryItem.Placeholder("2", "not a url", PlaceholderReason.NotUrl),
            GalleryItem.Placeholder("3", notImageUrl, PlaceholderReason.NotImage),
            GalleryItem.Placeholder("4", "https://example.com/y", PlaceholderReason.LoadFailed),
            GalleryItem.Image("5", notImageUrl),
        ),
        isLoading = false,
    )

    @Before
    fun setUp() {
        // Тест не должен зависеть от сети: Coil получает загрузчик, который
        // всегда завершает запрос ошибкой, поэтому состояние ячейки
        // детерминировано.
        imageLoader = ImageLoader.Builder(org.robolectric.RuntimeEnvironment.getApplication())
                .components {
                    add(
                        Fetcher.Factory<Uri> { data, _, _ ->
                            FailingFetcher(
                                if (data.toString() == notImageUrl) {
                                    NotImageContentException()
                                } else {
                                    IOException("Network error")
                                },
                            )
                        },
                    )
                }
                .build()
    }

    @Test
    fun `gallery light`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    GalleryContent(
                        imageLoader = imageLoader,
                        state = fakeState,
                        onRetry = {},
                        onImageClick = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/gallery_light.png")
    }

    @Test
    fun `gallery dark`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = true, dynamicColor = false) {
                Surface {
                    GalleryContent(
                        imageLoader = imageLoader,
                        state = fakeState,
                        onRetry = {},
                        onImageClick = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/gallery_dark.png")
    }

    @Test
    fun `gallery loading`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    GalleryContent(
                        imageLoader = imageLoader,
                        state = GalleryStore.State(isLoading = true),
                        onRetry = {},
                        onImageClick = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/gallery_loading.png")
    }

    /**
     * Список получен, но пуст: показывается явное состояние, а не пустая сетка.
     */
    @Test
    fun `gallery empty list`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    GalleryContent(
                        imageLoader = imageLoader,
                        state = GalleryStore.State(items = emptyList(), isLoading = false),
                        onRetry = {},
                        onImageClick = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/gallery_empty.png")
    }

    /**
     * Загрузчик, который всегда завершает запрос заданной ошибкой.
     */
    private class FailingFetcher(private val error: Throwable) : Fetcher {
        override suspend fun fetch(): FetchResult = throw error
    }
}
