package com.example.picturegrid

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.example.picturegrid.core.images.LocalImageLoader
import com.example.picturegrid.core.navigation.PictureGridSceneStrategy
import com.example.picturegrid.feature.gallery.api.navigation.GalleryRoute
import com.example.picturegrid.feature.gallery.impl.galleryNavEntry
import com.example.picturegrid.feature.viewer.impl.viewerNavEntry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val shareIntents: ShareIntents by inject()
    private val imageLoader: ImageLoader by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PictureGridTheme {
                // Единственный ImageLoader приложения: фичи получают его из
                // CompositionLocal, а не тащат через параметры Composable.
                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    val backStack = rememberNavBackStack(GalleryRoute)
                    val router = remember(backStack) { NavigationRouter(backStack) }
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        sceneStrategies = listOf(PictureGridSceneStrategy()),
                        entryProvider = entryProvider {
                            galleryNavEntry(
                                onImageClick = { selection ->
                                    router.navigateToViewer(selection.urls, selection.index)
                                },
                            )
                            viewerNavEntry(
                                selectionProvider = router::viewerSelection,
                                onShare = ::startShare,
                                onOpenInBrowser = ::startOpenInBrowser,
                            )
                        },
                    )
                }
            }
        }
    }

    /**
     * Отправка ссылки другим приложениям.
     */
    private fun startShare(url: String) {
        val chooser = Intent.createChooser(shareIntents.createShare(url), null)
        startActivitySafely(chooser) { getString(R.string.app_error_no_share_app) }
    }

    /**
     * Открытие ссылки в браузере.
     */
    private fun startOpenInBrowser(url: String) {
        startActivitySafely(shareIntents.createOpenInBrowser(url)) {
            getString(R.string.app_error_no_browser_app)
        }
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
