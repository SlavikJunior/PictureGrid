package com.example.picturegrid.feature.gallery.impl.data

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
internal class GalleryListStorage(
    private val file: File,
) {

    /**
     * Читает сохранённый список.
     *
     * @return содержимое списка либо `null`, если файла нет или он не может
     *   быть использован как список (пустой файл, ошибка чтения).
     */
    fun read(): String? {
        if (!file.exists() || !file.isFile) return null
        return try {
            val content = file.readText()
            if (content.isBlank()) null else content
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
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
            throw IOException("Cannot create directory for the list file")
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
            // ATOMIC_MOVE может не поддерживаться на некоторых ФС.
            // Обычное переименование заменяет файл целиком и сохраняет
            // предыдущую версию, если переименование не удалось.
            if (!tmp.renameTo(file)) {
                tmp.delete()
                throw IOException("Cannot replace the list file", e)
            }
        }
    }

    companion object {
        private val locks = java.util.concurrent.ConcurrentHashMap<String, Any>()
    }
}
