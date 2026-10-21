package com.example.picturegrid.core.images

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Ответ сервера заведомо не является изображением.
 *
 * Исключение наследуется от [IOException]: клиент OkHttp передаёт сетевые
 * сбои именно в таком виде, а Coil сохраняет исходную причину ошибки в
 * `ErrorResult.throwable`. Благодаря этому вызывающий код отличает такой
 * ответ от других ошибок загрузки.
 */
class NotImageContentException : IOException("Response content is not an image")

/**
 * Интерцептор, отбрасывающий ответы, которые заведомо не являются
 * изображением.
 *
 * Ответ отбрасывается по заголовку `Content-Type` (текстовые типы и подобные) либо
 * по сигнатуре первых байт, если тип ответа неоднозначен
 * (`application/octet-stream`, отсутствие заголовка). Ошибки HTTP и пустые
 * ответы не трогаются: это ошибки загрузки, а не признак отсутствия
 * изображения, и они требуют действия «Повторить».
 *
 * Тело ответа не читается целиком: проверяются только первые байты.
 */
class ImageContentInterceptor(
    private val classifier: ImageContentClassifier = ImageContentClassifier(),
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful) return response
        if (response.body == null) return response

        val contentType = response.header(CONTENT_TYPE_HEADER)
        if (classifier.isNotImageContentType(contentType)) {
            response.close()
            throw NotImageContentException()
        }
        if (!classifier.isImageContentType(contentType)) {
            val head = response.peekBody(SIGNATURE_PROBE_BYTES).bytes()
            if (!classifier.isImageSignature(head)) {
                response.close()
                throw NotImageContentException()
            }
        }
        return response
    }

    private companion object {
        const val CONTENT_TYPE_HEADER = "Content-Type"
        const val SIGNATURE_PROBE_BYTES = 32L
    }
}
