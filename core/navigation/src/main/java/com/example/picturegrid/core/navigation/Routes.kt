package com.example.picturegrid.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object GalleryRoute : NavKey

/**
 * Просмотр оригинала.
 *
 * Список URL нужен для перелистывания изображений, индекс — позиция
 * выбранного изображения в списке.
 */
@Serializable
data class ViewerRoute(val urls: List<String>, val index: Int) : NavKey
