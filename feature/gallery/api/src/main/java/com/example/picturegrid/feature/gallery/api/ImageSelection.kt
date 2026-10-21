package com.example.picturegrid.feature.gallery.api

import com.example.picturegrid.feature.gallery.api.domain.GalleryItem

/**
 * Выбор изображения для просмотра.
 *
 * Список нужен просмотрщику для перелистывания: позиция выбранного
 * изображения определяет, с какого изображения начинается просмотр.
 */
data class ImageSelection(
    val urls: List<String>,
    val index: Int,
)

/**
 * URL изображений галереи и позиция выбранного.
 *
 * В перелистывании участвуют только изображения: заглушки не являются
 * изображениями, листать их нельзя. Если выбранного элемента в списке
 * нет, берётся первое изображение.
 */
fun selection(items: List<GalleryItem>, selectedId: String): ImageSelection {
    val images = items.filterIsInstance<GalleryItem.Image>()
    val index = images.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    return ImageSelection(urls = images.map { it.source }, index = index)
}
