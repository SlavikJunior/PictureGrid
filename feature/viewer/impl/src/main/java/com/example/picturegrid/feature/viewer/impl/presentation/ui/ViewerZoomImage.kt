package com.example.picturegrid.feature.viewer.impl.presentation.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.example.picturegrid.core.images.LocalImageLoader
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStore
import java.io.File

/**
 * Изображение с поддержкой масштабирования щипком и перетаскивания.
 *
 * Масштаб и смещение хранятся в Store, поэтому состояние не теряется при
 * перерисовке и может быть восстановлено при повторном открытии.
 */
@Composable
internal fun ZoomableImage(
    file: File,
    zoom: ViewerStore.Zoom,
    onIntent: (ViewerStore.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageLoader = LocalImageLoader.current
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }
    val currentIntent by rememberUpdatedState(onIntent)
    val currentScale by rememberUpdatedState(zoom.scale)

    // Пока изображение не увеличено, перетаскивание не перехватывается:
    // иначе горизонтальный свайп не доходил бы до пагера.
    val canPan = remember { { _: Offset -> currentScale > ViewerZoom.MIN_SCALE } }

    val transformableState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        currentIntent(
            ViewerStore.Intent.ZoomGesture(
                zoomChange = zoomChange,
                panX = panChange.x,
                panY = panChange.y,
                centroidX = centroid.x,
                centroidY = centroid.y,
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerWidth = size.width.toFloat()
                containerHeight = size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentIntent(ViewerStore.Intent.ToggleFullscreen) },
                    onDoubleTap = { currentIntent(ViewerStore.Intent.ToggleZoom) },
                )
            }
            .transformable(transformableState, canPan = canPan)
            // Метка нужна инструментальному тесту: по ней находится узел,
            // на котором выполняется жест масштабирования.
            .testTag(ZOOMABLE_TEST_TAG),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onState = { painterState ->
                if (painterState is AsyncImagePainter.State.Success) {
                    val intrinsic = painterState.painter.intrinsicSize
                    imageWidth = intrinsic.width
                    imageHeight = intrinsic.height
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offsetX
                    translationY = zoom.offsetY
                },
        )
    }
}

/**
 * Метка узла, на котором выполняется жест масштабирования.
 *
 * Используется инструментальным тестом: без метки нельзя найти узел и
 * выполнить щипок.
 */
internal const val ZOOMABLE_TEST_TAG = "viewer_zoom_image"
