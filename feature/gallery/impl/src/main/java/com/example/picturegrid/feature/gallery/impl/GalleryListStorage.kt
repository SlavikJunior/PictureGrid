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
 *
 * Чтение различает отсутствие файла, пригодное содержимое и повреждённый
 * файл: пустой или превышающий лимит файл не выдаётся за корректный список.
 */
class GalleryListStorage(
    private val file: File,
) {

    /**
     * Читает сохранённый список.
     *
     * @return состояние кэша; [GalleryListCache.Damaged] означает, что файл
     *   существует, но использовать его как список нельзя.
     */
    fun read(): GalleryListCache {
        if (!file.exists()) return GalleryListCache.Absent
        if (!file.isFile) return GalleryListCache.Damaged
        // Файл больше транспортного лимита не может быть корректным списком:
        // список приходит из сети и не превышает GalleryListLimits.MAX_TEXT_BYTES.
        if (file.length() > GalleryListLimits.MAX_TEXT_BYTES) return GalleryListCache.Damaged
        return try {
            if (file.length() <= 0L) {
                GalleryListCache.Damaged
            } else {
                val content = file.readText()
                if (content.isBlank()) GalleryListCache.Damaged else GalleryListCache.Available(content)
            }
        } catch (e: IOException) {
            GalleryListCache.Damaged
        } catch (e: SecurityException) {
            GalleryListCache.Damaged
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
