package com.example.picturegrid.core.images

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Проверки ограничения объёма ответа, загружаемого Coil (PG-SEC-002).
 *
 * Ограничение применяется к потоку ответа, поэтому не зависит от
 * заголовка `Content-Length`.
 */
class ResponseSizeLimitInterceptorTest {

    private lateinit var server: MockWebServer

    private val limit = 1_024L

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ResponseSizeLimitInterceptor(limit))
        .build()

    private fun bodyOf(size: Int): Buffer = Buffer().write(ByteArray(size) { 0x41 })

    @Test
    fun `body below the limit is read completely`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(bodyOf(100)))
        val response = client().newCall(Request.Builder().url(server.url("/img.png")).build()).execute()
        response.use {
            assertEquals(100, it.body!!.bytes().size)
        }
    }

    @Test
    fun `body exactly at the limit is read completely`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(bodyOf(limit.toInt())))
        val response = client().newCall(Request.Builder().url(server.url("/img.png")).build()).execute()
        response.use {
            assertEquals(limit.toInt(), it.body!!.bytes().size)
        }
    }

    @Test
    fun `body above the limit is interrupted`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(bodyOf((limit + 100).toInt())))
        val response = client().newCall(Request.Builder().url(server.url("/img.png")).build()).execute()
        response.use {
            try {
                it.body!!.bytes()
                fail("Ожидалась ошибка превышения лимита")
            } catch (e: IOException) {
                assertTrue(
                    "сообщение должно описывать превышение лимита: ${e.message}",
                    e.message?.contains("limit") == true,
                )
            }
        }
    }

    @Test
    fun `body above the limit without content length is interrupted`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setChunkedBody(bodyOf((limit + 100).toInt()), 128),
        )
        val response = client().newCall(Request.Builder().url(server.url("/img.png")).build()).execute()
        response.use {
            try {
                it.body!!.bytes()
                fail("Ожидалась ошибка превышения лимита")
            } catch (e: IOException) {
                assertTrue(e.message?.contains("limit") == true)
            }
        }
    }
}
