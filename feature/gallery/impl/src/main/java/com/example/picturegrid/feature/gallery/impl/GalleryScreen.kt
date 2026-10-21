package com.example.picturegrid.feature.gallery.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.mvikotlin.core.store.Store
import com.example.picturegrid.feature.gallery.api.domain.GalleryRepository
import com.example.picturegrid.feature.gallery.api.ImageSelection
import com.example.picturegrid.feature.gallery.api.NetworkMonitor
import kotlinx.coroutines.launch
import coil3.ImageLoader
import org.koin.compose.koinInject

@Composable
internal fun GalleryScreen(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = koinInject(),
    repository: GalleryRepository = koinInject(),
    networkMonitor: NetworkMonitor = koinInject(),
    storeFactory: GalleryStoreFactory = koinInject(),
    onImageClick: (ImageSelection) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val store = remember(storeFactory) { storeFactory.create() }
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
    DisposableEffect(store, networkMonitor) {
        // Фоновая синхронизация обновляет сохранённый список, но экран об
        // этом не узнаёт. Поэтому при восстановлении сети список
        // перезапрашивается: требование задания — при появлении сети файл
        // загружается снова, и пользователь видит новые данные на экране.
        var wasAvailable: Boolean? = null
        val job = coroutineScope.launch {
            networkMonitor.isAvailable.collect { available ->
                val previous = wasAvailable
                wasAvailable = available
                if (shouldRefreshOnNetworkChange(previous, available)) {
                    store.accept(GalleryStore.Intent.Refresh)
                }
            }
        }
        onDispose { job.cancel() }
    }
    val state by store.stateFlow.collectAsStateWithLifecycle()
    GalleryContent(
        state = state,
        onRetry = { store.accept(GalleryStore.Intent.Retry) },
        onImageClick = { item -> onImageClick(com.example.picturegrid.feature.gallery.api.selection(state.items, item.id)) },
        imageLoader = imageLoader,
        modifier = modifier,
    )
}

/**
 * Нужно ли перезапрашивать список при изменении состояния сети.
 *
 * Обновление нужно только при восстановлении сети: первое значение — это
 * текущее состояние, а не событие, поэтому лишнего запроса при открытии
 * экрана не будет.
 */
internal fun shouldRefreshOnNetworkChange(previous: Boolean?, current: Boolean): Boolean =
    previous == false && current
