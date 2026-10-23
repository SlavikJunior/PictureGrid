package com.example.picturegrid.core.images

import com.example.picturegrid.core.network.BinaryResponse
import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
 * - Ошибки сети/ФС и повреждённые данные возвращаются как
 *   [ImageLoadResult.Failure] с причиной; исключение не пробрасывается.
 */
class OriginalImageLoader(
    private val networkClient: NetworkClient,
    private val storage: ImageStorage,
    private val ioDispatcher: CoroutineDispatcher,
    private val imageChecker: ImageContentChecker = BitmapImageContentChecker(),
    private val classifier: ImageContentClassifier = ImageContentClassifier(),
) {

    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun load(url: String): ImageLoadResult = withContext(ioDispatcher) {
        val lock = locks.getOrPut(url) { Mutex() }
        lock.withLock {
            storage.cachedFile(url)?.let { cached ->
                if (classifier.isImageFile(cached)) {
                    return@withLock ImageLoadResult.Success(cached)
                }
                // Сохранённый файл повреждён: он не показывается пользователю,
                // удаляется и загружается заново.
                storage.delete(url)
            }

            when (val result = networkClient.fetchBytes(url)) {
                is NetworkResult.Success -> {
                    val response: BinaryResponse = result.data
                    if (classifier.isNotImageContentType(response.contentType)) {
                        // Значение ответа сервера в причину не попадает:
                        // пользователю нужен класс проблемы, а не тело ответа.
                        return@withLock ImageLoadResult.Failure(ImageLoadError.NotAnImage)
                    }
                    if (!imageChecker.isImage(response.bytes)) {
                        return@withLock ImageLoadResult.Failure(ImageLoadError.Corrupted)
                    }
                    storage.store(url, response.bytes).fold(
                        onSuccess = { file -> ImageLoadResult.Success(file) },
                        onFailure = { ImageLoadResult.Failure(ImageLoadError.Storage) },
                    )
                }
                is NetworkResult.Failure -> {
                    ImageLoadResult.Failure(ImageLoadError.Network(result.error))
                }
            }
        }
    }

}
