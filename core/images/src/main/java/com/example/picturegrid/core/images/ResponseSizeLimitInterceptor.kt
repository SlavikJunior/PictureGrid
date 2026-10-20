package com.example.picturegrid.core.images

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException

/**
 * Ограничивает объём тела сетевого ответа.
 *
 * Ограничение применяется к фактически прочитанным байтам, поэтому не
 * зависит от `Content-Length`: ответ без заголовка и ответ с заниженным
 * заголовком отсекаются одинаково. При превышении чтение прерывается
 * исключением, а неполные данные не попадают в кэш Coil.
 */
internal class ResponseSizeLimitInterceptor(
    private val maxBytes: Long,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        return response.newBuilder()
            .body(LimitedResponseBody(body, maxBytes))
            .build()
    }
}

/**
 * Тело ответа, из которого нельзя прочитать больше [maxBytes] байт.
 */
internal class LimitedResponseBody(
    private val delegate: ResponseBody,
    private val maxBytes: Long,
) : ResponseBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun source(): BufferedSource = LimitedSource(delegate.source(), maxBytes).buffer()
}

/**
 * Источник, ограничивающий количество прочитанных байт.
 */
internal class LimitedSource(
    source: Source,
    private val maxBytes: Long,
) : ForwardingSource(source) {

    private var readBytes: Long = 0L

    override fun read(sink: Buffer, byteCount: Long): Long {
        if (readBytes >= maxBytes) {
            // Проверяем, остались ли данные: лимит достигнут ровно —
            // это допустимо, лимит превышен — это ошибка.
            val probe = Buffer()
            if (super.read(probe, 1L) > 0L) {
                throw IOException("Response body exceeds the limit of $maxBytes bytes")
            }
            return -1L
        }
        val allowed = minOf(byteCount, maxBytes - readBytes)
        val read = super.read(sink, allowed)
        if (read > 0L) readBytes += read
        return read
    }
}
