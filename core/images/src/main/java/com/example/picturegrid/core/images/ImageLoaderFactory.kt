package com.example.picturegrid.core.images

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import java.util.concurrent.TimeUnit

/**
 * Единая конфигурация Coil `ImageLoader` для превью и (в будущем) оригиналов.
 *
 * Использование одного экземпляра исключает несколько независимых кэшей.
 * Memory cache и disk cache настраиваются явно: превью Coil остаются кэшем
 * UI, а гарантированное хранение оригиналов выполняется через [ImageStorage].
 *
 * Сетевой фетчер подключается явно, собственным клиентом OkHttp: клиент
 * отбрасывает ответы, заведомо не являющиеся изображениями
 * ([ImageContentInterceptor]). Клиент зарегистрирован вручную, поэтому
 * автоматическая регистрация фетчера через `META-INF/services` не требуется
 * и не используется. Размер превью ограничивает сам Coil: запрос
 * выполняется с `ConstraintsSizeResolver`, то есть декодируется под размер
 * ячейки, а не в оригинальном разрешении.
 */
object ImageLoaderFactory {

    fun create(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return ImageLoader.Builder(appContext)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { imageHttpClient() }))
            }
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

    /**
     * Клиент для загрузки превью.
     *
     * Таймауты совпадают с клиентом списка (`core:network`), чтобы поведение
     * загрузки картинок и списка не отличалось.
     */
    private fun imageHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ImageContentInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
}
