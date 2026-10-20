package com.example.picturegrid.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class RetrofitNetworkClientTest {

    @get:Rule
    val server = MockWebServer()

    private fun client(
        connectTimeoutMs: Long = 10_000,
        readTimeoutMs: Long = 15_000,
    ): RetrofitNetworkClient = RetrofitNetworkClient(
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build(),
        baseUrl = server.url("/").toString(),
    )

    @Before
    fun setUp() {
        // MockWebServer rule starts server automatically.
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchText returns body on success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("first\nsecond"))
        val result = client().fetchText(server.url("/list").toString())
        assertTrue(result is NetworkResult.Success)
        assertEquals("first\nsecond", (result as NetworkResult.Success).data)
        val recorded = server.takeRequest()
        assertEquals("/list", recorded.path)
    }

    @Test
    fun `fetchBytes returns bytes and content type`() = runTest {
        val payload = byteArrayOf(1, 2, 3, 4)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(okio.Buffer().write(payload))
                .addHeader("Content-Type", "image/png"),
        )
        val result = client().fetchBytes(server.url("/img.png").toString())
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue(payload.contentEquals(data.bytes))
        assertEquals("image/png", data.contentType)
    }

    @Test
    fun `fetchText returns http error for 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = client().fetchText(server.url("/missing").toString())
        assertTrue(result is NetworkResult.Failure)
        assertEquals(NetworkError.Http(404), (result as NetworkResult.Failure).error)
    }

    @Test
    fun `fetchBytes returns http error for 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = client().fetchBytes(server.url("/broken").toString())
        assertTrue(result is NetworkResult.Failure)
        assertEquals(NetworkError.Http(500), (result as NetworkResult.Failure).error)
    }

    @Test
    fun `fetchText returns empty body for blank response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("   "))
        val result = client().fetchText(server.url("/empty").toString())
        assertTrue(result is NetworkResult.Failure)
        assertEquals(NetworkError.EmptyBody, (result as NetworkResult.Failure).error)
    }

    @Test
    fun `fetchBytes returns empty body for empty response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(okio.Buffer()))
        val result = client().fetchBytes(server.url("/empty").toString())
        assertTrue(result is NetworkResult.Failure)
        assertEquals(NetworkError.EmptyBody, (result as NetworkResult.Failure).error)
    }

    @Test
    fun `fetchText returns timeout`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok").setBodyDelay(1, TimeUnit.SECONDS))
        val result = client(readTimeoutMs = 100).fetchText(server.url("/slow").toString())
        assertTrue(result is NetworkResult.Failure)
        assertEquals(NetworkError.Timeout, (result as NetworkResult.Failure).error)
    }

    @Test
    fun `fetchText returns io error when server is unavailable`() = runTest {
        val port = server.port
        server.shutdown()
        val result = RetrofitNetworkClient(
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.MILLISECONDS)
                .readTimeout(100, TimeUnit.MILLISECONDS)
                .build(),
            baseUrl = "http://127.0.0.1:$port/",
        ).fetchText("http://127.0.0.1:$port/down")
        assertTrue(result is NetworkResult.Failure)
        assertEquals(NetworkError.Io, (result as NetworkResult.Failure).error)
    }

    @Test
    fun `fetchText propagates cancellation`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok").setBodyDelay(1, TimeUnit.SECONDS))
        val c = client(readTimeoutMs = 10_000)
        var thrown: Throwable? = null
        val job = launch {
            try {
                c.fetchText(server.url("/cancel").toString())
            } catch (e: Throwable) {
                thrown = e
            }
        }
        testScheduler.runCurrent()
        job.cancel()
        job.join()
        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `absolute url with query parameters is preserved`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        val url = server.url("/search").newBuilder()
            .addQueryParameter("foo", "bar")
            .addQueryParameter("baz", "qux")
            .build()
        val result = client().fetchText(url.toString())
        assertTrue(result is NetworkResult.Success)
        val recorded = server.takeRequest()
        assertEquals("bar", recorded.requestUrl?.queryParameter("foo"))
        assertEquals("qux", recorded.requestUrl?.queryParameter("baz"))
    }
}
