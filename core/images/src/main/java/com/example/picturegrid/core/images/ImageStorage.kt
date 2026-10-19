package com.example.picturegrid.core.images

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Гарантированное постоянное хранение оригиналов изображений
 * в приватном каталоге приложения.
 *
 * Ключ хранения — SHA-256 от URL, безопасный для файловой системы.
 * Запись идёт через временный файл с атомарным перемещением, поэтому
 * частичный файл никогда не становится доступным как готовый оригинал,
 * а предыдущая сохранённая копия не повреждается при неудачной записи.
 */
interface ImageStorage {

    /** Каталог, в котором хранятся оригиналы. */
    fun directory(): File

    /** Уже сохранённый файл для [url] или null, если его нет. */
    fun cachedFile(url: String): File?

    /** Атомарно сохраняет [bytes] как оригинал [url]. */
    suspend fun store(url: String, bytes: ByteArray): Result<File>
}

class ImageStorageImpl(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ImageStorage {

    override fun directory(): File = directory

    override fun cachedFile(url: String): File? {
        val target = fileFor(url)
        return if (target.exists() && target.length() > 0L) target else null
    }

    override suspend fun store(url: String, bytes: ByteArray): Result<File> =
        withContext(ioDispatcher) {
            val target = fileFor(url)
            val parent = target.parentFile
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return@withContext Result.failure(IOException("Cannot create directory: ${parent.absolutePath}"))
            }
            val tmp = File(parent, target.name + ".tmp")
            try {
                tmp.writeBytes(bytes)
                try {
                    Files.move(
                        tmp.toPath(),
                        target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (e: Exception) {
                    // ATOMIC_MOVE может не поддерживаться на некоторых ФС.
                    if (target.exists() && !target.delete()) {
                        tmp.delete()
                        return@withContext Result.failure(IOException("Cannot replace file: ${target.absolutePath}", e))
                    }
                    if (!tmp.renameTo(target)) {
                        tmp.delete()
                        return@withContext Result.failure(IOException("Cannot rename temp file to: ${target.absolutePath}", e))
                    }
                }
                Result.success(target)
            } catch (e: Exception) {
                tmp.delete()
                Result.failure(e)
            }
        }

    private fun fileFor(url: String): File =
        File(directory, storageKey(url) + ".img")

    companion object {
        fun storageKey(url: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(url.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
