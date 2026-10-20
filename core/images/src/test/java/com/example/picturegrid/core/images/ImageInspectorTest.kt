package com.example.picturegrid.core.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Проверки чтения размеров и ограничений изображения (PG-SEC-002).
 *
 * Проверка не должна выделять память под полноразмерный bitmap: размеры
 * читаются из заголовка файла, а декодируется только уменьшенная копия.
 *
 * Тест использует native-графику Robolectric, чтобы поведение
 * `BitmapFactory` совпадало с устройством.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageInspectorTest {

    @Test
    fun `valid png is accepted`() {
        assertEquals(ImageContent.VALID, ImageInspector.check(validPng()))
    }

    @Test
    fun `dimensions are read from header`() {
        val dimensions = ImageInspector.readDimensions(validPng())
        assertEquals(1, dimensions?.width)
        assertEquals(1, dimensions?.height)
    }

    @Test
    fun `corrupted data is not an image`() {
        assertEquals(ImageContent.NOT_IMAGE, ImageInspector.check("not an image".toByteArray()))
    }

    @Test
    fun `truncated png is not an image`() {
        val truncated = validPng().copyOf(30)
        assertEquals(ImageContent.NOT_IMAGE, ImageInspector.check(truncated))
    }

    @Test
    fun `empty data is not an image`() {
        assertEquals(ImageContent.NOT_IMAGE, ImageInspector.check(ByteArray(0)))
        assertNull(ImageInspector.readDimensions(ByteArray(0)))
    }

    @Test
    fun `image with excessive side is rejected`() {
        // Сторона больше ImageLimits.MAX_DIMENSION.
        val png = createPng(width = ImageLimits.MAX_DIMENSION + 1, height = 1)
        assertEquals(ImageContent.TOO_LARGE, ImageInspector.check(png))
    }

    @Test
    fun `image with excessive pixel count is rejected`() {
        // Стороны допустимы, но число пикселей превышает лимит.
        val png = createPng(width = 12_000, height = 5_001)
        assertEquals(ImageContent.TOO_LARGE, ImageInspector.check(png))
    }

    @Test
    fun `image within limits is accepted`() {
        val png = createPng(width = 64, height = 64)
        assertEquals(ImageContent.VALID, ImageInspector.check(png))
        val dimensions = ImageInspector.readDimensions(png)
        assertEquals(64, dimensions?.width)
        assertEquals(64, dimensions?.height)
    }

    @Test
    fun `dimensions within limits are acceptable`() {
        val dimensions = ImageDimensions(4_032, 3_024)
        assertTrue(ImageInspector.isAcceptable(dimensions))
        assertEquals(12_192_768L, dimensions.pixels)
    }

    @Test
    fun `dimensions above limits are not acceptable`() {
        assertFalse(ImageInspector.isAcceptable(ImageDimensions(12_001, 100)))
        assertFalse(ImageInspector.isAcceptable(ImageDimensions(100, 12_001)))
        assertFalse(ImageInspector.isAcceptable(ImageDimensions(10_000, 10_000)))
    }

    @Test
    fun `sample size keeps decoded copy small`() {
        val sample = ImageInspector.sampleSizeFor(ImageDimensions(4_032, 3_024))
        assertTrue("sample size должен быть степенью двойки", sample > 0 && (sample and (sample - 1)) == 0)
        val pixels = (4_032L / sample) * (3_024L / sample)
        assertTrue(
            "уменьшенная копия должна укладываться в лимит: $pixels",
            pixels <= ImageLimits.SAMPLE_MAX_PIXELS,
        )
    }

    private fun validPng(): ByteArray = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
    )

    /**
     * Создаёт корректный PNG заданных размеров (1 бит, оттенки серого).
     *
     * Однобитный формат позволяет построить большое изображение без
     * заметных затрат памяти: строка занимает 1 + width/8 байт.
     */
    private fun createPng(width: Int, height: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        val ihdr = ByteArrayOutputStream()
        ihdr.write(intToBytes(width))
        ihdr.write(intToBytes(height))
        ihdr.write(1) // bit depth
        ihdr.write(0) // color type: grayscale
        ihdr.write(0) // compression
        ihdr.write(0) // filter
        ihdr.write(0) // interlace
        writeChunk(out, "IHDR", ihdr.toByteArray())

        val rowBytes = (width + 7) / 8
        val raw = ByteArrayOutputStream()
        repeat(height) {
            raw.write(0) // filter type: none
            raw.write(ByteArray(rowBytes))
        }
        writeChunk(out, "IDAT", deflate(raw.toByteArray()))
        writeChunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_SPEED)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (!deflater.finished()) {
            val written = deflater.deflate(buffer)
            out.write(buffer, 0, written)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        out.write(intToBytes(data.size))
        val typeAndData = ByteArray(type.length + data.size)
        type.toByteArray(Charsets.US_ASCII).copyInto(typeAndData, 0)
        data.copyInto(typeAndData, type.length)
        out.write(typeAndData)
        val crc = CRC32()
        crc.update(typeAndData)
        out.write(intToBytes(crc.value.toInt()))
    }

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value ushr 24 and 0xFF).toByte(),
        (value ushr 16 and 0xFF).toByte(),
        (value ushr 8 and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )
}
