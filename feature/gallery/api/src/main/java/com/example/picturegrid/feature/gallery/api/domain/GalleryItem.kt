package com.example.picturegrid.feature.gallery.api.domain

sealed interface GalleryItem {
    val id: String
    val source: String

    data class Image(
        override val id: String,
        override val source: String,
    ) : GalleryItem

    data class Placeholder(
        override val id: String,
        override val source: String,
        val reason: PlaceholderReason,
    ) : GalleryItem
}

enum class PlaceholderReason {
    NotUrl,
    NotImage,
    LoadFailed,
}
