package com.example.picturegrid.core.images

import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.picturegrid.core.network.NetworkDestinationPolicy
import com.example.picturegrid.core.network.PublicNetworkDestinationPolicy
import com.example.picturegrid.core.network.RetrofitNetworkClient
import com.example.picturegrid.core.network.applyNetworkPolicy
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.net.InetAddress

/**
 * Проверки применения политики сетевых назначений в обоих путях загрузки:
 * превью через Coil и оригинал через [OriginalImageLoader].
 *
 * Клиент Coil и клиент сетевого слоя — разные экземпляры OkHttpClient,
 * поэтому политика проверяется для каждого пути отдельно.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageNetworkPolicyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer

    private val context = org.robolectric.RuntimeEnvironment.getApplication()

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
    fun `coil does not request a forbidden destination`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("image"))

        val loader = coilLoader(PublicNetworkDestinationPolicy)
        val request = ImageRequest.Builder(context).data("http://192.168.1.1/photo.png").build()

        val result = loader.execute(request)

        assertTrue("запрещённый адрес не должен загружаться", result is coil3.request.ErrorResult)
        assertEquals("к серверу обращения не было", 0, server.requestCount)
    }

    @Test
    fun `coil loads an allowed destination`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(okio.Buffer().write(VALID_PNG)))

        val loader = coilLoader(OnlyPortPolicy(server.port))
        val request = ImageRequest.Builder(context).data(server.url("/photo.png").toString()).build()

        val result = loader.execute(request)

        val cause = (result as? coil3.request.ErrorResult)?.throwable
        assertTrue(
            "ожидалась успешная загрузка, получено ${cause?.javaClass?.name}: ${cause?.message}",
            result is coil3.request.SuccessResult,
        )
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `coil blocks a redirect to a forbidden destination before the request`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(302).setHeader("Location", "http://192.168.1.1/secret.png"),
        )

        val loader = coilLoader(OnlyPortPolicy(server.port))
        val request = ImageRequest.Builder(context).data(server.url("/photo.png").toString()).build()

        val result = loader.execute(request)

        assertTrue("редирект на запрещённый адрес должен быть отклонён", result is coil3.request.ErrorResult)
        assertEquals("запрещённая цель не должна запрашиваться", 1, server.requestCount)
    }

    @Test
    fun `original image loader rejects a forbidden destination`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("image"))

        val client = RetrofitNetworkClient(
            okHttpClient = OkHttpClient.Builder()
                .applyNetworkPolicy(PublicNetworkDestinationPolicy)
                .build(),
            baseUrl = server.url("/").toString(),
        )
        val storage = ImageStorageImpl(tempFolder.newFolder("originals"))
        val loader = OriginalImageLoader(client, storage)

        val result = loader.load("http://169.254.169.254/latest/meta-data/")

        assertTrue("запрещённый адрес не должен загружаться", result.isFailure)
        assertEquals("к серверу обращения не было", 0, server.requestCount)
        assertEquals(null, storage.cachedFile("http://169.254.169.254/latest/meta-data/"))
    }

    @Test
    fun `original image loader loads an allowed destination`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(okio.Buffer().write(ByteArray(64) { 0x41 }))
                .addHeader("Content-Type", "image/png"),
        )

        val client = RetrofitNetworkClient(
            okHttpClient = OkHttpClient.Builder()
                .applyNetworkPolicy(OnlyPortPolicy(server.port))
                .build(),
            baseUrl = server.url("/").toString(),
        )
        val storage = ImageStorageImpl(tempFolder.newFolder("originals"))
        val loader = OriginalImageLoader(
            client,
            storage,
            imageChecker = ImageContentChecker { ImageContent.VALID },
        )

        val result = loader.load(server.url("/photo.png").toString())

        assertNotNull(result.getOrNull())
        assertTrue("оригинал должен быть сохранён", result.getOrNull()!!.exists())
        assertEquals(1, server.requestCount)
    }

    private fun coilLoader(policy: NetworkDestinationPolicy): ImageLoader {
        val client = OkHttpClient.Builder().applyNetworkPolicy(policy).build()
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()
    }

    private companion object {
        /** Валидный PNG 1×1: тест проверяет загрузку, а не декодирование. */
        val VALID_PNG: ByteArray = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAACklEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg=="
        )
    }

    /**
     * Политика, разрешающая соединения только с указанным портом.
     *
     * MockWebServer всегда слушает loopback, который production-политика
     * блокирует; ограничение по порту позволяет проверять разрешённый путь,
     * сохраняя запрет на переходы на другие адреса.
     */
    private class OnlyPortPolicy(private val allowedPort: Int) : NetworkDestinationPolicy {
        override fun isAllowed(url: String): Boolean =
            runCatching { url.toHttpUrlOrNull()?.port }.getOrNull() == allowedPort

        override fun isAllowedAddress(address: InetAddress): Boolean = true
    }
}
