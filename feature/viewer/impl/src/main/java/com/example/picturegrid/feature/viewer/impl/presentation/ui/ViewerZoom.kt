package com.example.picturegrid.feature.viewer.impl.presentation.ui
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStore

/**
 * Правила масштабирования изображения в просмотрщике.
 *
 * Вынесены отдельно от Store, чтобы правила можно было проверить обычными
 * unit-тестами: Store только переносит их в состояние.
 */
internal class ViewerZoom {

    companion object {
        /** Минимальный масштаб: изображение вписано в контейнер целиком. */
        const val MIN_SCALE: Float = 1f

        /** Максимальный масштаб: сильнее приближать бессмысленно. */
        const val MAX_SCALE: Float = 5f

        /** Масштаб при двойном нажатии. */
        const val DOUBLE_TAP_SCALE: Float = 2f

        fun apply(
            zoom: ViewerStore.Zoom,
            zoomChange: Float,
            panX: Float,
            panY: Float,
            centroidX: Float,
            centroidY: Float,
            containerWidth: Float,
            containerHeight: Float,
            imageWidth: Float,
            imageHeight: Float,
        ): ViewerStore.Zoom = ViewerZoom().apply(
            zoom,
            zoomChange,
            panX,
            panY,
            centroidX,
            centroidY,
            containerWidth,
            containerHeight,
            imageWidth,
            imageHeight,
        )

        fun toggle(zoom: ViewerStore.Zoom): ViewerStore.Zoom = ViewerZoom().toggle(zoom)
    }

    /**
     * Применяет жест масштабирования и перетаскивания.
     *
     * Масштаб ограничен диапазоном [MIN_SCALE]..[MAX_SCALE]. Точка под
     * пальцем остаётся на месте: при изменении масштаба смещение
     * пересчитывается относительно центра контейнера.
     *
     * Смещение ограничено размерами вписанного изображения: при масштабе `s`
     * содержимое выходит за границы контейнера на `fitted * (s - 1)`, поэтому
     * половина этого значения — предел сдвига. При возврате к минимальному
     * масштабу смещение сбрасывается: в исходном масштабе сдвигать нечего.
     */
    fun apply(
        zoom: ViewerStore.Zoom,
        zoomChange: Float,
        panX: Float,
        panY: Float,
        centroidX: Float,
        centroidY: Float,
        containerWidth: Float,
        containerHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
    ): ViewerStore.Zoom {
        val oldScale = zoom.scale
        val newScale = (oldScale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        if (newScale <= MIN_SCALE) return ViewerStore.Zoom()

        val centerX = containerWidth / 2f
        val centerY = containerHeight / 2f
        // Смещение после изменения масштаба: точка под пальцем не «уезжает».
        val ratio = 1f - newScale / oldScale
        val scaledX = (zoom.offsetX + centroidX - centerX) * ratio
        val scaledY = (zoom.offsetY + centroidY - centerY) * ratio

        val fittedWidth = fit(containerWidth, containerHeight, imageWidth, imageHeight).first
        val fittedHeight = fit(containerWidth, containerHeight, imageWidth, imageHeight).second
        val maxOffsetX = (fittedWidth * newScale - containerWidth) / 2f
        val maxOffsetY = (fittedHeight * newScale - containerHeight) / 2f

        return ViewerStore.Zoom(
            scale = newScale,
            offsetX = (scaledX + panX).coerceIn(-maxOffsetX, maxOffsetX),
            offsetY = (scaledY + panY).coerceIn(-maxOffsetY, maxOffsetY),
        )
    }

    /**
     * Переключает масштаб: увеличенное изображение возвращается в исходное,
     * исходное увеличивается до [DOUBLE_TAP_SCALE].
     */
    fun toggle(zoom: ViewerStore.Zoom): ViewerStore.Zoom =
        if (zoom.scale > MIN_SCALE) {
            ViewerStore.Zoom()
        } else {
            ViewerStore.Zoom(scale = DOUBLE_TAP_SCALE)
        }

    /**
     * Размер изображения, вписанного в контейнер (как при `ContentScale.Fit`).
     */
    private fun fit(
        containerWidth: Float,
        containerHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
    ): Pair<Float, Float> {
        if (imageWidth <= 0f || imageHeight <= 0f) return containerWidth to containerHeight
        val scale = minOf(containerWidth / imageWidth, containerHeight / imageHeight)
        return imageWidth * scale to imageHeight * scale
    }
}
