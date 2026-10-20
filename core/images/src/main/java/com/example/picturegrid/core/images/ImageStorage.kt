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
 *
 * Каталог ограничен по объёму: при превышении квоты самые старые оригиналы
 * удаляются, чтобы хранилище не разрасталось бесконечно.
 */
interface ImageStorage {

    /** Каталог, в котором хранятся оригиналы. */
    fun directory(): File

    /** Уже сохранённый файл для [url] или null, если его нет. */
    fun cachedFile(url: String): File?

    /**
     * Удаляет сохранённый оригинал [url].
     *
     * @return `true`, если после вызова файла не осталось.
     */
    fun delete(url: String): Boolean

    /** Атомарно сохраняет [bytes] как оригинал [url]. */
    suspend fun store(url: String, bytes: ByteArray): Result<File>
}

class ImageStorageImpl(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) : ImageStorage {

    override fun directory(): File = directory

    override fun cachedFile(url: String): File? {
        val target = fileFor(url)
        return if (target.exists() && target.length() > 0L) target else null
    }

    override fun delete(url: String): Boolean {
        val target = fileFor(url)
        return !target.exists() || target.delete()
    }

    override suspend fun store(url: String, bytes: ByteArray): Result<File> =
        withContext(ioDispatcher) {
            val target = fileFor(url)
            val parent = target.parentFile
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return@withContext Result.failure(IOException("Cannot create directory for the originals"))
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
                        return@withContext Result.failure(IOException("Cannot replace the original file", e))
                    }
                    if (!tmp.renameTo(target)) {
                        tmp.delete()
                        return@withContext Result.failure(IOException("Cannot rename the temporary file", e))
                    }
                }
                evictOutdated(target)
                Result.success(target)
            } catch (e: Exception) {
                tmp.delete()
                Result.failure(e)
            }
        }

    /**
     * Удаляет самые старые оригиналы, пока каталог не уложится в квоту.
     *
     * Только что сохранённый файл не удаляется: он самый свежий, но
     * пользователь запросил его именно сейчас.
     */
    private fun evictOutdated(keep: File) {
        val files = directory.listFiles()
            ?.filter { it.isFile && it != keep }
            ?.sortedWith(compareBy({ it.lastModified() }, { it.name }))
            ?: return
        var total = files.sumOf { it.length() } + keep.length()
        if (total <= maxBytes) return
        for (file in files) {
            if (total <= maxBytes) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private fun fileFor(url: String): File =
        File(directory, storageKey(url) + ".img")

    companion object {
        /**
         * Квота каталога оригиналов: 200 МиБ.
         *
         * Достаточно для нескольких десятков фотографий в высоком
         * разрешении и при этом ограничивает объём приватного хранилища
         * приложения.
         */
        const val DEFAULT_MAX_BYTES: Long = 200L * 1024 * 1024

        fun storageKey(url: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(url.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
