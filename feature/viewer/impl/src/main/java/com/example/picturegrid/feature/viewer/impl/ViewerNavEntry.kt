package com.example.picturegrid.feature.viewer.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.picturegrid.core.navigation.BottomSheetSceneStrategy
import com.example.picturegrid.core.navigation.ViewerRoute
import com.example.picturegrid.core.navigation.ViewerSelection

/** Регистрирует destination просмотра оригинала в composition root приложения. */
fun EntryProviderScope<NavKey>.viewerNavEntry(
    selectionProvider: (String) -> ViewerSelection?,
    onShare: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit,
) {
    entry<ViewerRoute>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
        selectionProvider(key.selectionId)?.let { selection ->
            ViewerScreen(
                urls = selection.urls,
                initialIndex = selection.index,
                onShare = onShare,
                onOpenInBrowser = onOpenInBrowser,
            )
        }
    }
}
