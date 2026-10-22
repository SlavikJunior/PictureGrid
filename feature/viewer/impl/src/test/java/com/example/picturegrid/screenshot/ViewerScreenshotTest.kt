package com.example.picturegrid.screenshot

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import java.io.File
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStore
import com.example.picturegrid.feature.viewer.impl.presentation.ui.ViewerContent

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
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    ViewerContent(state = ViewerStore.State(urls = listOf("u"), index = 0, file = file, isLoading = false), onIntent = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/viewer_success.png")
    }

    @Test
    fun `viewer error light`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    ViewerContent(state = ViewerStore.State(urls = listOf("u"), index = 0, isLoading = false, error = "Network error"), onIntent = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/viewer_error.png")
    }

    @Test
    fun `viewer loading light`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    ViewerContent(state = ViewerStore.State(urls = listOf("u"), index = 0, isLoading = true), onIntent = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/viewer_loading.png")
    }

}
