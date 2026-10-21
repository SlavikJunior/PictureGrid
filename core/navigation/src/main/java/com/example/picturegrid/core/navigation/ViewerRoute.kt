package com.example.picturegrid.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Идентификатор выбора, зарегистрированного composition root. */
@Serializable
data class ViewerRoute(val selectionId: String) : NavKey
