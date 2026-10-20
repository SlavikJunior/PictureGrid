package com.example.picturegrid.core.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Размеры изображения, прочитанные из заголовка файла.
 */
data class ImageDimensions(val width: Int, val height: Int) {

    /** Полное число пикселей изображения. */
    val pixels: Long get() = width.toLong() * height.toLong()
}

/**
 * Проверка изображения без выделения памяти под полноразмерный bitmap.
 *
 * Порядок проверки:
 * 1. размеры читаются из заголовка файла (`inJustDecodeBounds`) — пиксели
 *    не декодируются, память не выделяется;
 * 2. размеры сверяются с [ImageLimits];
 * 3. декодируется только уменьшенная копия — это подтверждает, что данные
 *    действительно являются изображением, но не требует памяти под
 *    полноразмерный bitmap.
 */
object ImageInspector : ImageContentChecker {

    override fun check(bytes: ByteArray): ImageContent {
        val dimensions = readDimensions(bytes) ?: return ImageContent.NOT_IMAGE
        if (!isAcceptable(dimensions)) return ImageContent.TOO_LARGE
        return if (decodesToImage(bytes, dimensions)) {
            ImageContent.VALID
        } else {
            ImageContent.NOT_IMAGE
        }
    }

    /**
     * Читает размеры изображения из заголовка, не декодируя пиксели.
     *
     * @return размеры или `null`, если данные не являются изображением.
     */
    fun readDimensions(bytes: ByteArray): ImageDimensions? {
        if (bytes.isEmpty()) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return ImageDimensions(options.outWidth, options.outHeight)
    }

    /** Допустимы ли размеры изображения. */
    fun isAcceptable(dimensions: ImageDimensions): Boolean =
        dimensions.width <= ImageLimits.MAX_DIMENSION &&
            dimensions.height <= ImageLimits.MAX_DIMENSION &&
            dimensions.pixels <= ImageLimits.MAX_PIXELS

    /**
     * Проверяет, что данные декодируются как изображение.
     *
     * Декодируется уменьшенная копия: память под полноразмерный bitmap
     * не выделяется, но некорректные данные отбрасываются.
     */
    fun decodesToImage(bytes: ByteArray, dimensions: ImageDimensions): Boolean {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(dimensions)
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return false
        bitmap.recycle()
        return true
    }

    /**
     * Подбирает коэффициент уменьшения (степень двойки) так, чтобы
     * уменьшенная копия не превышала [ImageLimits.SAMPLE_MAX_PIXELS].
     */
    internal fun sampleSizeFor(dimensions: ImageDimensions): Int {
        var sample = 1
        while (dimensions.pixels / (sample.toLong() * sample) > ImageLimits.SAMPLE_MAX_PIXELS) {
            sample *= 2
        }
        return sample
    }
}
