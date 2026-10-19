package com.example.picturegrid.core.navigation

interface Router {
    fun navigateToGallery()
    fun navigateToViewer(url: String)
    fun navigateBack()
}