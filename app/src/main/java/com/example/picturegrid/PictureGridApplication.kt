package com.example.picturegrid

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext

class PictureGridApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) startKoin {
            androidContext(this@PictureGridApplication)
            workManagerFactory()
            modules(
                appModule(),
                com.example.picturegrid.feature.gallery.impl.galleryModule(),
                com.example.picturegrid.feature.viewer.impl.viewerModule(),
            )
        }
    }
}
