package com.example.picturegrid.feature.viewer.impl.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.ImageLoader
import com.example.picturegrid.feature.viewer.impl.R
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStore

/**
 * Перелистывание изображений.
 *
 * Открытое изображение показывается оригиналом из хранилища, соседние —
 * превью Coil: так при листании видно, что находится рядом.
 */
@Composable
internal fun ViewerPager(
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
        // Ключ страницы: без него при перелистывании страницы пересоздаются
        // и заново грузят картинки (INV-16).
        key = { page -> state.urls[page] },
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
