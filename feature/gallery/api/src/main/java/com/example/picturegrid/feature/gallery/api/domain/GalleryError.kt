package com.example.picturegrid.feature.gallery.api.domain

/**
 * Причина, по которой список изображений недоступен.
 *
 * Текста для пользователя здесь нет: строки живут в ресурсах UI-слоя
 * (INV-14). Состояние экрана хранит сам тип ошибки, поэтому экран может
 * выбрать формулировку и решить, показывать ли повтор (INV-20).
 */
public sealed interface GalleryError {

    /** Допустима ли повторная попытка без действия пользователя. */
    public val retryable: Boolean

    /** Список не получен из сети. */
    public data class Network(
        public val reason: NetworkFailure,
        public val httpCode: Int? = null,
        override val retryable: Boolean,
    ) : GalleryError

    /** Список не удалось сохранить в локальное хранилище. */
    public data class Storage(
        override val retryable: Boolean = false,
    ) : GalleryError
}
