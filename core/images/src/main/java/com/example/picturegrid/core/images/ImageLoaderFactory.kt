package com.example.picturegrid.core.images

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toPath

/**
 * Единая конфигурация Coil `ImageLoader` для превью и (в будущем) оригиналов.
 *
 * Использование одного экземпляра исключает несколько независимых кэшей.
 * Memory cache и disk cache настраиваются явно: превью Coil остаются кэшем
 * UI, а гарантированное хранение оригиналов выполняется через [ImageStorage].
 *
 * Сетевой фетчер Coil подключается автоматически: `coil-network-okhttp`
 * регистрирует его через `META-INF/services`, поэтому отдельный клиент
 * OkHttp конфигурировать не требуется. Размер превью ограничивает сам Coil:
 * запрос выполняется с `ConstraintsSizeResolver`, то есть декодируется под
 * размер ячейки, а не в оригинальном разрешении.
 */
object ImageLoaderFactory {

    fun create(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader.Builder(appContext)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(appContext, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("coil_image_cache").absolutePath.toPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
