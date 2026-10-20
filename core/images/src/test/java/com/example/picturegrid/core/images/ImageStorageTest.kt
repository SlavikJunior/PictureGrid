package com.example.picturegrid.core.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImageStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun storage(): ImageStorageImpl = ImageStorageImpl(File(tempFolder.root, "orig"))

    @Test
    fun `cachedFile returns null when nothing stored`() {
        assertNull(storage().cachedFile("https://example.com/a.png"))
    }

    @Test
    fun `store and read back bytes`() {
        val storage = storage()
        val bytes = "hello".toByteArray()
        val result = kotlinx.coroutines.runBlocking { storage.store("https://example.com/a.png", bytes) }
        assertTrue(result.isSuccess)
        val file = storage.cachedFile("https://example.com/a.png")
        assertNotNull(file)
        assertEquals("hello", file!!.readText())
    }

    @Test
    fun `safe key differs for different urls`() {
        val a = ImageStorageImpl.storageKey("https://example.com/a.png")
        val b = ImageStorageImpl.storageKey("https://example.com/b.png")
        assertTrue(a != b)
        assertEquals(64, a.length) // sha-256 hex
    }

    @Test
    fun `same url yields same key`() {
        assertEquals(
            ImageStorageImpl.storageKey("https://example.com/a.png"),
            ImageStorageImpl.storageKey("https://example.com/a.png"),
        )
    }

    @Test
    fun `failed store does not corrupt previous file`() {
        val storage = storage()
        val url = "https://example.com/a.png"
        kotlinx.coroutines.runBlocking { storage.store(url, "first".toByteArray()) }
        val original = storage.cachedFile(url)!!.readText()
        // Занимаем путь временного файла каталогом, чтобы запись не удалась.
        val target = storage.cachedFile(url)!!
        File(target.parentFile, target.name + ".tmp").mkdirs()
        val result = kotlinx.coroutines.runBlocking { storage.store(url, "second".toByteArray()) }
        assertTrue(result.isFailure)
        // предыдущий файл не пострадал
        assertEquals(original, storage.cachedFile(url)!!.readText())
    }

    @Test
    fun `partial temp file is not exposed as cached`() {
        val storage = storage()
        val url = "https://example.com/a.png"
        val dir = storage.directory()
        dir.mkdirs()
        val key = ImageStorageImpl.storageKey(url)
        // Создаём только tmp-файл, целевого нет.
        File(dir, "$key.img.tmp").writeText("partial")
        assertNull(storage.cachedFile(url))
    }

    @Test
    fun `delete removes stored file`() {
        val storage = storage()
        val url = "https://example.com/a.png"
        kotlinx.coroutines.runBlocking { storage.store(url, "hello".toByteArray()) }
        assertNotNull(storage.cachedFile(url))

        assertTrue(storage.delete(url))

        assertNull(storage.cachedFile(url))
    }

    @Test
    fun `delete of missing file is not a failure`() {
        val storage = storage()
        assertTrue(storage.delete("https://example.com/missing.png"))
    }

    @Test
    fun `oldest originals are removed when quota is exceeded`() {
        val storage = ImageStorageImpl(
            File(tempFolder.root, "quota"),
            kotlinx.coroutines.Dispatchers.IO,
            maxBytes = 150,
        )
        val first = kotlinx.coroutines.runBlocking {
            storage.store("https://example.com/1.png", ByteArray(60)).getOrThrow()
        }
        val second = kotlinx.coroutines.runBlocking {
            storage.store("https://example.com/2.png", ByteArray(60)).getOrThrow()
        }
        // Порядок вытеснения задаётся временем изменения файла.
        first.setLastModified(1_000_000L)
        second.setLastModified(2_000_000L)

        val third = kotlinx.coroutines.runBlocking {
            storage.store("https://example.com/3.png", ByteArray(60)).getOrThrow()
        }

        assertFalse("самый старый оригинал должен быть удалён", first.exists())
        assertTrue("свежий оригинал должен остаться", second.exists())
        assertTrue(third.exists())
    }

    @Test
    fun `freshly stored file is never evicted`() {
        val storage = ImageStorageImpl(
            File(tempFolder.root, "small-quota"),
            kotlinx.coroutines.Dispatchers.IO,
            maxBytes = 10,
        )
        val file = kotlinx.coroutines.runBlocking {
            storage.store("https://example.com/big.png", ByteArray(100)).getOrThrow()
        }
        assertTrue(file.exists())
    }
}
