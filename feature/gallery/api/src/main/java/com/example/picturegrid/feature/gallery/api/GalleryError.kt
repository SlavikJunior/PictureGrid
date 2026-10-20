package com.example.picturegrid.feature.gallery.api

sealed interface GalleryError {
    val retryable: Boolean
    data class Network(val message: String, override val retryable: Boolean) : GalleryError
    data class Storage(val message: String, override val retryable: Boolean = false) : GalleryError

    /**
     * Локально сохранённый список недоступен или повреждён.
     *
     * Ошибка retryable: список восстанавливается из сети при следующей
     * успешной синхронизации, а сохранённые данные не удаляются.
     */
    data class Cache(val message: String, override val retryable: Boolean = true) : GalleryError
}
