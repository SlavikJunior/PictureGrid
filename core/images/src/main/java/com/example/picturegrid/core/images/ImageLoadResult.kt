package com.example.picturegrid.core.images

import java.io.File

/**
 * Результат загрузки оригинала.
 *
 * Типизированный результат вместо исключения: вызывающий код обязан
 * разобрать причину отказа, а не разбирать текст сообщения (INV-20).
 */
sealed interface ImageLoadResult {

    /** Оригинал доступен в [file]. */
    data class Success(val file: File) : ImageLoadResult

    /** Оригинал недоступен по причине [error]. */
    data class Failure(val error: ImageLoadError) : ImageLoadResult
}
