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
 * `null`, если кэша нет или он повреждён. [refresh] загружает текст,
 * разбирает его и только затем атомарно сохраняет: отклонённый по
 * ограничениям формата список не затирает ранее сохранённые данные.
 * При сетевой или ошибке записи возвращается ошибка вместе с ранее
 * сохранённым списком, если он доступен.
 */
class GalleryRepositoryImpl(
    private val networkClient: NetworkClient,
    private val storage: GalleryListStorage,
    private val parser: GalleryListParser = GalleryListParser,
    private val url: String = DEFAULT_URL,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GalleryRepository {

    override suspend fun getCached(): List<GalleryItem>? = withContext(ioDispatcher) {
        cachedItems(storage.read())
    }

    override suspend fun refresh(): GalleryRefreshResult = withContext(ioDispatcher) {
        val cache = storage.read()
        when (val result = networkClient.fetchText(url)) {
            is NetworkResult.Success -> {
                when (val parsed = parser.parse(result.data)) {
                    is GalleryListParseResult.Parsed -> {
                        try {
                            storage.write(result.data)
                        } catch (e: IOException) {
                            return@withContext GalleryRefreshResult.Failure(
                                GalleryError.Storage("Не удалось сохранить список", retryable = false),
                                cachedItems(cache),
                            )
                        }
                        GalleryRefreshResult.Success(parsed.items)
                    }
                    is GalleryListParseResult.Rejected -> {
                        // Список нарушает ограничения формата: сохранять его
                        // нельзя, иначе он затрёт последний корректный кэш.
                        GalleryRefreshResult.Failure(
                            GalleryError.Storage("Список изображений нарушает формат", retryable = false),
                            cachedItems(cache),
                        )
                    }
                }
            }
            is NetworkResult.Failure -> {
                val error = if (cache is GalleryListCache.Damaged) {
                    GalleryError.Cache("Сохранённый список повреждён")
                } else {
                    GalleryError.Network(result.error.toMessage(), result.error.isRetryable())
                }
                GalleryRefreshResult.Failure(error, cachedItems(cache))
            }
        }
    }

    private fun cachedItems(cache: GalleryListCache): List<GalleryItem>? = when (cache) {
        is GalleryListCache.Available -> (parser.parse(cache.content) as? GalleryListParseResult.Parsed)?.items
        GalleryListCache.Absent, GalleryListCache.Damaged -> null
    }

    private fun NetworkError.toMessage(): String = when (this) {
        NetworkError.Io -> "Network I/O error"
        NetworkError.Timeout -> "Network timeout"
        NetworkError.EmptyBody -> "Empty response body"
        is NetworkError.Http -> "HTTP error $code"
        is NetworkError.TooLarge -> "Response is too large"
        NetworkError.ForbiddenDestination -> "Destination is not allowed by the network policy"
    }

    companion object {
        const val DEFAULT_URL = "https://it-link.ru/test/images.txt"
    }
}
