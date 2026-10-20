package com.example.picturegrid.feature.gallery.impl

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GalleryListStorageConcurrencyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `parallel writes leave one valid content`() {
        val storage = GalleryListStorage(File(tempFolder.root, "list.txt"))
        val t1 = Thread { storage.write("first content") }
        val t2 = Thread { storage.write("second content") }
        t1.start()
        t2.start()
        t1.join()
        t2.join()

        val result = storage.read()
        assertTrue(result == "first content" || result == "second content")
        val remaining = tempFolder.root.listFiles()?.map { it.name } ?: emptyList()
        assertTrue("list.txt" in remaining)
        assertTrue("list.txt.tmp" !in remaining)
    }
}
