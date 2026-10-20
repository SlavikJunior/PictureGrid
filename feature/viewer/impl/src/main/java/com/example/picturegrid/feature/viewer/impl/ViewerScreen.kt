package com.example.picturegrid.feature.viewer.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.mvikotlin.core.rx.Observer
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.example.picturegrid.core.images.OriginalImageLoaderFactory
import com.example.picturegrid.core.navigation.LocalBottomSheetController

/**
 * @param urls изображения для перелистывания.
 * @param initialIndex позиция открытого изображения в списке.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    urls: List<String>,
    initialIndex: Int = 0,
    loader: com.example.picturegrid.core.images.OriginalImageLoader? = null,
    onShare: (String) -> Unit = {},
    onOpenInBrowser: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actualLoader = loader ?: remember { OriginalImageLoaderFactory.create(context) }
    val store = remember(urls) {
        ViewerStoreFactory(
            storeFactory = DefaultStoreFactory(),
            loader = actualLoader,
            urls = urls,
            initialIndex = initialIndex,
        ).create()
    }
    DisposableEffect(store) { onDispose { store.dispose() } }
    var state by remember { mutableStateOf(ViewerStore.State(urls = urls, index = initialIndex)) }
    DisposableEffect(store) {
        val disposable = store.states(object : Observer<ViewerStore.State> {
            override fun onNext(value: ViewerStore.State) { state = value }
            override fun onComplete() {}
        })
        onDispose { disposable.dispose() }
    }

    // Полноэкранный режим: лист раскрывается, системные панели скрываются.
    // Лист живёт в отдельном окне, поэтому управление им принадлежит
    // модулю навигации, а не слою приложения.
    val sheetController = LocalBottomSheetController.current
    LaunchedEffect(state.isFullscreen) {
        sheetController?.setFullscreen(state.isFullscreen)
    }

    ViewerContent(
        state = state,
        onIntent = { store.accept(it) },
        onShare = onShare,
        onOpenInBrowser = onOpenInBrowser,
        modifier = modifier,
    )
}
