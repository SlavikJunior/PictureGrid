package com.example.picturegrid.feature.gallery.api

import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.PlaceholderReason
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверка подготовки данных для просмотра: в перелистывании участвуют
 * только изображения, позиция выбранного считается среди них.
 */
class GalleryImagesTest {

    private fun image(id: String) = GalleryItem.Image(id = id, source = "https://example.com/$id.png")

    @Test
    fun `selection keeps order of images`() {
        val items = listOf(
            image("a"),
            GalleryItem.Placeholder("b", "lalala", PlaceholderReason.NotUrl),
            image("c"),
        )
        val selection = selection(items, "c")
        assertEquals(listOf("https://example.com/a.png", "https://example.com/c.png"), selection.urls)
        assertEquals(1, selection.index)
    }

    @Test
    fun `selection of first image`() {
        val items = listOf(image("a"), image("b"))
        val selection = selection(items, "a")
        assertEquals(0, selection.index)
    }

    @Test
    fun `unknown selection falls back to first image`() {
        val items = listOf(image("a"), image("b"))
        val selection = selection(items, "missing")
        assertEquals(0, selection.index)
    }

    @Test
    fun `placeholder cannot be selected`() {
        val items = listOf(
            GalleryItem.Placeholder("p", "lalala", PlaceholderReason.NotUrl),
            image("a"),
        )
        val selection = selection(items, "p")
        assertEquals(0, selection.index)
        assertEquals(listOf("https://example.com/a.png"), selection.urls)
    }
}
