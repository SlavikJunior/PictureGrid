package com.example.picturegrid.feature.gallery.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arkivanov.mvikotlin.core.rx.Observer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.picturegrid.feature.gallery.api.GalleryImages
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import com.example.picturegrid.feature.gallery.api.ImageSelection

@Composable
fun GalleryScreen(
    repository: GalleryRepository? = null,
    onImageClick: (ImageSelection) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = repository ?: createDefaultRepository(context)
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { GalleryStoreFactory(DefaultStoreFactory(), repo, syncScheduler = { GallerySyncScheduler.schedule(context) }).create() }
    DisposableEffect(store) { onDispose { store.dispose() } }
    DisposableEffect(lifecycleOwner, store) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                store.accept(GalleryStore.Intent.ReloadCached)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var state by remember { mutableStateOf(GalleryStore.State()) }
    DisposableEffect(store) {
        val disposable = store.states(object : Observer<GalleryStore.State> {
            override fun onNext(value: GalleryStore.State) { state = value }
            override fun onComplete() {}
        })
        onDispose { disposable.dispose() }
    }
    GalleryContent(
        state = state,
        onRetry = { store.accept(GalleryStore.Intent.Retry) },
        onImageClick = { item -> onImageClick(GalleryImages.selection(state.items, item.id)) },
        modifier = modifier,
    )
}
