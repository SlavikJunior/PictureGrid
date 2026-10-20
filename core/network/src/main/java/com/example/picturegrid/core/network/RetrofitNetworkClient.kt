package com.example.picturegrid.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Реализация [NetworkClient] поверх Retrofit/OkHttp.
 *
 * Клиент и Retrofit создаются один раз на экземпляр [RetrofitNetworkClient];
 * для каждого запроса новые объекты не создаются. Публичный контракт
 * [NetworkClient] сохраняется, а детали HTTP-стека скрыты от потребителей.
 *
 * Тело каждого ответа читается с ограничением объёма [NetworkLimits]:
 * ограничение применяется к фактически прочитанным байтам и работает даже
 * при отсутствующем или недостоверном `Content-Length`.
 */
class RetrofitNetworkClient(
    okHttpClient: OkHttpClient = defaultOkHttpClient(),
    baseUrl: String = "https://example.com/",
    private val maxTextBytes: Long = NetworkLimits.MAX_TEXT_BYTES,
    private val maxBinaryBytes: Long = NetworkLimits.MAX_BINARY_BYTES,
) : NetworkClient {

    private val api: NetworkApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(NetworkApi::class.java)

    override suspend fun fetchText(url: String): NetworkResult<String> =
        request(url, maxTextBytes) { bytes, contentType ->
            val text = String(bytes, contentType?.charset() ?: Charsets.UTF_8)
            if (text.isBlank()) {
                NetworkResult.Failure(NetworkError.EmptyBody)
            } else {
                NetworkResult.Success(text)
            }
        }

    override suspend fun fetchBytes(url: String): NetworkResult<BinaryResponse> =
        request(url, maxBinaryBytes) { bytes, contentType ->
            if (bytes.isEmpty()) {
                NetworkResult.Failure(NetworkError.EmptyBody)
            } else {
                NetworkResult.Success(BinaryResponse(bytes, contentType?.toString()))
            }
        }

    private suspend fun <T> request(
        url: String,
        maxBytes: Long,
        read: (bytes: ByteArray, contentType: MediaType?) -> NetworkResult<T>,
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = api.request(url)
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                return@withContext NetworkResult.Failure(NetworkError.Http(response.code()))
            }
            val body = response.body()
                ?: run {
                    response.errorBody()?.close()
                    return@withContext NetworkResult.Failure(NetworkError.EmptyBody)
                }
            body.use {
                val contentType = it.contentType()
                val bytes = readLimited(it, maxBytes)
                    ?: return@withContext NetworkResult.Failure(NetworkError.TooLarge(maxBytes))
                read(bytes, contentType)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: ForbiddenDestinationException) {
            NetworkResult.Failure(NetworkError.ForbiddenDestination)
        } catch (e: SocketTimeoutException) {
            NetworkResult.Failure(NetworkError.Timeout)
        } catch (e: IOException) {
            NetworkResult.Failure(NetworkError.Io)
        }
    }

    companion object {
        /**
         * Читает тело ответа, не более [maxBytes] байт.
         *
         * Ограничение считается по фактически прочитанным байтам, поэтому
         * не зависит от заголовка `Content-Length`: ответ без заголовка и
         * ответ с заниженным заголовком ограничиваются одинаково.
         *
         * @return байты тела либо `null`, если тело превысило лимит.
         */
        internal fun readLimited(body: ResponseBody, maxBytes: Long): ByteArray? {
            val source = body.source()
            val limit = maxBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            var result = ByteArray(minOf(limit, INITIAL_CAPACITY))
            var size = 0
            val chunk = ByteArray(minOf(limit, READ_CHUNK_BYTES).coerceAtLeast(1))
            while (true) {
                val read = source.read(chunk, 0, chunk.size)
                if (read == -1) break
                if (size + read > limit) return null
                if (size + read > result.size) {
                    val grown = minOf(maxOf(result.size * 2, size + read), limit)
                    result = result.copyOf(grown)
                }
                System.arraycopy(chunk, 0, result, size, read)
                size += read
            }
            return if (size == result.size) result else result.copyOf(size)
        }

        private const val READ_CHUNK_BYTES = 64 * 1024
        private const val INITIAL_CAPACITY = 8 * 1024

        /**
         * Клиент по умолчанию: таймауты и политика сетевых назначений.
         *
         * Политика включает проверку адреса назначения и контролируемую
         * обработку редиректов; см. [applyNetworkPolicy].
         */
        fun defaultOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .applyNetworkPolicy()
                .build()
    }
}
