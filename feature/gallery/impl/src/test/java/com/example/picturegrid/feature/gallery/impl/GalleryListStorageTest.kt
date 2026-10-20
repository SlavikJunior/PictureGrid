package com.example.picturegrid.feature.gallery.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class GalleryListStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun storage(fileName: String = "list.txt"): GalleryListStorage =
        GalleryListStorage(File(tempFolder.root, fileName))

    private fun contentOf(storage: GalleryListStorage): String? =
        (storage.read() as? GalleryListCache.Available)?.content

    @Test
    fun `read reports absent cache when file does not exist`() {
        assertEquals(GalleryListCache.Absent, storage().read())
    }

    @Test
    fun `write then read roundtrip`() {
        val s = storage()
        s.write("https://example.com/a.png\nhttps://example.com/b.jpg")
        assertEquals(
            GalleryListCache.Available("https://example.com/a.png\nhttps://example.com/b.jpg"),
            s.read(),
        )
    }

    @Test
    fun `overwrite replaces previous content`() {
        val s = storage()
        s.write("old content")
        s.write("new content")
        assertEquals(GalleryListCache.Available("new content"), s.read())
    }

    @Test
    fun `no temp file remains after successful write`() {
        val s = storage()
        s.write("content")
        val remaining = tempFolder.root.listFiles()?.map { it.name } ?: emptyList()
        assertEquals(listOf("list.txt"), remaining)
    }

    @Test
    fun `write failure preserves previous content`() {
        val s = storage()
        s.write("old content")
        // Force the temp-file write to fail by occupying the tmp path with a directory.
        File(tempFolder.root, "list.txt.tmp").mkdirs()
        try {
            s.write("new content")
        } catch (_: IOException) {
            // expected
        }
        assertEquals("old content", contentOf(s))
    }

    @Test
    fun `write throws when parent path is a file`() {
        val blocker = tempFolder.newFile("blocker")
        val badStorage = GalleryListStorage(File(blocker, "list.txt"))
        try {
            badStorage.write("content")
        } catch (_: IOException) {
            // expected
        }
        assertTrue(blocker.exists())
    }

    // Различение состояний кэша (PG-SEC-005).

    @Test
    fun `empty file is reported as damaged`() {
        val file = File(tempFolder.root, "list.txt")
        file.writeBytes(ByteArray(0))
        assertEquals(GalleryListCache.Damaged, storage().read())
    }

    @Test
    fun `blank file is reported as damaged`() {
        val file = File(tempFolder.root, "list.txt")
        file.writeText("\n   \n\t")
        assertEquals(GalleryListCache.Damaged, storage().read())
    }

    @Test
    fun `file with content but no valid items stays available`() {
        val file = File(tempFolder.root, "list.txt")
        file.writeText("not a url\nanother text")
        val cache = storage().read()
        assertTrue("ожидался доступный кэш, получено $cache", cache is GalleryListCache.Available)
    }

    @Test
    fun `file exceeding size limit is reported as damaged`() {
        val file = File(tempFolder.root, "list.txt")
        val oversized = ByteArray((GalleryListLimits.MAX_TEXT_BYTES + 1).toInt()) { 'a'.code.toByte() }
        file.writeBytes(oversized)
        assertEquals(GalleryListCache.Damaged, storage().read())
    }

    @Test
    fun `unreadable path is reported as damaged`() {
        val directory = tempFolder.newFolder("list.txt")
        assertEquals(GalleryListCache.Damaged, GalleryListStorage(directory).read())
    }

    @Test
    fun `damaged cache is not silently replaced by empty data`() {
        val file = File(tempFolder.root, "list.txt")
        file.writeBytes(ByteArray(0))
        val s = storage()
        assertEquals(GalleryListCache.Damaged, s.read())
        // Повторное чтение даёт тот же результат: данные не появляются и не исчезают.
        assertEquals(GalleryListCache.Damaged, s.read())
    }
}
