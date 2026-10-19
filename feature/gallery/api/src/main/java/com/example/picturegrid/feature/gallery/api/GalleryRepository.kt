package com.example.picturegrid.feature.gallery.api

interface GalleryRepository {
    /**
     * Ранее сохранённый список (без сетевого запроса) или null, если его нет.
     */
    suspend fun getCached(): List<GalleryItem>?

    /**
     * Загружает список из сети, сохраняет его атомарно и возвращает результат.
     * При сетевой или ошибке записи сохранённый список не теряется.
     */
    suspend fun refresh(): GalleryRefreshResult
}
