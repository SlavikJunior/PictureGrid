package com.example.picturegrid.feature.gallery.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Маршрут галереи.
 *
 * Маршрут принадлежит фиче, которой принадлежит экран (INV-18): приложение
 * регистрирует его в composition root, а другие фичи о нём не знают.
 */
@Serializable
public data object GalleryRoute : NavKey
