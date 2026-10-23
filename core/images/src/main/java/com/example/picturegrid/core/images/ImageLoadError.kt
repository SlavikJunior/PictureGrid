package com.example.picturegrid.core.images

import com.example.picturegrid.core.network.NetworkError

/**
 * Причина, по которой оригинал изображения недоступен.
 *
 * Тип описывает результат загрузки для вызывающего кода и не содержит
 * текста: формулировку для пользователя выбирает UI (INV-14, INV-20).
 */
sealed interface ImageLoadError {

    /** Сеть не отдала файл. Причина сохраняется, чтобы вызывающий код
     *  мог решить, показывать ли повтор. */
    data class Network(val cause: NetworkError) : ImageLoadError

    /** Сервер ответил содержимым, которое не является изображением. */
    data object NotAnImage : ImageLoadError

    /** Данные не декодируются как изображение: файл повреждён. */
    data object Corrupted : ImageLoadError

    /** Оригинал не удалось сохранить в локальное хранилище. */
    data object Storage : ImageLoadError
}
