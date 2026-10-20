package com.example.picturegrid.screenshot

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Базовая проверка screenshot-инфраструктуры на существующем Compose-компоненте.
 *
 * Данные детерминированы: фиксированный SDK/квалификаторы (robolectric.properties),
 * явные [darkTheme]/[dynamicColor], без сети, времени и случайности.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `theme renders deterministic light surface`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Text("PictureGrid")
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/theme_light.png")
    }
}
