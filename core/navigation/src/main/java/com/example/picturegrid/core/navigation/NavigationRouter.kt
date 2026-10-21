package com.example.picturegrid.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavigationRouter(
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

data class ViewerSelection(
    val urls: List<String>,
    val index: Int,
)
