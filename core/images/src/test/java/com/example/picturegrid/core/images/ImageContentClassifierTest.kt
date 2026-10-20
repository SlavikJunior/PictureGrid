package com.example.picturegrid.core.images

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверка классификатора содержимого ответа.
 */
class ImageContentClassifierTest {

    @Test
    fun `text content types are not images`() {
        assertTrue(ImageContentClassifier.isNotImageContentType("text/html"))
        assertTrue(ImageContentClassifier.isNotImageContentType("text/html; charset=utf-8"))
        assertTrue(ImageContentClassifier.isNotImageContentType("text/plain"))
        assertTrue(ImageContentClassifier.isNotImageContentType("application/json"))
        assertTrue(ImageContentClassifier.isNotImageContentType("application/xml"))
        assertTrue(ImageContentClassifier.isNotImageContentType("application/pdf"))
    }

    @Test
    fun `image content types are images`() {
        assertFalse(ImageContentClassifier.isNotImageContentType("image/png"))
        assertFalse(ImageContentClassifier.isNotImageContentType("image/jpeg"))
        assertFalse(ImageContentClassifier.isNotImageContentType("image/webp"))
        assertTrue(ImageContentClassifier.isImageContentType("image/png"))
        assertTrue(ImageContentClassifier.isImageContentType("image/png; charset=binary"))
    }

    @Test
    fun `ambiguous content types are not classified by header`() {
        assertFalse(ImageContentClassifier.isNotImageContentType("application/octet-stream"))
        assertFalse(ImageContentClassifier.isNotImageContentType(null))
        assertFalse(ImageContentClassifier.isImageContentType("application/octet-stream"))
        assertFalse(ImageContentClassifier.isImageContentType(null))
    }

    @Test
    fun `image signatures are recognized`() {
        assertTrue(ImageContentClassifier.isImageSignature(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)))
        assertTrue(
            ImageContentClassifier.isImageSignature(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            ),
        )
        assertTrue(ImageContentClassifier.isImageSignature("GIF89a".toByteArray()))
        assertTrue(ImageContentClassifier.isImageSignature("RIFF0000WEBPVP8 ".toByteArray()))
        assertTrue(ImageContentClassifier.isImageSignature("BM0000".toByteArray()))
        assertTrue(
            ImageContentClassifier.isImageSignature(
                byteArrayOf(0x00, 0x00, 0x00, 0x18) + "ftypavif".toByteArray(),
            ),
        )
    }

    @Test
    fun `non image content is not recognized as image`() {
        assertFalse(ImageContentClassifier.isImageSignature(ByteArray(0)))
        assertFalse(ImageContentClassifier.isImageSignature("<html>".toByteArray()))
        assertFalse(ImageContentClassifier.isImageSignature("not an image at all".toByteArray()))
        assertFalse(
            ImageContentClassifier.isImageSignature(
                byteArrayOf(0x00, 0x00, 0x00, 0x18) + "ftypmp42".toByteArray(),
            ),
        )
    }
}
