package com.example.picturegrid.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Проверки ограничения объёма сетевого ответа (PG-SEC-001).
 *
 * Лимиты задаются небольшими значениями, поэтому тесты не зависят от
 * реального интернета и не требуют больших объёмов памяти.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RetrofitNetworkClientSizeLimitTest {

    private lateinit var server: MockWebServer

    private val textLimit = 1_024L
    private val binaryLimit = 2_048L

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): RetrofitNetworkClient = RetrofitNetworkClient(
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build(),
        baseUrl = server.url("/").toString(),
        maxTextBytes = textLimit,
        maxBinaryBytes = binaryLimit,
    )

    private fun bodyOf(size: Int): Buffer = Buffer().write(ByteArray(size) { 'a'.code.toByte() })

    @Test
    fun `text below the limit is returned`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(bodyOf(100)))
        val result = client().fetchText(server.url("/list").toString())
        assertTrue("expected success, got $result", result is NetworkResult.Success)
        assertEquals(100, (result as NetworkResult.Success).data.length)
    }

    @Test
    fun `text exactly at the limit is returned`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(bodyOf(textLimit.toInt())))
        val result = client().fetchText(server.url("/list").toString())
        assertTrue("expected success, got $result", result is NetworkResult.Success)
        assertEquals(textLimit.toInt(), (result as NetworkResult.Success).data.length)
    }

    @Test
    fun `text above the limit is rejected`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(bodyOf((textLimit + 1).toInt())))
        val result = client().fetchText(server.url("/list").toString())
        assertTrue("expected failure, got $result", result is NetworkResult.Failure)
        assertEquals(NetworkError.TooLarge(textLimit), (result as NetworkResult.Failure).error)
    }

    @Test
    fun `text above the limit without content length is rejected`() = runTest {
        // Chunked-ответ не содержит Content-Length.
        server.enqueue(
            MockResponse().setResponseCode(200).setChunkedBody(bodyOf((textLimit + 100).toInt()), 128),
        )
        val result = client().fetchText(server.url("/list").toString())
        assertTrue("expected failure, got $result", result is NetworkResult.Failure)
        assertEquals(NetworkError.TooLarge(textLimit), (result as NetworkResult.Failure).error)
    }

    @Test
    fun `text below the limit without content length is returned`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setChunkedBody(bodyOf(100), 32))
        val result = client().fetchText(server.url("/list").toString())
        assertTrue("expected success, got $result", result is NetworkResult.Success)
        assertEquals(100, (result as NetworkResult.Success).data.length)
    }

    @Test
    fun `truncated response results in io error instead of hanging`() = runTest {
        // Сервер обещает больше данных, чем отправляет, и обрывает соединение
        // в середине тела: клиент не должен ни зависнуть, ни получить
        // частичные данные как успешный результат.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(bodyOf(1_000))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )
        val result = client().fetchText(server.url("/list").toString())
        assertTrue("expected failure, got $result", result is NetworkResult.Failure)
        assertEquals(NetworkError.Io, (result as NetworkResult.Failure).error)
    }

    @Test
    fun `binary body below the limit is returned`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(bodyOf(100))
                .addHeader("Content-Type", "image/png"),
        )
        val result = client().fetchBytes(server.url("/img.png").toString())
        assertTrue("expected success, got $result", result is NetworkResult.Success)
        assertEquals(100, (result as NetworkResult.Success).data.bytes.size)
    }

    @Test
    fun `binary body exactly at the limit is returned`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(bodyOf(binaryLimit.toInt()))
                .addHeader("Content-Type", "image/png"),
        )
        val result = client().fetchBytes(server.url("/img.png").toString())
        assertTrue("expected success, got $result", result is NetworkResult.Success)
        assertEquals(binaryLimit.toInt(), (result as NetworkResult.Success).data.bytes.size)
    }

    @Test
    fun `binary body above the limit is rejected`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(bodyOf((binaryLimit + 1).toInt()))
                .addHeader("Content-Type", "image/png"),
        )
        val result = client().fetchBytes(server.url("/img.png").toString())
        assertTrue("expected failure, got $result", result is NetworkResult.Failure)
        assertEquals(NetworkError.TooLarge(binaryLimit), (result as NetworkResult.Failure).error)
    }

    @Test
    fun `oversized response is not retryable`() {
        assertTrue(!NetworkError.TooLarge(textLimit).isRetryable())
    }

    @Test
    fun `empty body still reported as empty`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result = client().fetchText(server.url("/list").toString())
        assertTrue("expected failure, got $result", result is NetworkResult.Failure)
        assertEquals(NetworkError.EmptyBody, (result as NetworkResult.Failure).error)
    }
}
