package com.example.picturegrid.core.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Retrofit-интерфейс для выполнения GET-запросов по абсолютному URL.
 *
 * Один метод используется и для текстового списка, и для бинарного
 * содержимого: различие состоит только в способе чтения тела ответа.
 * Абсолютный URL передаётся через [@Url], поэтому query-параметры и путь
 * сохраняются без ручной сборки.
 */
internal interface NetworkApi {
    @GET
    suspend fun request(@Url url: String): Response<ResponseBody>
}
