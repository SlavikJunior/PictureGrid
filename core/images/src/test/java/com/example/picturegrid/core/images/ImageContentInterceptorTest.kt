package com.example.picturegrid.core.images

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Проверка интерцептора, отбрасывающего ответы, не являющиеся изображениями.
 */
class ImageContentInterceptorTest {

    @get:Rule
    val server = MockWebServer()

    private val client = OkHttpClient.Builder()
        .addInterceptor(ImageContentInterceptor())
        .build()

    private val validPng = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
    )

    @Test
    fun `html response is rejected`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<html><body>not an image</body></html>")
                .addHeader("Content-Type", "text/html; charset=utf-8"),
        )

        val error = runCatching { execute("/html") }.exceptionOrNull()

        assertTrue(
            "ожидалась NotImageContentException, получено $error",
            error is NotImageContentException,
        )
    }

    @Test
    fun `ambiguous content type without image signature is rejected`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("just bytes")
                .addHeader("Content-Type", "application/octet-stream"),
        )

        val error = runCatching { execute("/bytes") }.exceptionOrNull()

        assertTrue(
            "ожидалась NotImageContentException, получено $error",
            error is NotImageContentException,
        )
    }

    @Test
    fun `image response is passed through`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(validPng))
                .addHeader("Content-Type", "image/png"),
        )

        val response = execute("/image.png")

        assertTrue("ожидался успешный ответ, получено ${response.code}", response.isSuccessful)
        response.close()
    }

    @Test
    fun `ambiguous content type with image signature is passed through`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(validPng))
                .addHeader("Content-Type", "application/octet-stream"),
        )

        val response = execute("/octet.png")

        assertTrue("ожидался успешный ответ, получено ${response.code}", response.isSuccessful)
        response.close()
    }

    @Test
    fun `http error is not treated as not an image`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("missing")
                .addHeader("Content-Type", "text/html"),
        )

        val response = execute("/missing")

        assertTrue("ожидался ответ 404, получено ${response.code}", response.code == 404)
        response.close()
    }

    private fun execute(path: String) = client.newCall(
        Request.Builder().url(server.url(path).toString()).build(),
    ).execute()
}
