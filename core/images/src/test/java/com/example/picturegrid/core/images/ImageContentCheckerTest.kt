package com.example.picturegrid.core.images

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Проверка [BitmapImageContentChecker].
 *
 * Robolectric в режиме legacy не разбирает заголовок файла и для любых
 * данных возвращает размеры по умолчанию, поэтому здесь проверяются только
 * случаи, не зависящие от разбора заголовка. Проверка «данные не являются
 * изображением» на реальном устройстве выполняется штатным `BitmapFactory`;
 * в тестах загрузчика она подменяется заглушкой [ImageContentChecker].
 */
@RunWith(RobolectricTestRunner::class)
class ImageContentCheckerTest {

    private val validPng = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
    )

    @Test
    fun `png data is recognised as an image`() {
        assertTrue(BitmapImageContentChecker.isImage(validPng))
    }

    @Test
    fun `empty data is not an image`() {
        assertFalse(BitmapImageContentChecker.isImage(ByteArray(0)))
    }
}
