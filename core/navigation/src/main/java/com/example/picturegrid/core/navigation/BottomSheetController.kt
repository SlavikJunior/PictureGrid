package com.example.picturegrid.core.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Управление BottomSheet, доступное из содержимого листа.
 *
 * Лист живёт в отдельном окне, поэтому системные панели нужно скрывать в
 * окне листа, а не в окне Activity: иначе панели остаются видимыми.
 */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetController internal constructor(
    private val sheetState: SheetState,
    private val view: View,
    private val onClose: () -> Unit,
) {

    /**
     * Закрывает лист тем же путём, что и системный Back.
     *
     * Нужен содержимому листа, чтобы обработать Back самостоятельно: после
     * выхода из immersive-режима окно листа пересоздаётся и следующее
     * системное нажатие может быть потеряно.
     */
    fun close() = onClose()

    /**
     * Полноэкранный режим: лист раскрывается, системные панели скрываются.
     *
     * Панели можно временно вернуть свайпом от края экрана — это штатное
     * поведение immersive-режима.
     */
    suspend fun setFullscreen(enabled: Boolean) {
        if (enabled) {
            sheetState.expand()
        }
        windowInsetsControllers().forEach { controller ->
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (enabled) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        // На API 26–29 окно листа не всегда применяет флаг immersive, и
        // панели возвращаются при первом же касании. Флаги выставляются
        // напрямую, поэтому поведение одинаково на всех версиях.
        @Suppress("DEPRECATION")
        view.systemUiVisibility = if (enabled) {
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    /**
     * Контроллеры системных панелей окон, которые участвуют в показе листа.
     *
     * Окно листа — это окно диалога, и достать его из View нельзя:
     * `ViewCompat.getWindowInsetsController` определяет окно по корню view,
     * а корнем является decorview окна листа. Панели нужно скрывать и в окне
     * листа, и в окне Activity: система учитывает запросы всех видимых окон.
     */
    @Suppress("DEPRECATION")
    private fun windowInsetsControllers(): List<WindowInsetsControllerCompat> = buildList {
        ViewCompat.getWindowInsetsController(view)?.let { add(it) }
        activityWindow()?.let { window ->
            add(WindowCompat.getInsetsController(window, view))
        }
    }

    /**
     * Окно Activity, если его удалось найти в контексте view.
     */
    private fun activityWindow(): android.view.Window? {
        var context: Context? = view.context
        while (context is ContextWrapper) {
            if (context is Activity) return context.window
            context = context.baseContext
        }
        return null
    }
}
