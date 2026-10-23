package com.example.picturegrid.screenshot

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.example.picturegrid.core.images.LocalImageLoader
import com.example.picturegrid.feature.viewer.api.domain.ViewerError
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStore
import com.example.picturegrid.feature.viewer.impl.presentation.ui.ViewerContent
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30])
class ViewerScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val minimalPng = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
    )

    @Test
    fun `viewer success light`() {
        val file = File.createTempFile("orig", ".png").apply { writeBytes(minimalPng) }
        composeRule.setContent {
            RenderViewer(
                state = ViewerStore.State(urls = listOf("u"), index = 0, file = file, isLoading = false),
            )
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/viewer_success.png")
    }

    @Test
    fun `viewer error light`() {
        composeRule.setContent {
            RenderViewer(
                state = ViewerStore.State(
                    urls = listOf("u"),
                    index = 0,
                    isLoading = false,
                    error = ViewerError.Network,
                ),
            )
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/viewer_error.png")
    }

    @Test
    fun `viewer loading light`() {
        composeRule.setContent {
            RenderViewer(
                state = ViewerStore.State(urls = listOf("u"), index = 0, isLoading = true),
            )
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/viewer_loading.png")
    }

    /**
     * Единственное место, где задаётся окружение экрана: тема и загрузчик
     * изображений. Иначе каждый тест повторял бы обвязку и расходился с
     * остальными.
     */
    @Composable
    private fun RenderViewer(state: ViewerStore.State) {
        val platformContext = LocalPlatformContext.current
        val imageLoader = remember(platformContext) {
            ImageLoader.Builder(platformContext).build()
        }
        PictureGridTheme(darkTheme = false, dynamicColor = false) {
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                Surface {
                    ViewerContent(state = state, onIntent = {})
                }
            }
        }
    }
}
