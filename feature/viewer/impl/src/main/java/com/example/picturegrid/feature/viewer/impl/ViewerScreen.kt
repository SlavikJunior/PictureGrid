package com.example.picturegrid.feature.viewer.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.mvikotlin.core.rx.Observer
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.picturegrid.core.images.OriginalImageLoaderFactory

@Composable
fun ViewerScreen(
    url: String,
    loader: com.example.picturegrid.core.images.OriginalImageLoader? = null,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actualLoader = loader ?: remember { OriginalImageLoaderFactory.create(context) }
    val store = remember { ViewerStoreFactory(DefaultStoreFactory(), actualLoader, url).create() }
    DisposableEffect(store) { onDispose { store.dispose() } }
    var state by remember { mutableStateOf(ViewerStore.State(url = url)) }
    DisposableEffect(store) {
        val disposable = store.states(object : Observer<ViewerStore.State> {
            override fun onNext(value: ViewerStore.State) { state = value }
            override fun onComplete() {}
        })
        onDispose { disposable.dispose() }
    }
    ViewerContent(
        state = state,
        onIntent = { store.accept(it) },
        modifier = modifier,
    )
}
