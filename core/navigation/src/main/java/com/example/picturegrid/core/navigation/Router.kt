package com.example.picturegrid.core.navigation

interface Router {
    fun navigateToGallery()

    /**
     * Переход к просмотру оригинала.
     *
     * @param urls список изображений для перелистывания.
     * @param index позиция выбранного изображения в списке.
     */
    fun navigateToViewer(urls: List<String>, index: Int)

    fun navigateBack()
}
