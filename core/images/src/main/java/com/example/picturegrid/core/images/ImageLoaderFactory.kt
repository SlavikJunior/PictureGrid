package com.example.picturegrid.core.images

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import java.io.File
import okio.Path.Companion.toPath

/**
 * Единая конфигурация Coil `ImageLoader` для превью и (в будущем) оригиналов.
 *
 * Использование одного экземпляра исключает несколько независимых кэшей.
 * Memory cache и disk cache настраиваются явно: превью Coil остаются кэшем
 * UI, а гарантированное хранение оригиналов выполняется через [ImageStorage].
 */
object ImageLoaderFactory {

    fun create(context: Context): ImageLoader {
        val appContext = context.applicationContext
        val okHttpClient = OkHttpClient()
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
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
    }
}
