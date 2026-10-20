package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.PlaceholderReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryListParserTest {

    @Test
    fun `empty text produces empty list`() {
        assertTrue(GalleryListParser.parse("").isEmpty())
    }

    @Test
    fun `valid http and https urls become images`() {
        val items = GalleryListParser.parse(
            "https://example.com/a.png\nhttp://example.com/b.jpg"
        )
        assertEquals(2, items.size)
        assertTrue(items.all { it is GalleryItem.Image })
    }

    @Test
    fun `non-url strings become not-url placeholders`() {
        val items = GalleryListParser.parse("hello world")
        assertEquals(1, items.size)
        val item = items.single() as GalleryItem.Placeholder
        assertEquals(PlaceholderReason.NotUrl, item.reason)
        assertEquals("hello world", item.source)
    }

    @Test
    fun `non-http schemes are not treated as urls`() {
        val items = GalleryListParser.parse("ftp://example.com/a.png")
        assertEquals(PlaceholderReason.NotUrl, (items.single() as GalleryItem.Placeholder).reason)
    }

    @Test
    fun `relative paths are not urls`() {
        val items = GalleryListParser.parse("example.com/a.png")
        assertEquals(PlaceholderReason.NotUrl, (items.single() as GalleryItem.Placeholder).reason)
    }

    @Test
    fun `url without host is not valid`() {
        val items = GalleryListParser.parse("https://")
        assertEquals(PlaceholderReason.NotUrl, (items.single() as GalleryItem.Placeholder).reason)
    }

    @Test
    fun `mixed content preserves order`() {
        val items = GalleryListParser.parse(
            "hello\nhttps://example.com/a.png\nworld\nhttp://example.com/b.jpg"
        )
        assertEquals(4, items.size)
        assertTrue(items[0] is GalleryItem.Placeholder)
        assertTrue(items[1] is GalleryItem.Image)
        assertTrue(items[2] is GalleryItem.Placeholder)
        assertTrue(items[3] is GalleryItem.Image)
    }

    @Test
    fun `blank lines are skipped`() {
        val items = GalleryListParser.parse("\n   \nhttps://example.com/a.png\n\n")
        assertEquals(1, items.size)
        assertTrue(items.single() is GalleryItem.Image)
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        val items = GalleryListParser.parse("  https://example.com/a.png  ")
        assertEquals(1, items.size)
        val item = items.single() as GalleryItem.Image
        assertEquals("https://example.com/a.png", item.source)
    }

    @Test
    fun `duplicate lines get distinct ids`() {
        val items = GalleryListParser.parse(
            "https://example.com/a.png\nhttps://example.com/a.png"
        )
        assertEquals(2, items.size)
        assertEquals("https://example.com/a.png", items[0].id)
        assertEquals("https://example.com/a.png#1", items[1].id)
    }

    @Test
    fun `ids preserve original line for duplicates with index`() {
        val items = GalleryListParser.parse(
            "https://example.com/a.png\nhttps://example.com/b.jpg\nhttps://example.com/a.png"
        )
        assertEquals("https://example.com/a.png", items[0].id)
        assertEquals("https://example.com/b.jpg", items[1].id)
        assertEquals("https://example.com/a.png#1", items[2].id)
    }

    @Test
    fun `line without host is a placeholder and does not break the list`() {
        // Кривые строки не отклоняют список: на их месте показывается
        // заглушка, как и требуется для не-ссылок.
        val items = GalleryListParser.parse(
            "https://example.com/a.png\n::not a url::\nhttps://example.com/b.jpg"
        )
        assertEquals(3, items.size)
        assertTrue(items[0] is GalleryItem.Image)
        assertEquals(PlaceholderReason.NotUrl, (items[1] as GalleryItem.Placeholder).reason)
        assertTrue(items[2] is GalleryItem.Image)
    }
}
