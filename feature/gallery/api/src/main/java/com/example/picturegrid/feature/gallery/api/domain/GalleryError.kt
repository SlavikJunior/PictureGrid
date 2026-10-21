package com.example.picturegrid.feature.gallery.api.domain

sealed interface GalleryError {
    val retryable: Boolean
    data class Network(val message: String, override val retryable: Boolean) : GalleryError
    data class Storage(val message: String, override val retryable: Boolean = false) : GalleryError
}
