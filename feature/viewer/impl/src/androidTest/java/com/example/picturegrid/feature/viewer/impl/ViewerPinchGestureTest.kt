package com.example.picturegrid.feature.viewer.impl

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.picturegrid.core.designsystem.PictureGridTheme
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Инструментальная проверка щипка в просмотрщике.
 *
 * Жест выполняется на настоящем устройстве: мультитач нельзя достоверно
 * воспроизвести в unit-тесте, а Compose принимает два указателя только в
 * реальном сценарии ввода.
 */
@RunWith(AndroidJUnit4::class)
class ViewerPinchGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinchEnlargesImage() {
        val intents = mutableListOf<ViewerStore.Intent>()
        val image = createImage()
        composeRule.setContent {
            PictureGridTheme {
                ViewerContent(
                    state = ViewerStore.State(
                        urls = listOf("https://example.com/1.png"),
                        index = 0,
                        isLoading = false,
                        file = image,
                    ),
                    onIntent = { intents += it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ZOOMABLE_TEST_TAG).performTouchInput {
            // Два указателя расходятся от центра: это щипок на увеличение.
            val middle = center
            down(0, middle - Offset(START_OFFSET, 0f))
            down(1, middle + Offset(START_OFFSET, 0f))
            repeat(STEPS) { step ->
                val distance = START_OFFSET + step * STEP
                moveTo(0, middle - Offset(distance, 0f))
                moveTo(1, middle + Offset(distance, 0f))
            }
            up(0)
            up(1)
        }
        composeRule.waitForIdle()

        val gesture = intents.filterIsInstance<ViewerStore.Intent.ZoomGesture>().firstOrNull()
        assertNotNull("жест масштабирования должен обрабатываться", gesture)
        assertTrue(
            "коэффициент масштаба должен быть больше единицы, а не ${gesture?.zoomChange}",
            gesture!!.zoomChange > 1f,
        )
    }

    private fun createImage(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "pinch-test-image.png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return file
    }

    private companion object {
        const val START_OFFSET = 20f
        const val STEP = 15f
        const val STEPS = 8
    }
}
