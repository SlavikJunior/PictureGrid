package com.example.picturegrid.feature.gallery.api

import com.example.picturegrid.feature.gallery.api.domain.GalleryItem

/**
 * Выбор изображения для просмотра.
 *
 * Список нужен просмотрщику для перелистывания: позиция выбранного
 * изображения определяет, с какого изображения начинается просмотр.
 */
public data class ImageSelection(
    public val urls: List<String>,
    public val index: Int,
)

/**
 * URL изображений галереи и позиция выбранного.
 *
 * В перелистывании участвуют только изображения: заглушки не являются
 * изображениями, листать их нельзя. Если выбранного элемента в списке
 * нет, берётся первое изображение.
 */
public fun selection(items: List<GalleryItem>, selectedId: String): ImageSelection {
    val images = items.filterIsInstance<GalleryItem.Image>()
    val index = images.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    return ImageSelection(urls = images.map { it.source }, index = index)
}
