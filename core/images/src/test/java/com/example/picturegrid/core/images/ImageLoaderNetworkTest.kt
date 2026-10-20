package com.example.picturegrid.core.images

import android.content.Context
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Проверка того, что настроенный `ImageLoader` выполняет сетевые запросы.
 *
 * Сетевой фетчер подключается к Coil автоматически, через
 * `META-INF/services`, поэтому отсутствие явной регистрации не должно мешать
 * загрузке. Robolectric в режиме legacy не декодирует растровые изображения,
 * поэтому проверяется сетевая часть: запрос доходит до сервера, а ошибка
 * результата не является сетевой.
 */
@RunWith(RobolectricTestRunner::class)
class ImageLoaderNetworkTest {

    @get:Rule
    val server = MockWebServer()

    private val validPng = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
    )

    @Test
    fun `preview is requested over the network`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(validPng))
                .addHeader("Content-Type", "image/png"),
        )
        val context: Context = org.robolectric.RuntimeEnvironment.getApplication()
        val loader = ImageLoaderFactory.create(context)

        val request = ImageRequest.Builder(context)
            .data(server.url("/preview.png").toString())
            .size(64)
            .build()
        val result = loader.execute(request)

        assertEquals("превью должно запрашиваться по сети", 1, server.requestCount)
        assertEquals("/preview.png", server.takeRequest().path)
        val error = result as? ErrorResult
        assertTrue("ожидался результат с ошибкой декодирования, получено $result", error != null)
        assertFalse(
            "ошибка не должна быть сетевой: ${error?.throwable}",
            error?.throwable is IOException,
        )
    }
}
