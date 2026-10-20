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

    override fun navigateToViewer(urls: List<String>, index: Int) {
        backStack.add(ViewerRoute(urls = urls, index = index))
    }

    override fun navigateBack() {
        backStack.removeLastOrNull()
    }
}
