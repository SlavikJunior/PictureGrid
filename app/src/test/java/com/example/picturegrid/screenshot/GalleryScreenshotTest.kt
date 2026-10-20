package com.example.picturegrid.screenshot

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.picturegrid.core.designsystem.PictureGridTheme
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.PlaceholderReason
import com.example.picturegrid.feature.gallery.impl.GalleryContent
import com.example.picturegrid.feature.gallery.impl.GalleryStore
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GalleryScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fakeState = GalleryStore.State(
        items = listOf(
            GalleryItem.Image("1", "https://example.com/1.png"),
            GalleryItem.Placeholder("2", "not a url", PlaceholderReason.NotUrl),
            GalleryItem.Placeholder("3", "https://example.com/x", PlaceholderReason.NotImage),
            GalleryItem.Placeholder("4", "https://example.com/y", PlaceholderReason.LoadFailed),
        ),
        isLoading = false,
    )

    @Test
    fun `gallery light`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    GalleryContent(
                        state = fakeState,
                        onRetry = {},
                        onImageClick = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/gallery_light.png")
    }

    @Test
    fun `gallery dark`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = true, dynamicColor = false) {
                Surface {
                    GalleryContent(
                        state = fakeState,
                        onRetry = {},
                        onImageClick = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/gallery_dark.png")
    }

    @Test
    fun `gallery loading`() {
        composeRule.setContent {
            PictureGridTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    GalleryContent(
                        state = GalleryStore.State(isLoading = true),
                        onRetry = {},
                        onImageClick = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/gallery_loading.png")
    }
}
