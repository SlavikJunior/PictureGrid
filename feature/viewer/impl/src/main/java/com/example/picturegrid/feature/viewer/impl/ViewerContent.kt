package com.example.picturegrid.feature.viewer.impl

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.ImageLoader
import java.io.File

@Composable
internal fun ViewerContent(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader? = null,
    state: ViewerStore.State,
    onIntent: (ViewerStore.Intent) -> Unit,
    onShare: (String) -> Unit = {},
    onOpenInBrowser: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val actualImageLoader = imageLoader ?: remember {
        ImageLoader.Builder(context).build()
    }
    // Фон рисует Compose, а не окно: так фон просмотра совпадает с
    // MaterialTheme в светлой и тёмной темах.
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            // В полноэкранном режиме панель действий скрыта.
            if (!state.isFullscreen) {
                ViewerActions(
                    onShare = { onShare(state.url) },
                    onOpenInBrowser = { onOpenInBrowser(state.url) },
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ViewerPager(state = state, imageLoader = actualImageLoader, onIntent = onIntent)
            }
        }
    }
}

/**
 * Перелистывание изображений.
 *
 * Открытое изображение показывается оригиналом из хранилища, соседние —
 * превью Coil: так при листании видно, что находится рядом.
 */
@Composable
private fun ViewerPager(
    state: ViewerStore.State,
    imageLoader: ImageLoader,
    onIntent: (ViewerStore.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.urls.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = state.index) { state.urls.size }
    val currentIntent by rememberUpdatedState(onIntent)

    // Страница пагера и состояние Store должны совпадать: Store — источник
    // истины, пагер подстраивается под него.
    LaunchedEffect(state.index) {
        if (pagerState.currentPage != state.index) {
            pagerState.scrollToPage(state.index)
        }
    }

    // Перелистывание меняет открытое изображение: страница отслеживается
    // после того, как она «устоялась», чтобы не дёргать загрузку в середине
    // жеста.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page -> currentIntent(ViewerStore.Intent.Select(page)) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        if (page == state.index) {
            CurrentImage(state = state, imageLoader = imageLoader, onIntent = onIntent)
        } else {
            AsyncImage(
                model = state.urls[page],
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Открытое изображение: загрузка, ошибка с повтором или оригинал с
 * поддержкой масштабирования.
 */
@Composable
private fun CurrentImage(
    state: ViewerStore.State,
    imageLoader: ImageLoader,
    onIntent: (ViewerStore.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading && state.file == null -> CircularProgressIndicator()
            state.error != null && state.file == null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { onIntent(ViewerStore.Intent.Retry) }, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.viewer_retry))
                    }
                }
            }
            state.file != null -> ZoomableImage(
                file = state.file,
                zoom = state.zoom,
                onIntent = onIntent,
                imageLoader = imageLoader,
            )
        }
    }
}

/**
 * Действия с изображением: поделиться ссылкой и открыть её в браузере.
 */
@Composable
private fun ViewerActions(
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            TextButton(onClick = onShare) {
                Text(stringResource(R.string.viewer_share))
            }
            TextButton(onClick = onOpenInBrowser) {
                Text(stringResource(R.string.viewer_open_in_browser))
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
    imageLoader: ImageLoader,
    zoom: ViewerStore.Zoom,
    onIntent: (ViewerStore.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
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

@Preview(showBackground = true)
@Composable
private fun ViewerContentPreview() {
    ViewerContent(
        imageLoader = ImageLoader.Builder(androidx.compose.ui.platform.LocalContext.current).build(),
        state = ViewerStore.State(urls = listOf("https://example.com/image.png"), index = 0),
        onIntent = {},
    )
}
