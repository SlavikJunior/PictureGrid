package com.example.picturegrid.feature.gallery.api

/**
 * Модель элемента галереи.
 *
 * На этапе разбора списка различаем только «валидный URL» и «не URL».
 * Классификация «URL не-изображения» и «ошибка загрузки изображения»
 * уточняется на этапе загрузки (Epic 3), но контракт уже предусматривает
 * эти состояния.
 */
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
    /** Строка не является корректным HTTP(S)-URL. */
    NotUrl,

    /** URL корректен, но ресурс не является изображением. */
    NotImage,

    /** Валидный URL изображения, загрузка которого завершилась ошибкой. */
    LoadFailed,
}
