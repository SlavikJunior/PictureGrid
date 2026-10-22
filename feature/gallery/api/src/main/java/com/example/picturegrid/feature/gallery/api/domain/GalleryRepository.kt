package com.example.picturegrid.feature.gallery.api.domain

public interface GalleryRepository {
    public suspend fun getCached(): List<GalleryItem>?
    public suspend fun refresh(): GalleryRefreshResult
}
