package com.example.picturegrid.feature.viewer.api.domain

/**
 * Причина, по которой оригинал изображения не показан.
 *
 * Контракт экрана просмотра: текст для пользователя выбирается в UI по типу
 * ошибки (INV-14), а решение о повторе принимается по [retryable] (INV-20).
 */
public sealed interface ViewerError {

    /** Допустим ли повтор загрузки без изменения входных данных. */
    public val retryable: Boolean

    /** Оригинал не скачался: сеть недоступна или сервер ответил ошибкой. */
    public data object Network : ViewerError {
        override val retryable: Boolean = true
    }

    /** Сервер ответил содержимым, которое не является изображением. */
    public data object NotAnImage : ViewerError {
        override val retryable: Boolean = false
    }

    /** Данные не декодируются как изображение. */
    public data object Corrupted : ViewerError {
        override val retryable: Boolean = false
    }

    /** Оригинал не удалось сохранить в локальное хранилище. */
    public data object Storage : ViewerError {
        override val retryable: Boolean = true
    }
}
