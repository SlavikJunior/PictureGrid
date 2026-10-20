package com.example.picturegrid.core.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetAddress

/**
 * Сквозная проверка того, что [SafeDns] применяется при установлении
 * соединения.
 *
 * Проверяется не сам класс [SafeDns] (это делает [SafeDnsTest]), а факт
 * подключения production-клиента: OkHttp обязан обратиться к DNS-фильтру и
 * отказаться от соединения, если фильтр отклонил адрес.
 *
 * Сеть не используется: имя разрешается подставным [Dns], а сервером служит
 * локальный `MockWebServer`.
 */
class SafeDnsEndToEndTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `hostname resolving to a forbidden address is rejected before connecting`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("image"))

        val client = client(PublicNetworkDestinationPolicy, loopbackDns())
        val request = Request.Builder()
            .url("http://images.example.test:${server.port}/photo.png")
            .build()

        val error = runCatching { client.newCall(request).execute() }.exceptionOrNull()

        assertNotNull("ожидалась ошибка политики назначения", error)
        assertTrue(
            "ожидалась ошибка политики, получено ${error?.javaClass?.name}: ${error?.message}",
            error is ForbiddenDestinationException,
        )
        assertEquals("соединение не должно устанавливаться", 0, server.requestCount)
    }

    @Test
    fun `the same setup completes the request when the policy permits the address`() {
        // Контрольная проверка: тестовый сетевой путь действительно способен
        // выполнить запрос, поэтому нулевое число запросов в предыдущем тесте
        // означает отказ политики, а не недоступность сервера.
        server.enqueue(MockResponse().setResponseCode(200).setBody("image"))

        val client = client(PermissivePolicy, loopbackDns())
        val request = Request.Builder()
            .url("http://images.example.test:${server.port}/photo.png")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }
        assertEquals(1, server.requestCount)
    }

    private fun client(policy: NetworkDestinationPolicy, dns: Dns): OkHttpClient =
        OkHttpClient.Builder()
            .applyNetworkPolicy(policy)
            .dns(SafeDns(policy, dns))
            .build()

    /**
     * Все имена разрешаются в loopback.
     *
     * Loopback выбран потому, что он заведомо не является публичным
     * назначением, и проверка не зависит от внешней сети.
     */
    private fun loopbackDns(): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            listOf(InetAddress.getByName("127.0.0.1"))
    }

    /**
     * Политика, разрешающая любые адреса.
     *
     * Используется только в контрольной проверке, чтобы убедиться, что
     * тестовый сетевой путь работает. Production-политика не изменяется.
     */
    private object PermissivePolicy : NetworkDestinationPolicy {
        override fun isAllowed(url: String): Boolean = true
        override fun isAllowedAddress(address: InetAddress): Boolean = true
    }
}
