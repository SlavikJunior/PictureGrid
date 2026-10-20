package com.example.picturegrid.core.images

import com.example.picturegrid.core.network.BinaryResponse
import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkError
import com.example.picturegrid.core.network.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Загрузка оригинала изображения по URL с гарантированным сохранением
 * в [ImageStorage].
 *
 * - Сначала проверяет локальную копию и возвращает её без сети.
 * - Повторные параллельные запросы одного URL сериализуются через [Mutex],
 *   чтобы не скачивать один оригинал несколько раз.
 * - Повторно используемый оригинал проверяется на целостность: повреждённый
 *   файл удаляется и загружается заново.
 * - Содержимое проверяется как изображение ([ImageContentChecker]) до
 *   сохранения: размеры читаются из заголовка, полноразмерный bitmap
 *   не декодируется.
 * - Ошибки сети/ФС и повреждённые данные возвращаются как [Result.failure],
 *   исключение не пробрасывается.
 */
class OriginalImageLoader(
    private val networkClient: NetworkClient,
    private val storage: ImageStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val imageChecker: ImageContentChecker = BitmapImageContentChecker,
) {

    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun load(url: String): Result<File> = withContext(ioDispatcher) {
        val lock = locks.getOrPut(url) { Mutex() }
        lock.withLock {
            storage.cachedFile(url)?.let { cached ->
                if (ImageContentClassifier.isImageFile(cached)) {
                    return@withLock Result.success(cached)
                }
                // Сохранённый файл повреждён: он не показывается пользователю,
                // удаляется и загружается заново.
                storage.delete(url)
            }

            when (val result = networkClient.fetchBytes(url)) {
                is NetworkResult.Success -> {
                    val response: BinaryResponse = result.data
                    if (ImageContentClassifier.isNotImageContentType(response.contentType)) {
                        // Текст ошибки не содержит значения ответа сервера:
                        // причина не нужна пользователю и не должна попадать
                        // в тексты ошибок приложения.
                        return@withLock Result.failure(IllegalArgumentException("Not an image"))
                    }
                    if (!imageChecker.isImage(response.bytes)) {
                        return@withLock Result.failure(IllegalArgumentException("Invalid image data"))
                    }
                    storage.store(url, response.bytes)
                }
                is NetworkResult.Failure -> {
                    Result.failure(Exception(result.error.toMessage()))
                }
            }
        }
    }

    private fun NetworkError.toMessage(): String = when (this) {
        NetworkError.Io -> "Network I/O error"
        NetworkError.Timeout -> "Network timeout"
        NetworkError.EmptyBody -> "Empty response body"
        is NetworkError.Http -> "HTTP error $code"
        is NetworkError.TooLarge -> "Response is too large (limit $limitBytes bytes)"
    }
}

object OriginalImageLoaderFactory {
    fun create(context: android.content.Context): OriginalImageLoader {
        val storage = ImageStorageImpl(java.io.File(context.filesDir, "images/originals"))
        return OriginalImageLoader(
            networkClient = com.example.picturegrid.core.network.RetrofitNetworkClient(),
            storage = storage,
        )
    }
}
