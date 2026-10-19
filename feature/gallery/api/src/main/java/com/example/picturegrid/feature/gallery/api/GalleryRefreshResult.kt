package com.example.picturegrid.feature.gallery.api

sealed interface GalleryRefreshResult {
    data class Success(val items: List<GalleryItem>) : GalleryRefreshResult

    /**
     * @param cached ранее сохранённый список, если он доступен; null, если его нет.
     */
    data class Failure(
        val error: GalleryError,
        val cached: List<GalleryItem>?,
    ) : GalleryRefreshResult
}
