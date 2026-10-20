package com.example.picturegrid.core.network

import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.Connection
import okhttp3.ConnectionPool
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.Proxy
import java.net.ProxySelector
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Проверки безопасной обработки редиректов.
 *
 * Тесты детерминированы и не требуют интернета: адреса назначения
 * синтетические, а цепочка переходов воспроизводится подставным
 * [Interceptor.Chain], который фиксирует выполненные запросы. Это позволяет
 * проверить главное свойство защиты: **запрещённый адрес не запрашивается**.
 */
class NetworkRedirectSafetyTest {

    private val interceptor = NetworkPolicyInterceptor(PublicNetworkDestinationPolicy)

    // Разрешённые переходы.

    @Test
    fun `redirect to allowed url is followed`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/first.png"),
            responses = listOf(
                redirect("https://images.example.com/second.png"),
                ok("https://images.example.com/second.png"),
            ),
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.proceeded.size)
        assertEquals("https://images.example.com/second.png", chain.proceeded.last().url.toString())
    }

    @Test
    fun `relative location is resolved against the current url`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/dir/first.png"),
            responses = listOf(
                redirect("/dir/second.png"),
                ok("https://images.example.com/dir/second.png"),
            ),
        )

        interceptor.intercept(chain)

        assertEquals("https://images.example.com/dir/second.png", chain.proceeded.last().url.toString())
    }

    // Запрещённые назначения: запрос к цели не выполняется.

    @Test
    fun `redirect to loopback ipv4 is blocked before the request`() {
        assertBlocked("http://127.0.0.1/secret")
    }

    @Test
    fun `redirect to loopback ipv6 is blocked before the request`() {
        assertBlocked("http://[::1]/secret")
    }

    @Test
    fun `redirect to private address is blocked before the request`() {
        assertBlocked("http://192.168.1.1/secret")
        assertBlocked("http://10.0.0.5/secret")
        assertBlocked("http://172.16.0.1/secret")
    }

    @Test
    fun `redirect to link local address is blocked before the request`() {
        assertBlocked("http://169.254.169.254/latest/meta-data/")
        assertBlocked("http://[fe80::1]/secret")
    }

    @Test
    fun `redirect to local host name is blocked before the request`() {
        assertBlocked("http://localhost/secret")
        assertBlocked("http://router.local/secret")
    }

    @Test
    fun `chain of allowed redirects followed by forbidden one is blocked`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/1.png"),
            responses = listOf(
                redirect("https://images.example.com/2.png"),
                redirect("https://images.example.com/3.png"),
                redirect("http://127.0.0.1/secret"),
            ),
        )

        val error = runCatching { interceptor.intercept(chain) }.exceptionOrNull()

        assertNotNull("редирект на запрещённый адрес должен быть отклонён", error)
        // Запрещённая цель не запрашивалась: выполнено ровно три запроса.
        assertEquals(3, chain.proceeded.size)
        assertEquals("https://images.example.com/3.png", chain.proceeded.last().url.toString())
    }

    @Test
    fun `https to http downgrade is rejected before the request`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/secure.png"),
            responses = listOf(redirect("http://images.example.com/plain.png")),
        )

        val error = runCatching { interceptor.intercept(chain) }.exceptionOrNull()

        assertNotNull("понижение https -> http должно быть отклонено", error)
        assertEquals(1, chain.proceeded.size)
    }

    // Некорректные ответы с редиректом.

    @Test
    fun `redirect without location header results in error`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/first.png"),
            responses = listOf(noLocation()),
        )

        val error = runCatching { interceptor.intercept(chain) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error is IOException)
        assertEquals(1, chain.proceeded.size)
    }

    @Test
    fun `redirect with invalid location results in error`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/first.png"),
            responses = listOf(redirect("http://[invalid")),
        )

        val error = runCatching { interceptor.intercept(chain) }.exceptionOrNull()

        assertNotNull(error)
        assertEquals(1, chain.proceeded.size)
    }

    @Test
    fun `redirect loop is stopped by the limit`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/loop.png"),
            responses = List(NetworkDestinationPolicy.MAX_REDIRECTS + 2) {
                redirect("https://images.example.com/loop.png")
            },
        )

        val error = runCatching { interceptor.intercept(chain) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(
            "ошибка должна описывать лимит редиректов: ${error?.message}",
            error?.message?.contains("redirect") == true,
        )
        assertEquals(NetworkDestinationPolicy.MAX_REDIRECTS + 1, chain.proceeded.size)
    }

    @Test
    fun `custom redirect limit is respected`() {
        val limited = NetworkPolicyInterceptor(PublicNetworkDestinationPolicy, maxRedirects = 2)
        val chain = RecordingChain(
            request = request("https://images.example.com/loop.png"),
            responses = List(6) { redirect("https://images.example.com/loop.png") },
        )

        val error = runCatching { limited.intercept(chain) }.exceptionOrNull()

        assertNotNull(error)
        assertEquals(3, chain.proceeded.size)
    }

    // Заголовки при смене origin.

    @Test
    fun `authorization header is not carried to another origin`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/first.png", authorization = "Bearer secret"),
            responses = listOf(
                redirect("https://other.example.com/second.png"),
                ok("https://other.example.com/second.png"),
            ),
        )

        interceptor.intercept(chain)

        assertNull(chain.proceeded.last().header("Authorization"))
    }

    @Test
    fun `authorization header is kept within the same origin`() {
        val chain = RecordingChain(
            request = request("https://images.example.com/first.png", authorization = "Bearer secret"),
            responses = listOf(
                redirect("https://images.example.com/second.png"),
                ok("https://images.example.com/second.png"),
            ),
        )

        interceptor.intercept(chain)

        assertEquals("Bearer secret", chain.proceeded.last().header("Authorization"))
    }

    // Сквозная проверка на реальном HTTP-сервере.

    @Test
    fun `allowed redirect returns the final body over http`() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/final.png"))
            server.enqueue(MockResponse().setResponseCode(200).setBody("image-bytes"))

            val client = client(OnlyPortPolicy(server.port))
            val response = client.newCall(Request.Builder().url(server.url("/first.png")).build()).execute()
            response.use {
                assertEquals(200, it.code)
                assertEquals("image-bytes", it.body!!.string())
            }
            assertEquals(2, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `redirect to another local port is blocked and target receives no request`() {
        val origin = MockWebServer()
        val target = MockWebServer()
        origin.start()
        target.start()
        try {
            origin.enqueue(MockResponse().setResponseCode(302).setHeader("Location", target.url("/secret").toString()))

            val client = client(OnlyPortPolicy(origin.port))

            val error = runCatching {
                client.newCall(Request.Builder().url(origin.url("/first.png")).build()).execute()
            }.exceptionOrNull()

            assertNotNull("переход на запрещённый адрес должен быть отклонён", error)
            assertTrue(
                "ожидалась ошибка политики, получено ${error!!.javaClass.name}",
                error is ForbiddenDestinationException,
            )
            assertEquals("запрос к запрещённому адресу не должен выполняться", 0, target.requestCount)
            assertEquals("исходный сервер получил только первый запрос", 1, origin.requestCount)
        } finally {
            origin.shutdown()
            target.shutdown()
        }
    }

    // Вспомогательные функции.

    private fun assertBlocked(location: String) {
        val chain = RecordingChain(
            request = request("https://images.example.com/first.png"),
            responses = listOf(redirect(location)),
        )

        val error = runCatching { interceptor.intercept(chain) }.exceptionOrNull()

        assertNotNull("редирект на $location должен быть отклонён", error)
        assertTrue(
            "ожидалась ошибка политики, получено ${error!!.javaClass.name}",
            error is ForbiddenDestinationException,
        )
        assertEquals("запрещённая цель не должна запрашиваться", 1, chain.proceeded.size)
    }

    private fun client(policy: NetworkDestinationPolicy): OkHttpClient =
        OkHttpClient.Builder().applyNetworkPolicy(policy).build()

    private fun request(url: String, authorization: String? = null): Request {
        val builder = Request.Builder().url(url)
        if (authorization != null) builder.header("Authorization", authorization)
        return builder.build()
    }

    private fun redirect(location: String): Response = Response.Builder()
        .request(request("https://images.example.com/redirect"))
        .protocol(Protocol.HTTP_1_1)
        .code(302)
        .message("Found")
        .header("Location", location)
        .body(ResponseBody.EMPTY)
        .build()

    private fun ok(url: String): Response = Response.Builder()
        .request(request(url))
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("body".toResponseBody())
        .build()

    private fun noLocation(): Response = Response.Builder()
        .request(request("https://images.example.com/redirect"))
        .protocol(Protocol.HTTP_1_1)
        .code(302)
        .message("Found")
        .body(ResponseBody.EMPTY)
        .build()

    /**
     * Политика, разрешающая соединения только с указанным портом.
     *
     * Нужна для тестов на MockWebServer: сервер всегда слушает loopback,
     * который production-политика блокирует. Разрешение ограничено портом,
     * поэтому переход на другой адрес остаётся запрещённым.
     */
    private class OnlyPortPolicy(private val allowedPort: Int) : NetworkDestinationPolicy {
        override fun isAllowed(url: String): Boolean =
            runCatching { url.toHttpUrlOrNull()?.port }.getOrNull() == allowedPort

        override fun isAllowedAddress(address: InetAddress): Boolean = true
    }

    /**
     * Подставная цепочка перехватчиков: фиксирует выполненные запросы и
     * возвращает заранее подготовленные ответы.
     */
    private class RecordingChain(
        private val request: Request,
        private val responses: List<Response>,
    ) : Interceptor.Chain {

        val proceeded = mutableListOf<Request>()

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            proceeded += request
            val index = proceeded.size - 1
            if (index >= responses.size) {
                throw IOException("No prepared response for request #$index")
            }
            return responses[index]
        }

        override fun connection(): Connection? = null
        override fun call(): okhttp3.Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override val followSslRedirects: Boolean = true
        override val followRedirects: Boolean = true
        override val dns: Dns = Dns.SYSTEM
        override fun withDns(dns: Dns): Interceptor.Chain = this
        override val socketFactory: SocketFactory = SocketFactory.getDefault()
        override fun withSocketFactory(socketFactory: SocketFactory): Interceptor.Chain = this
        override val retryOnConnectionFailure: Boolean = true
        override fun withRetryOnConnectionFailure(retryOnConnectionFailure: Boolean): Interceptor.Chain = this
        override val authenticator: Authenticator = Authenticator.NONE
        override fun withAuthenticator(authenticator: Authenticator): Interceptor.Chain = this
        override val cookieJar: CookieJar = CookieJar.NO_COOKIES
        override fun withCookieJar(cookieJar: CookieJar): Interceptor.Chain = this
        override val cache: Cache? = null
        override fun withCache(cache: Cache?): Interceptor.Chain = this
        override val proxy: Proxy? = null
        override fun withProxy(proxy: Proxy?): Interceptor.Chain = this
        override val proxySelector: ProxySelector = ProxySelector.getDefault()
        override fun withProxySelector(proxySelector: ProxySelector): Interceptor.Chain = this
        override val proxyAuthenticator: Authenticator = Authenticator.NONE
        override fun withProxyAuthenticator(proxyAuthenticator: Authenticator): Interceptor.Chain = this
        override val sslSocketFactoryOrNull: SSLSocketFactory? = null
        override fun withSslSocketFactory(
            sslSocketFactory: SSLSocketFactory?,
            trustManager: X509TrustManager?,
        ): Interceptor.Chain = this
        override val x509TrustManagerOrNull: X509TrustManager? = null
        override val hostnameVerifier: HostnameVerifier = HostnameVerifier { _, _ -> true }
        override fun withHostnameVerifier(hostnameVerifier: HostnameVerifier): Interceptor.Chain = this
        override val certificatePinner: CertificatePinner = CertificatePinner.DEFAULT
        override fun withCertificatePinner(certificatePinner: CertificatePinner): Interceptor.Chain = this
        override val connectionPool: ConnectionPool = ConnectionPool()
        override fun withConnectionPool(connectionPool: ConnectionPool): Interceptor.Chain = this
        override val eventListener: EventListener = EventListener.NONE
    }
}
