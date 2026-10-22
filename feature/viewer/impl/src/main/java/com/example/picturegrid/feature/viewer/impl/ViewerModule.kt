package com.example.picturegrid.feature.viewer.impl

import com.example.picturegrid.core.images.OriginalImageLoader
import com.arkivanov.mvikotlin.core.store.StoreFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStoreFactory
import com.example.picturegrid.feature.viewer.impl.presentation.ui.ViewerZoom

/** Koin-граф зависимостей просмотрщика. */
fun viewerModule(): Module = module {
    factory<ViewerStoreFactory> {
        parameters ->
        ViewerStoreFactory(
            storeFactory = get<StoreFactory>(),
            loader = get<OriginalImageLoader>(),
            urls = parameters.get(),
            initialIndex = parameters.get(),
            zoomRules = get(),
        )
    }
    single<ViewerZoom> { ViewerZoom() }
}
