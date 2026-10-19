package com.example.picturegrid.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavigationRouter(
    private val backStack: NavBackStack<NavKey>,
) : Router {

    override fun navigateToGallery() {
        backStack.remove(GalleryRoute)
        backStack.add(GalleryRoute)
    }

    override fun navigateToViewer(url: String) {
        backStack.add(ViewerRoute(url))
    }

    override fun navigateBack() {
        backStack.removeLastOrNull()
    }
}
