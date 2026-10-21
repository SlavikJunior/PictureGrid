package com.example.picturegrid.feature.gallery.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.picturegrid.core.navigation.GalleryRoute

/** Регистрирует destination галереи в composition root приложения. */
fun EntryProviderScope<NavKey>.galleryNavEntry(
    onImageClick: (com.example.picturegrid.feature.gallery.api.ImageSelection) -> Unit,
) {
    entry<GalleryRoute> {
        GalleryScreen(
            onImageClick = onImageClick,
        )
    }
}
