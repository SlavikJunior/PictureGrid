package com.example.picturegrid.feature.viewer.impl.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.example.picturegrid.feature.viewer.impl.R
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStore

/**
 * Состояния экрана просмотра.
 *
 * Каждое состояние — отдельный Composable с собственным превью.
 */

/** Оригинал загружается. */
@Composable
internal fun ViewerLoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Оригинал не загрузился: сообщение и действие «Повторить». */
@Composable
internal fun ViewerErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.viewer_retry))
            }
        }
    }
}

/** Оригинал загружен: перелистывание изображений. */
@Composable
internal fun ViewerSuccessContent(
    state: ViewerStore.State,
    imageLoader: ImageLoader,
    onIntent: (ViewerStore.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ViewerPager(state = state, imageLoader = imageLoader, onIntent = onIntent)
    }
}

@Preview(showBackground = true)
@Composable
private fun ViewerLoadingContentPreview() {
    ViewerLoadingContent()
}

@Preview(showBackground = true)
@Composable
private fun ViewerErrorContentPreview() {
    ViewerErrorContent(message = "Ошибка загрузки", onRetry = {})
}

@Preview(showBackground = true)
@Composable
private fun ViewerSuccessContentPreview() {
    ViewerSuccessContent(
        state = ViewerStore.State(urls = listOf("https://example.com/image.png"), index = 0),
        imageLoader = ImageLoader.Builder(LocalContext.current).build(),
        onIntent = {},
    )
}
