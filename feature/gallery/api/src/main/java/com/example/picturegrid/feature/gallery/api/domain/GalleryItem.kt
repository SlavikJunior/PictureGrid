package com.example.picturegrid.feature.gallery.api.domain

public sealed interface GalleryItem {
    public val id: String
    public val source: String

    public data class Image(
        override val id: String,
        override val source: String,
    ) : GalleryItem

    public data class Placeholder(
        override val id: String,
        override val source: String,
        val reason: PlaceholderReason,
    ) : GalleryItem
}

public enum class PlaceholderReason {
    NotUrl,
    NotImage,
    LoadFailed,
}
