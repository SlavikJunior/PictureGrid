package com.example.picturegrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import coil3.SingletonImageLoader
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.example.picturegrid.core.images.ImageLoaderFactory
import com.example.picturegrid.core.navigation.BottomSheetSceneStrategy
import com.example.picturegrid.core.navigation.PicGridSceneStrategy
import com.example.picturegrid.core.navigation.GalleryRoute
import com.example.picturegrid.core.navigation.NavigationRouter
import com.example.picturegrid.core.navigation.ViewerRoute
import com.example.picturegrid.feature.gallery.impl.GalleryScreen
import com.example.picturegrid.feature.viewer.impl.ViewerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SingletonImageLoader.setSafe { context -> ImageLoaderFactory.create(context) }
        setContent {
            PictureGridTheme {
                val backStack = rememberNavBackStack(GalleryRoute)
                val router = remember(backStack) { NavigationRouter(backStack) }
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategies = listOf(PicGridSceneStrategy()),
                    entryProvider = entryProvider {
                        entry<GalleryRoute> { GalleryScreen(onImageClick = router::navigateToViewer) }
                        entry<ViewerRoute>(
                            metadata = BottomSheetSceneStrategy.bottomSheet(),
                        ) { key ->
                            ViewerScreen(url = key.url)
                        }
                    },
                )
            }
        }
    }
}
