package com.example.picturegrid.feature.viewer.impl

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверка правил масштабирования: ограничения масштаба и сдвига, а также
 * поведение точки под пальцем.
 */
class ViewerZoomTest {

    private val containerWidth = 1000f
    private val containerHeight = 1000f

    private fun zoom(
        scale: Float = ViewerZoom.MIN_SCALE,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
    ) = ViewerStore.Zoom(scale = scale, offsetX = offsetX, offsetY = offsetY)

    private fun apply(
        current: ViewerStore.Zoom = zoom(),
        zoomChange: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f,
        centroidX: Float = containerWidth / 2f,
        centroidY: Float = containerHeight / 2f,
        imageWidth: Float = 1000f,
        imageHeight: Float = 1000f,
    ) = ViewerZoom.apply(
        zoom = current,
        zoomChange = zoomChange,
        panX = panX,
        panY = panY,
        centroidX = centroidX,
        centroidY = centroidY,
        containerWidth = containerWidth,
        containerHeight = containerHeight,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
    )

    @Test
    fun `zoom in increases scale`() {
        val result = apply(zoomChange = 2f)
        assertEquals(2f, result.scale, 0.001f)
    }

    @Test
    fun `scale is limited by maximum`() {
        val result = apply(zoomChange = 10f)
        assertEquals(ViewerZoom.MAX_SCALE, result.scale, 0.001f)
    }

    @Test
    fun `scale is not reduced below minimum and offset is reset`() {
        val result = apply(current = zoom(scale = 2f, offsetX = 100f), zoomChange = 0.1f)
        assertEquals(ViewerZoom.MIN_SCALE, result.scale, 0.001f)
        assertEquals(0f, result.offsetX, 0.001f)
    }

    @Test
    fun `vertical offset is limited by the letterboxed image`() {
        // Изображение 1000x500 вписывается в контейнер по ширине: при
        // масштабе 3 оно выходит за границы по высоте на 500 пикселей,
        // поэтому по вертикали можно сдвинуть максимум на 250.
        val result = apply(
            current = zoom(scale = 2f),
            zoomChange = 1.5f,
            panY = 10_000f,
            imageHeight = 500f,
        )
        assertEquals(3f, result.scale, 0.001f)
        assertEquals(250f, result.offsetY, 0.001f)
    }

    @Test
    fun `offset is limited in both directions`() {
        val result = apply(current = zoom(scale = 2f), zoomChange = 1f, panX = -10_000f)
        assertEquals(-500f, result.offsetX, 0.001f)
    }

    @Test
    fun `point under the finger stays in place`() {
        // Палец на 100 пикселей правее и 50 ниже центра.
        val centroidX = containerWidth / 2f + 100f
        val centroidY = containerHeight / 2f + 50f
        val before = zoom()
        val after = apply(current = before, zoomChange = 2f, centroidX = centroidX, centroidY = centroidY)

        // Точка изображения под пальцем до жеста и после него должна совпадать.
        val contentX = centroidX - before.offsetX
        val contentY = centroidY - before.offsetY
        val screenX = (contentX - containerWidth / 2f) * after.scale + containerWidth / 2f + after.offsetX
        val screenY = (contentY - containerHeight / 2f) * after.scale + containerHeight / 2f + after.offsetY
        assertEquals(centroidX, screenX, 1f)
        assertEquals(centroidY, screenY, 1f)
    }

    @Test
    fun `double tap toggles zoom`() {
        val zoomed = ViewerZoom.toggle(zoom())
        assertEquals(ViewerZoom.DOUBLE_TAP_SCALE, zoomed.scale, 0.001f)

        val restored = ViewerZoom.toggle(zoomed)
        assertEquals(ViewerZoom.MIN_SCALE, restored.scale, 0.001f)
        assertEquals(0f, restored.offsetX, 0.001f)
        assertEquals(0f, restored.offsetY, 0.001f)
    }
}
