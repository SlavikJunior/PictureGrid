package com.example.picturegrid.feature.viewer.impl.presentation.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.picturegrid.core.navigation.LocalBottomSheetController
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStore
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStoreFactory

/**
 * @param urls изображения для перелистывания.
 * @param initialIndex позиция открытого изображения в списке.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewerScreen(
    modifier: Modifier = Modifier,
    urls: List<String>,
    initialIndex: Int = 0,
    onShare: (String) -> Unit = {},
    onOpenInBrowser: (String) -> Unit = {},
) {
    val viewerStoreFactory: ViewerStoreFactory = koinInject { parametersOf(urls, initialIndex) }
    val store = remember(viewerStoreFactory) { viewerStoreFactory.create() }
    DisposableEffect(store) { onDispose { store.dispose() } }
    val state by store.stateFlow.collectAsStateWithLifecycle()

    // Полноэкранный режим: лист раскрывается, системные панели скрываются.
    // Лист живёт в отдельном окне, поэтому управление им принадлежит
    // модулю навигации, а не слою приложения.
    val sheetController = LocalBottomSheetController.current
    LaunchedEffect(state.isFullscreen) {
        sheetController?.setFullscreen(state.isFullscreen)
    }

    // Back обрабатывается здесь, а не только навигацией: после выхода из
    // immersive-режима окно листа пересоздаётся, и системное нажатие может
    // потеряться, оставив лист открытым без панели действий.
    BackHandler(enabled = true) {
        if (state.isFullscreen) {
            store.accept(ViewerStore.Intent.ToggleFullscreen)
        } else {
            sheetController?.close()
        }
    }

    ViewerContent(
        state = state,
        onIntent = { store.accept(it) },
        onShare = onShare,
        onOpenInBrowser = onOpenInBrowser,
        modifier = modifier,
    )
}
