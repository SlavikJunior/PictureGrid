package com.example.picturegrid

import coil3.ImageLoader
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.picturegrid.core.images.ImageLoaderFactory
import com.example.picturegrid.core.images.ImageContentClassifier
import com.example.picturegrid.core.images.BitmapImageContentChecker
import com.example.picturegrid.core.images.ImageContentChecker
import com.example.picturegrid.core.images.ImageStorage
import com.example.picturegrid.core.images.ImageStorageImpl
import com.example.picturegrid.core.images.OriginalImageLoader
import com.example.picturegrid.core.network.NetworkClient
import com.example.picturegrid.core.network.RetrofitNetworkClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** App-level Koin graph for infrastructure shared by features. */
fun appModule(): Module = module {
    single<NetworkClient> { RetrofitNetworkClient() }
    single<ImageStorage> {
        ImageStorageImpl(androidContext().filesDir.resolve("images/originals"))
    }
    single<ImageContentClassifier> { ImageContentClassifier() }
    single<BitmapImageContentChecker> { BitmapImageContentChecker() }
    single<ImageContentChecker> { get<BitmapImageContentChecker>() }
    single<OriginalImageLoader> {
        OriginalImageLoader(get(), get(), kotlinx.coroutines.Dispatchers.IO, get(), get())
    }
    single<ImageLoader> { ImageLoaderFactory(androidContext(), get()).create() }
    single<StoreFactory> { DefaultStoreFactory() }
    single<ShareIntents> { ShareIntents() }
}
