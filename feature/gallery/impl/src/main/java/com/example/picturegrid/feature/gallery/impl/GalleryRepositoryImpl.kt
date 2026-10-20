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
import java.io.IOException

/**
 * Реализация [GalleryRepository], оркестрирующая сеть, парсер и хранилище.
 *
 * [getCached] читает сохранённый текст без ожидания сети и возвращает
 * `null`, если кэша нет или он непригоден. [refresh] загружает текст,
 * разбирает его и сохраняет: сохранённый ранее список не затирается, если
 * новая запись не удалась. При сетевой или ошибке записи возвращается
 * ошибка вместе с ранее сохранённым списком, если он доступен.
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
        val cached = storage.read()?.let { parser.parse(it) }
        when (val result = networkClient.fetchText(url)) {
            is NetworkResult.Success -> {
                val items = parser.parse(result.data)
                try {
                    storage.write(result.data)
                } catch (e: IOException) {
                    return@withContext GalleryRefreshResult.Failure(
                        GalleryError.Storage("Не удалось сохранить список", retryable = false),
                        cached,
                    )
                }
                GalleryRefreshResult.Success(items)
            }
            is NetworkResult.Failure -> {
                GalleryRefreshResult.Failure(
                    GalleryError.Network(result.error.toMessage(), result.error.isRetryable()),
                    cached,
                )
            }
        }
    }

    private fun NetworkError.toMessage(): String = when (this) {
        NetworkError.Io -> "Network I/O error"
        NetworkError.Timeout -> "Network timeout"
        NetworkError.EmptyBody -> "Empty response body"
        is NetworkError.Http -> "HTTP error $code"
        is NetworkError.TooLarge -> "Response is too large"
    }

    companion object {
        const val DEFAULT_URL = "https://it-link.ru/test/images.txt"
    }
}
