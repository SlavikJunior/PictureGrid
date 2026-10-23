package com.example.picturegrid

import com.example.picturegrid.core.navigation.Router
import com.example.picturegrid.core.navigation.ViewerSelection
import com.example.picturegrid.feature.gallery.api.navigation.GalleryRoute
import com.example.picturegrid.feature.viewer.api.navigation.ViewerRoute
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Реализация [Router] для back stack приложения.
 *
 * Живёт в composition root: только приложение знает о маршрутах обеих фич,
 * поэтому `core:navigation` остаётся независимым от них (INV-03, INV-18).
 */
internal class NavigationRouter(
    private val backStack: NavBackStack<NavKey>,
) : Router {

    private val viewerSelections = mutableMapOf<String, ViewerSelection>()
    private var nextSelectionId = 0

    override fun navigateToGallery() {
        backStack.remove(GalleryRoute)
        backStack.add(GalleryRoute)
    }

    override fun navigateToViewer(urls: List<String>, index: Int) {
        val selectionId = "selection-${nextSelectionId++}"
        viewerSelections[selectionId] = ViewerSelection(urls = urls, index = index)
        backStack.add(ViewerRoute(selectionId = selectionId))
    }

    fun viewerSelection(selectionId: String): ViewerSelection? = viewerSelections[selectionId]

    override fun navigateBack() {
        backStack.removeLastOrNull()
    }
}
