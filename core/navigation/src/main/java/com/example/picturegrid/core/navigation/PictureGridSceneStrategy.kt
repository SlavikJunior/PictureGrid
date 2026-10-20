package com.example.picturegrid.core.navigation

import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy

/**
 * Комбинированная стратегия: BottomSheet для помеченных destinations,
 * иначе обычный однопанельный экран.
 */
class PictureGridSceneStrategy<T : Any> :
    SceneStrategy<T> by BottomSheetSceneStrategy<T>().then(SinglePaneSceneStrategy())
