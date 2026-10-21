package com.example.picturegrid.core.images

import android.graphics.BitmapFactory

/**
 * Проверка, что полученные по URL данные являются изображением.
 *
 * Вынесено в отдельную границу, чтобы проверка не зависела от Android API:
 * тесты подставляют собственную реализацию, приложение использует
 * [BitmapImageContentChecker].
 */
fun interface ImageContentChecker {

    /** Являются ли [bytes] изображением. */
    fun isImage(bytes: ByteArray): Boolean
}

/**
 * Проверка изображения по его заголовку.
 *
 * Размеры читаются через `inJustDecodeBounds`: пиксели не декодируются и
 * память под полноразмерный bitmap не выделяется. Данные считаются
 * изображением, если в заголовке указаны корректные размеры.
 */
class BitmapImageContentChecker : ImageContentChecker {

    companion object {
        fun isImage(bytes: ByteArray): Boolean = BitmapImageContentChecker().isImage(bytes)
    }

    override fun isImage(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth > 0 && options.outHeight > 0
    }
}
