package com.example.picturegrid.feature.gallery.impl

/**
 * Состояние локально сохранённого списка URL.
 *
 * Состояния различаются явно, чтобы повреждённый или пустой файл не
 * выдавался за корректный (в том числе пустой) список.
 */
sealed interface GalleryListCache {

    /** Список ещё ни разу не сохранён. */
    data object Absent : GalleryListCache

    /** Список сохранён и пригоден для использования. */
    data class Available(val content: String) : GalleryListCache

    /**
     * Список существует, но не может быть использован: файл пуст, состоит
     * только из пробелов, превышает допустимый объём или не читается.
     */
    data object Damaged : GalleryListCache
}
