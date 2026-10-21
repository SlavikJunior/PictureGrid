package com.example.picturegrid.feature.gallery.api.domain

interface GalleryRepository {
    suspend fun getCached(): List<GalleryItem>?
    suspend fun refresh(): GalleryRefreshResult
}
