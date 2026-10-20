package com.example.picturegrid

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.example.picturegrid.core.navigation.BottomSheetSceneStrategy
import com.example.picturegrid.core.navigation.PictureGridSceneStrategy
import com.example.picturegrid.core.navigation.GalleryRoute
import com.example.picturegrid.core.navigation.NavigationRouter
import com.example.picturegrid.core.navigation.ViewerRoute
import com.example.picturegrid.feature.gallery.impl.GalleryScreen
import com.example.picturegrid.feature.viewer.impl.ViewerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PictureGridTheme {
                val backStack = rememberNavBackStack(GalleryRoute)
                val router = remember(backStack) { NavigationRouter(backStack) }
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategies = listOf(PictureGridSceneStrategy()),
                    entryProvider = entryProvider {
                        entry<GalleryRoute> {
                            GalleryScreen(
                                onImageClick = { selection ->
                                    router.navigateToViewer(selection.urls, selection.index)
                                },
                            )
                        }
                        entry<ViewerRoute>(
                            metadata = BottomSheetSceneStrategy.bottomSheet(),
                        ) { key ->
                            ViewerScreen(
                                urls = key.urls,
                                initialIndex = key.index,
                                onShare = { url -> startShare(url) },
                                onOpenInBrowser = { url -> startOpenInBrowser(url) },
                            )
                        }
                    },
                )
            }
        }
    }

    /**
     * Отправка ссылки другим приложениям.
     */
    private fun startShare(url: String) {
        val chooser = Intent.createChooser(ShareIntents.createShare(url), null)
        startActivitySafely(chooser) { "Нет приложения для отправки ссылки" }
    }

    /**
     * Открытие ссылки в браузере.
     */
    private fun startOpenInBrowser(url: String) {
        startActivitySafely(ShareIntents.createOpenInBrowser(url)) { "Нет приложения для открытия ссылки" }
    }

    /**
     * Запуск действия с понятным сообщением, если подходящего приложения нет.
     */
    private fun startActivitySafely(intent: Intent, errorMessage: () -> String) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, errorMessage(), Toast.LENGTH_SHORT).show()
        }
    }

}
