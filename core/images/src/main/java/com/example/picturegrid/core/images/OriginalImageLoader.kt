package com.example.picturegrid.core.images

import android.graphics.BitmapFactory
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
 * - Содержимое валидируется как изображение (BitmapFactory + Content-Type),
 *   и только после этого атомарно публикуется в хранилище.
 * - Ошибки сети/ФС и повреждённые данные возвращаются как [Result.failure],
 *   исключение не пробрасывается.
 */
class OriginalImageLoader(
    private val networkClient: NetworkClient,
    private val storage: ImageStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val imageValidator: (ByteArray) -> Boolean = ::defaultImageValidator,
) {

    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun load(url: String): Result<File> = withContext(ioDispatcher) {
        val lock = locks.getOrPut(url) { Mutex() }
        lock.withLock {
            storage.cachedFile(url)?.let { return@withLock Result.success(it) }

            when (val result = networkClient.fetchBytes(url)) {
                is NetworkResult.Success -> {
                    val response: BinaryResponse = result.data
                    if (!isAcceptableContentType(response.contentType)) {
                        return@withLock Result.failure(IllegalArgumentException("Not an image: ${response.contentType}"))
                    }
                    if (!isValidImage(response.bytes)) {
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

    private fun isValidImage(bytes: ByteArray): Boolean = imageValidator(bytes)

    private fun isAcceptableContentType(contentType: String?): Boolean {
        if (contentType == null) return true // полагаемся на валидацию байтов
        val lower = contentType.lowercase()
        if (lower.startsWith("text/")) return false
        if (lower.startsWith("image/")) return true
        // application/octet-stream и прочее — проверяем по байтам.
        return true
    }

    private fun NetworkError.toMessage(): String = when (this) {
        NetworkError.Io -> "Network I/O error"
        NetworkError.Timeout -> "Network timeout"
        NetworkError.EmptyBody -> "Empty response body"
        is NetworkError.Http -> "HTTP error $code"
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

private fun defaultImageValidator(bytes: ByteArray): Boolean =
    runCatching { android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) != null }
        .getOrDefault(false)
