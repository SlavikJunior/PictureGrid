package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkError
import com.example.picturegrid.core.network.NetworkResult
import com.example.picturegrid.core.network.isRetryable
import com.example.picturegrid.feature.gallery.api.GalleryError
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация [GalleryRepository], оркестрирующая сеть, парсер и хранилище.
 *
 * [getCached] читает сохранённый текст без ожидания сети. [refresh] загружает
 * текст, атомарно сохраняет его и возвращает разобранный список; при сетевой
 * или ошибке записи возвращает ошибку вместе с ранее сохранённым списком.
 */
class GalleryRepositoryImpl(
    private val networkClient: NetworkClient,
    private val storage: GalleryListStorage,
    private val parser: GalleryListParser = GalleryListParser,
    private val url: String = DEFAULT_URL,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GalleryRepository {

    override suspend fun getCached(): List<GalleryItem>? = withContext(ioDispatcher) {
        storage.read()?.let { parser.parse(it) }
    }

    override suspend fun refresh(): GalleryRefreshResult = withContext(ioDispatcher) {
        when (val result = networkClient.fetchText(url)) {
            is NetworkResult.Success -> {
                try {
                    storage.write(result.data)
                } catch (e: Exception) {
                    return@withContext GalleryRefreshResult.Failure(
                        GalleryError.Storage(e.message ?: "Write failed", retryable = false),
                        getCached(),
                    )
                }
                GalleryRefreshResult.Success(parser.parse(result.data))
            }
            is NetworkResult.Failure -> GalleryRefreshResult.Failure(
                GalleryError.Network(result.error.toMessage(), result.error.isRetryable()),
                getCached(),
            )
        }
    }

    private fun NetworkError.toMessage(): String = when (this) {
        NetworkError.Io -> "Network I/O error"
        NetworkError.Timeout -> "Network timeout"
        NetworkError.EmptyBody -> "Empty response body"
        is NetworkError.Http -> "HTTP error $code"
    }

    companion object {
        const val DEFAULT_URL = "https://it-link.ru/test/images.txt"
    }
}
