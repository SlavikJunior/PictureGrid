package com.example.picturegrid.feature.gallery.impl

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Атомарное хранение содержимого списка в приватном каталоге приложения.
 *
 * Запись идёт через временный файл с последующим переименованием, поэтому
 * при ошибке записи ранее сохранённая версия не повреждается. Класс не
 * зависит от Android Context: путь передаётся явно, что упрощает тестирование.
 */
class GalleryListStorage(
    private val file: File,
) {

    fun read(): String? {
        return if (file.exists()) file.readText() else null
    }

    @Throws(IOException::class)
    fun write(content: String) {
        val lock = Companion.locks.getOrPut(file.absolutePath) { Any() }
        synchronized(lock) {
            writeLocked(content)
        }
    }

    @Throws(IOException::class)
    private fun writeLocked(content: String) {
        val parent = file.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw IOException("Cannot create directory: ${parent.absolutePath}")
        }
        val tmp = File(parent, file.name + ".tmp")
        tmp.writeText(content)
        try {
            Files.move(
                tmp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (e: Exception) {
            if (file.exists() && !file.delete()) {
                tmp.delete()
                throw IOException("Cannot replace file: ${file.absolutePath}", e)
            }
            if (!tmp.renameTo(file)) {
                tmp.delete()
                throw IOException("Cannot rename temp file to: ${file.absolutePath}", e)
            }
        }
    }

    companion object {
        private val locks = java.util.concurrent.ConcurrentHashMap<String, Any>()
    }
}
