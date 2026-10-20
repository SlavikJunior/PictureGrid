package com.example.picturegrid.screenshot

import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.doubleClick
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.example.picturegrid.feature.viewer.impl.ViewerContent
import com.example.picturegrid.feature.viewer.impl.ViewerStore
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Проверка жестов просмотрщика: щипок масштабирует изображение, двойное
 * нажатие переключает масштаб.
 *
 * Жесты проверяются по интентам, которые UI отправляет в Store: правила
 * масштаба покрыты отдельными unit-тестами `ViewerZoomTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ViewerGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ViewerStore.Intent>()

    private fun setContent() {
        val file = File.createTempFile("orig", ".png")
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    ViewerContent(
                        state = ViewerStore.State(url = "u", file = file, isLoading = false),
                        onIntent = { intents += it },
                    )
                }
            }
        }
    }

    @Test
    fun `pinch produces zoom gesture`() {
        setContent()
        val center = Offset(200f, 400f)
        composeRule.onRoot().performTouchInput {
            pinch(
                start0 = center,
                end0 = center.copy(x = center.x - 100f, y = center.y - 100f),
                start1 = center,
                end1 = center.copy(x = center.x + 100f, y = center.y + 100f),
            )
        }
        val gesture = intents.filterIsInstance<ViewerStore.Intent.ZoomGesture>()
        assertTrue("Ожидался жест масштабирования", gesture.isNotEmpty())
        assertTrue("Ожидалось изменение масштаба", gesture.any { it.zoomChange != 1f })
    }

    @Test
    fun `double tap toggles zoom`() {
        setContent()
        composeRule.onRoot().performTouchInput { doubleClick(Offset(200f, 400f)) }
        assertTrue(
            "Ожидалось переключение масштаба",
            intents.any { it is ViewerStore.Intent.ToggleZoom },
        )
    }
}
