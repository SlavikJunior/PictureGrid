package com.example.picturegrid.core.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Стратегия, представляющая destination как Material 3 [ModalBottomSheet].
 *
 * Лист сохраняет скруглённые верхние углы даже в полностью раскрытом
 * состоянии и закрывается системной кнопкой Back / onBack.
 */
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        if (entry.metadata[BOTTOM_SHEET_KEY] != true) return null

        return object : OverlayScene<T> {
            override val key = entry.contentKey
            override val entries = listOf(entry)
            override val previousEntries = entries.dropLast(1)
            override val overlaidEntries = previousEntries.takeLast(1)

            lateinit var sheetState: SheetState

            @OptIn(ExperimentalMaterial3Api::class)
            override val content: @Composable (() -> Unit) = {
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ModalBottomSheet(
                    onDismissRequest = onBack,
                    sheetState = sheetState,
                    shape = shape,
                    dragHandle = null,
                ) {
                    entry.Content()
                }
            }
        }
    }

    companion object {
        const val BOTTOM_SHEET_KEY = "bottomSheet"

        fun bottomSheet(): Map<String, Any> = mapOf(BOTTOM_SHEET_KEY to true)
    }
}
