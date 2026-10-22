package com.example.picturegrid.feature.gallery.impl.presentation.ui

import com.example.picturegrid.core.images.NotImageContentException

/**
 * Причина, по которой ячейка с изображением не может показать превью.
 */
internal enum class ImageCellFailure {

    /**
     * Ресурс заведомо не является изображением.
     *
     * Повторная загрузка не поможет: содержимое ответа не изменится, поэтому
     * ячейка показывает заглушку без действия «Повторить».
     */
    NotImage,

    /**
     * Ошибка загрузки: сеть недоступна, ответ неуспешный или данные не
     * декодируются. Доступен повтор.
     */
    LoadFailed,
}

/**
 * Классификация ошибки загрузки превью.
 *
 * @return причина отказа или `null`, если ошибки нет.
 */
internal fun classifyImageCellFailure(throwable: Throwable?): ImageCellFailure? = when (throwable) {
    null -> null
    is NotImageContentException -> ImageCellFailure.NotImage
    else -> ImageCellFailure.LoadFailed
}
