package com.example.picturegrid.feature.gallery.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `read returns null when file does not exist`() {
        assertNull(storage().read())
    }

    @Test
    fun `write then read roundtrip`() {
        val s = storage()
        s.write("https://example.com/a.png\nhttps://example.com/b.jpg")
        assertEquals("https://example.com/a.png\nhttps://example.com/b.jpg", s.read())
    }

    @Test
    fun `overwrite replaces previous content`() {
        val s = storage()
        s.write("old content")
        s.write("new content")
        assertEquals("new content", s.read())
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
        assertEquals("old content", s.read())
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
}
