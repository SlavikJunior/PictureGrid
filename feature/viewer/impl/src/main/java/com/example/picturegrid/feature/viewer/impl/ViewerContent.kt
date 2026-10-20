package com.example.picturegrid.feature.viewer.impl

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import java.io.File

@Composable
fun ViewerContent(
    state: ViewerStore.State,
    onIntent: (ViewerStore.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Фон рисует Compose, а не окно: так фон просмотра совпадает с
    // MaterialTheme в светлой и тёмной темах.
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                state.isLoading && state.file == null -> CircularProgressIndicator()
                state.error != null && state.file == null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { onIntent(ViewerStore.Intent.Retry) }, modifier = Modifier.padding(top = 12.dp)) {
                            Text("Повторить")
                        }
                    }
                }
                state.file != null -> ZoomableImage(
                    file = state.file,
                    zoom = state.zoom,
                    onIntent = onIntent,
                )
            }
        }
    }
}

/**
 * Изображение с поддержкой масштабирования щипком и перетаскивания.
 *
 * Масштаб и смещение хранятся в Store, поэтому состояние не теряется при
 * перерисовке и может быть восстановлено при повторном открытии.
 */
@Composable
private fun ZoomableImage(
    file: File,
    zoom: ViewerStore.Zoom,
    onIntent: (ViewerStore.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }
    val currentIntent by rememberUpdatedState(onIntent)

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
                detectTapGestures(onDoubleTap = { currentIntent(ViewerStore.Intent.ToggleZoom) })
            }
            .transformable(transformableState),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file,
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
