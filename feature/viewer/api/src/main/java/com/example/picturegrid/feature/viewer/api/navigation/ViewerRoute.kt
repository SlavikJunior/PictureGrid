package com.example.picturegrid.feature.viewer.api.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Маршрут просмотра оригинала.
 *
 * В аргументах только идентификатор выбора: сами изображения передаются
 * через реестр выбора в composition root, а не сериализуются в back stack
 * (INV-18).
 */
@Serializable
public data class ViewerRoute(public val selectionId: String) : NavKey
