package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkResult
import com.example.picturegrid.core.network.isRetryable
import com.example.picturegrid.core.network.toUserMessage
import com.example.picturegrid.feature.gallery.api.GalleryError
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
) : GalleryRepository {

    private val refreshLock = Mutex()
    private var inFlight: Deferred<GalleryRefreshResult>? = null

    override suspend fun getCached(): List<GalleryItem>? = withContext(ioDispatcher) {
        storage.read()?.let { parser.parse(it) }
    }

    /**
     * Загружает список из сети и сохраняет его.
     *
     * Параллельные вызовы не порождают параллельных запросов: все
     * ожидающие получают результат одной загрузки (single-flight). Загрузка
     * выполняется в собственном scope репозитория, поэтому отмена одного
     * вызывающего не прерывает общую работу.
     */
    override suspend fun refresh(): GalleryRefreshResult {
        val deferred = refreshLock.withLock {
            inFlight?.takeIf { it.isActive }
                ?: scope.async { fetchAndStore() }.also { inFlight = it }
        }
        return deferred.await()
    }

    private suspend fun fetchAndStore(): GalleryRefreshResult = withContext(ioDispatcher) {
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
                    GalleryError.Network(result.error.toUserMessage(), result.error.isRetryable()),
                    cached,
                )
            }
        }
    }

    companion object {
        const val DEFAULT_URL = "https://it-link.ru/test/images.txt"
    }
}
