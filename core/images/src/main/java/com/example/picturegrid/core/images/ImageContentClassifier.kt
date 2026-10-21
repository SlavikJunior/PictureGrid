package com.example.picturegrid.core.images

import java.io.File

private const val WEBP_BRAND_OFFSET = 8
private const val BMFF_BRAND_OFFSET = 4
private const val SIGNATURE_PROBE_BYTES = 32

/**
 * Определение того, является ли содержимое ответа изображением.
 *
 * Проверка опирается на два признака: заголовок `Content-Type` и сигнатуру
 * первых байт. Обе проверки дешёвые, не требуют декодирования и не читают
 * тело целиком, поэтому применимы к потоковому ответу.
 *
 * Классификатор отвечает на вопрос «это заведомо не изображение».
 * Отрицательный ответ не гарантирует, что содержимое корректно: битое
 * изображение распознаётся уже при декодировании и считается ошибкой
 * загрузки, а не отсутствием изображения.
 */
class ImageContentClassifier {

    companion object {
        fun isNotImageContentType(contentType: String?): Boolean =
            ImageContentClassifier().isNotImageContentType(contentType)

        fun isImageContentType(contentType: String?): Boolean =
            ImageContentClassifier().isImageContentType(contentType)

        fun isImageSignature(bytes: ByteArray): Boolean =
            ImageContentClassifier().isImageSignature(bytes)

        fun isImageFile(file: File): Boolean =
            ImageContentClassifier().isImageFile(file)
    }

    /**
     * Заведомо не-изображения по заголовку `Content-Type`.
     *
     * Типы `image/` изображением считаются, текстовые типы и перечисленные ниже
     * типы — точно нет. Остальные (`application/octet-stream`, отсутствие
     * заголовка) не определяются по заголовку и проверяются по сигнатуре.
     */
    fun isNotImageContentType(contentType: String?): Boolean {
        val type = normalizeContentType(contentType) ?: return false
        if (type.startsWith("image/")) return false
        if (type.startsWith("text/")) return true
        return type in NON_IMAGE_CONTENT_TYPES
    }

    /** Является ли заголовок `Content-Type` признаком изображения. */
    fun isImageContentType(contentType: String?): Boolean =
        normalizeContentType(contentType)?.startsWith("image/") == true

    /**
     * Является ли содержимое изображением по сигнатуре первых байт.
     *
     * Распознаются JPEG, PNG, GIF, WEBP, BMP, ICO и форматы семейства
     * ISO-BMFF (AVIF, HEIC). Пустой или слишком короткий набор байт
     * изображением не считается.
     */
    fun isImageSignature(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        return when {
            startsWith(bytes, JPEG_SIGNATURE) -> true
            startsWith(bytes, PNG_SIGNATURE) -> true
            startsWith(bytes, GIF_SIGNATURE) -> true
            startsWith(bytes, RIFF_SIGNATURE) -> asciiAt(bytes, WEBP_BRAND_OFFSET, "WEBP")
            startsWith(bytes, BMP_SIGNATURE) -> true
            startsWith(bytes, ICO_SIGNATURE) -> true
            asciiAt(bytes, BMFF_BRAND_OFFSET, "ftyp") -> {
                ISO_BMFF_IMAGE_BRANDS.any { asciiAt(bytes, BMFF_BRAND_OFFSET + 4, it) }
            }
            else -> false
        }
    }

    /**
     * Является ли файл сохранённым изображением.
     *
     * Проверяется сигнатура первых байт: этого достаточно, чтобы отсечь
     * пустой или повреждённый файл, и файл не нужно читать целиком.
     */
    fun isImageFile(file: File): Boolean {
        if (!file.isFile || file.length() == 0L) return false
        return file.inputStream().use { stream ->
            val head = ByteArray(SIGNATURE_PROBE_BYTES)
            var offset = 0
            while (offset < head.size) {
                val read = stream.read(head, offset, head.size - offset)
                if (read <= 0) break
                offset += read
            }
            isImageSignature(head.copyOf(offset))
        }
    }

    private fun normalizeContentType(contentType: String?): String? =
        contentType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }

    private fun startsWith(bytes: ByteArray, prefix: ByteArray): Boolean {
        if (bytes.size < prefix.size) return false
        return prefix.indices.all { bytes[it] == prefix[it] }
    }

    private fun asciiAt(bytes: ByteArray, offset: Int, value: String): Boolean {
        if (bytes.size < offset + value.length) return false
        return value.indices.all { bytes[offset + it] == value[it].code.toByte() }
    }

    private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )
    private val GIF_SIGNATURE = "GIF8".toByteArray(Charsets.US_ASCII)
    private val RIFF_SIGNATURE = "RIFF".toByteArray(Charsets.US_ASCII)
    private val BMP_SIGNATURE = "BM".toByteArray(Charsets.US_ASCII)
    private val ICO_SIGNATURE = byteArrayOf(0x00, 0x00, 0x01, 0x00)

    private val ISO_BMFF_IMAGE_BRANDS = setOf(
        "avif", "avis", "heic", "heix", "heif", "hevc", "mif1", "msf1",
    )

    private val NON_IMAGE_CONTENT_TYPES = setOf(
        "application/json",
        "application/xml",
        "application/javascript",
        "application/xhtml+xml",
        "application/pdf",
    )
}
