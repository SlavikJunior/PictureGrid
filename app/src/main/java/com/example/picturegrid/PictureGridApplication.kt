package com.example.picturegrid

import android.app.Application
import coil3.SingletonImageLoader
import com.example.picturegrid.core.images.ImageLoaderFactory

/**
 * Приложение устанавливает общий `ImageLoader` Coil.
 *
 * Загрузчик создаётся один раз на процесс: так в приложении не появляется
 * нескольких независимых кэшей изображений, а превью и оригиналы
 * используют один и тот же кэш.
 */
class PictureGridApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { context -> ImageLoaderFactory.create(context) }
    }
}
