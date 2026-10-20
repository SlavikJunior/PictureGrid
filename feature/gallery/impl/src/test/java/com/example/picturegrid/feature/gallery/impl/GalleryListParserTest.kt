package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.PlaceholderReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryListParserTest {

    private fun parse(text: String): List<GalleryItem> {
        val result = GalleryListParser.parse(text)
        assertTrue("ожидался успешный разбор, получено $result", result is GalleryListParseResult.Parsed)
        return (result as GalleryListParseResult.Parsed).items
    }

    private fun rejectionOf(text: String): GalleryListRejection {
        val result = GalleryListParser.parse(text)
        assertTrue("ожидалось отклонение списка, получено $result", result is GalleryListParseResult.Rejected)
        return (result as GalleryListParseResult.Rejected).reason
    }

    @Test
    fun `empty text produces empty list`() {
        assertTrue(parse("").isEmpty())
    }

    @Test
    fun `valid http and https urls become images`() {
        val items = parse(
            "https://example.com/a.png\nhttp://example.com/b.jpg"
        )
        assertEquals(2, items.size)
        assertTrue(items.all { it is GalleryItem.Image })
    }

    @Test
    fun `non-url strings become not-url placeholders`() {
        val items = parse("hello world")
        assertEquals(1, items.size)
        val item = items.single() as GalleryItem.Placeholder
        assertEquals(PlaceholderReason.NotUrl, item.reason)
        assertEquals("hello world", item.source)
    }

    @Test
    fun `non-http schemes are not treated as urls`() {
        val items = parse("ftp://example.com/a.png")
        assertEquals(PlaceholderReason.NotUrl, (items.single() as GalleryItem.Placeholder).reason)
    }

    @Test
    fun `relative paths are not urls`() {
        val items = parse("example.com/a.png")
        assertEquals(PlaceholderReason.NotUrl, (items.single() as GalleryItem.Placeholder).reason)
    }

    @Test
    fun `url without host is not valid`() {
        val items = parse("https://")
        assertEquals(PlaceholderReason.NotUrl, (items.single() as GalleryItem.Placeholder).reason)
    }

    @Test
    fun `mixed content preserves order`() {
        val items = parse(
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
        val items = parse("\n   \nhttps://example.com/a.png\n\n")
        assertEquals(1, items.size)
        assertTrue(items.single() is GalleryItem.Image)
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        val items = parse("  https://example.com/a.png  ")
        assertEquals(1, items.size)
        val item = items.single() as GalleryItem.Image
        assertEquals("https://example.com/a.png", item.source)
    }

    @Test
    fun `duplicate lines get distinct ids`() {
        val items = parse(
            "https://example.com/a.png\nhttps://example.com/a.png"
        )
        assertEquals(2, items.size)
        assertEquals("https://example.com/a.png", items[0].id)
        assertEquals("https://example.com/a.png#1", items[1].id)
    }

    @Test
    fun `ids preserve original line for duplicates with index`() {
        val items = parse(
            "https://example.com/a.png\nhttps://example.com/b.jpg\nhttps://example.com/a.png"
        )
        assertEquals("https://example.com/a.png", items[0].id)
        assertEquals("https://example.com/b.jpg", items[1].id)
        assertEquals("https://example.com/a.png#1", items[2].id)
    }

    // Политика сетевых назначений (PG-SEC-003).

    @Test
    fun `public url remains an image`() {
        val items = parse("https://images.example.com/photo.png")
        assertTrue(items.single() is GalleryItem.Image)
    }

    @Test
    fun `loopback url becomes a forbidden destination placeholder`() {
        val items = parse("http://127.0.0.1:8080/photo.png")
        val item = items.single() as GalleryItem.Placeholder
        assertEquals(PlaceholderReason.ForbiddenDestination, item.reason)
    }

    @Test
    fun `private address becomes a forbidden destination placeholder`() {
        val item = parse("http://192.168.1.1/photo.png").single() as GalleryItem.Placeholder
        assertEquals(PlaceholderReason.ForbiddenDestination, item.reason)
    }

    @Test
    fun `link local address becomes a forbidden destination placeholder`() {
        val item = parse("http://169.254.169.254/latest/meta-data/").single() as GalleryItem.Placeholder
        assertEquals(PlaceholderReason.ForbiddenDestination, item.reason)
    }

    @Test
    fun `local host name becomes a forbidden destination placeholder`() {
        val item = parse("http://localhost/photo.png").single() as GalleryItem.Placeholder
        assertEquals(PlaceholderReason.ForbiddenDestination, item.reason)
        val router = parse("http://router/photo.png").single() as GalleryItem.Placeholder
        assertEquals(PlaceholderReason.ForbiddenDestination, router.reason)
    }

    @Test
    fun `url with userinfo becomes a forbidden destination placeholder`() {
        val item = parse("http://user:password@example.com/photo.png").single() as GalleryItem.Placeholder
        assertEquals(PlaceholderReason.ForbiddenDestination, item.reason)
    }

    @Test
    fun `order and placeholders are preserved with policy rejections`() {
        val items = parse(
            "hello\nhttp://127.0.0.1/photo.png\nhttps://images.example.com/photo.png"
        )
        assertEquals(3, items.size)
        assertTrue(items[0] is GalleryItem.Placeholder)
        assertEquals(
            PlaceholderReason.ForbiddenDestination,
            (items[1] as GalleryItem.Placeholder).reason,
        )
        assertTrue(items[2] is GalleryItem.Image)
    }

    // Ограничения формата списка (PG-SEC-008).

    @Test
    fun `list below the item limit is parsed`() {
        val text = (1 until GalleryListLimits.MAX_ITEMS).joinToString("\n") { "https://example.com/$it.png" }
        val items = parse(text)
        assertEquals(GalleryListLimits.MAX_ITEMS - 1, items.size)
    }

    @Test
    fun `list exactly at the item limit is parsed`() {
        val text = (1..GalleryListLimits.MAX_ITEMS).joinToString("\n") { "https://example.com/$it.png" }
        val items = parse(text)
        assertEquals(GalleryListLimits.MAX_ITEMS, items.size)
    }

    @Test
    fun `list above the item limit is rejected`() {
        val text = (1..GalleryListLimits.MAX_ITEMS + 1).joinToString("\n") { "https://example.com/$it.png" }
        assertEquals(GalleryListRejection.TOO_MANY_ITEMS, rejectionOf(text))
    }

    @Test
    fun `many blank lines do not bypass the item limit`() {
        val text = buildString {
            repeat(100_000) { append("\n") }
            repeat(GalleryListLimits.MAX_ITEMS + 1) { append("https://example.com/$it.png\n") }
        }
        assertEquals(GalleryListRejection.TOO_MANY_ITEMS, rejectionOf(text))
    }

    @Test
    fun `duplicates count towards the item limit`() {
        val text = (1..GalleryListLimits.MAX_ITEMS + 1).joinToString("\n") { "https://example.com/same.png" }
        assertEquals(GalleryListRejection.TOO_MANY_ITEMS, rejectionOf(text))
    }

    @Test
    fun `line at the length limit is parsed`() {
        val prefix = "https://example.com/"
        val line = prefix + "a".repeat(GalleryListLimits.MAX_LINE_LENGTH - prefix.length)
        assertEquals(GalleryListLimits.MAX_LINE_LENGTH, line.length)
        val items = parse(line)
        assertEquals(1, items.size)
    }

    @Test
    fun `line above the length limit is rejected`() {
        val line = "https://example.com/" + "a".repeat(GalleryListLimits.MAX_LINE_LENGTH)
        assertEquals(GalleryListRejection.LINE_TOO_LONG, rejectionOf(line))
    }

    @Test
    fun `text above the size limit is rejected`() {
        val text = "a".repeat(GalleryListLimits.MAX_TEXT_CHARS + 1)
        assertEquals(GalleryListRejection.TEXT_TOO_LARGE, rejectionOf(text))
    }

    @Test
    fun `text at the size limit is parsed`() {
        // Текст предельного размера из пустых строк: элементов нет,
        // ограничение по размеру не превышено.
        val text = "\n".repeat(GalleryListLimits.MAX_TEXT_CHARS)
        assertEquals(GalleryListLimits.MAX_TEXT_CHARS, text.length)
        val items = parse(text)
        assertTrue(items.isEmpty())
    }
}
