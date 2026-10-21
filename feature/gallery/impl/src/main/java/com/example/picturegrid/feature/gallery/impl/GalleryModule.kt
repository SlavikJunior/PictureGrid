package com.example.picturegrid.feature.gallery.impl

import androidx.work.WorkManager
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.example.picturegrid.feature.gallery.api.domain.GalleryRepository
import com.example.picturegrid.feature.gallery.api.NetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.plugin.module.dsl.worker

/** Koin-граф зависимостей галереи. */
fun galleryModule(): Module = module {
    single<GalleryRepository> {
        GalleryRepositoryImpl(
            networkClient = get(),
            storage = GalleryListStorage(androidContext().filesDir.resolve("gallery/list.txt")),
            parser = get(),
        )
    }
    single<GalleryListParser> { GalleryListParser() }
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
    single<GallerySyncScheduler> { GallerySyncScheduler(WorkManager.getInstance(androidContext())) }
    factory<GalleryStoreFactory> {
        GalleryStoreFactory(
            storeFactory = get<StoreFactory>(),
            repository = get(),
            syncScheduler = get<GallerySyncScheduler>()::schedule,
        )
    }
    worker<GallerySyncWorker>()
}
