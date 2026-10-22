package com.example.picturegrid.feature.gallery.api.domain

public sealed interface GalleryRefreshResult {
    public data class Success(val items: List<GalleryItem>) : GalleryRefreshResult

    /**
     * @param cached ранее сохранённый список, если он доступен; null, если его нет.
     */
    public data class Failure(
        val error: GalleryError,
        val cached: List<GalleryItem>?,
    ) : GalleryRefreshResult
}
