package com.example.picturegrid.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 */
class RetrofitNetworkClient(
    okHttpClient: OkHttpClient = defaultOkHttpClient(),
    baseUrl: String = "https://example.com/",
) : NetworkClient {

    private val api: NetworkApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(NetworkApi::class.java)

    override suspend fun fetchText(url: String): NetworkResult<String> =
        request(url) { body ->
            val text = body.string()
            if (text.isBlank()) {
                NetworkResult.Failure(NetworkError.EmptyBody)
            } else {
                NetworkResult.Success(text)
            }
        }

    override suspend fun fetchBytes(url: String): NetworkResult<BinaryResponse> =
        request(url) { body ->
            val bytes = body.bytes()
            if (bytes.isEmpty()) {
                NetworkResult.Failure(NetworkError.EmptyBody)
            } else {
                NetworkResult.Success(BinaryResponse(bytes, body.contentType()?.toString()))
            }
        }

    private suspend fun <T> request(
        url: String,
        readBody: (ResponseBody) -> NetworkResult<T>,
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
            body.use { readBody(it) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            NetworkResult.Failure(NetworkError.Timeout)
        } catch (e: IOException) {
            NetworkResult.Failure(NetworkError.Io)
        }
    }

    companion object {
        fun defaultOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
    }
}
