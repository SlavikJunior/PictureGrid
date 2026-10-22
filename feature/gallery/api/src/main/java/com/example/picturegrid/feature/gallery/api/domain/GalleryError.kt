package com.example.picturegrid.feature.gallery.api.domain

public sealed interface GalleryError {
    public val retryable: Boolean
    public data class Network(val message: String, override val retryable: Boolean) : GalleryError
    public data class Storage(val message: String, override val retryable: Boolean = false) : GalleryError
}
