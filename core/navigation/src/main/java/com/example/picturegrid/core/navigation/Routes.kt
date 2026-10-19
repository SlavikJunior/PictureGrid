package com.example.picturegrid.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object GalleryRoute : NavKey

@Serializable
data class ViewerRoute(val url: String) : NavKey
