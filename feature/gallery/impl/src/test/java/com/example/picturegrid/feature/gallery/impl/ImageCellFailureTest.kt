package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.images.NotImageContentException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/**
 * Проверка различения ошибок загрузки превью.
 */
class ImageCellFailureTest {

    @Test
    fun `response that is not an image is classified as not image`() {
        assertEquals(
            ImageCellFailure.NotImage,
            classifyImageCellFailure(NotImageContentException()),
        )
    }

    @Test
    fun `other errors are classified as load failed`() {
        assertEquals(
            ImageCellFailure.LoadFailed,
            classifyImageCellFailure(IOException("network error")),
        )
        assertEquals(
            ImageCellFailure.LoadFailed,
            classifyImageCellFailure(IllegalStateException("decode error")),
        )
    }

    @Test
    fun `no error means no failure`() {
        assertNull(classifyImageCellFailure(null))
    }
}
