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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import com.example.picturegrid.core.images.LocalImageLoader
import com.example.picturegrid.feature.viewer.api.domain.ViewerError
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
    error: ViewerError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = ViewerErrorMessage(error),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
            )
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
    onIntent: (ViewerStore.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ViewerPager(state = state, onIntent = onIntent)
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
    ViewerErrorContent(error = ViewerError.Network, onRetry = {})
}

@Preview(showBackground = true)
@Composable
private fun ViewerSuccessContentPreview() {
    CompositionLocalProvider(
        LocalImageLoader provides ImageLoader.Builder(LocalPlatformContext.current).build(),
    ) {
        ViewerSuccessContent(
            state = ViewerStore.State(
                urls = listOf("https://example.com/image.png"),
                index = 0,
            ),
            onIntent = {},
        )
    }
}

/**
 * Текст ошибки для пользователя.
 *
 * Строки берутся только из ресурсов: store не знает, как ошибка называется
 * на экране (INV-14, INV-20). Имя в PascalCase — требование
 * ComposeConventionTest к композируемым функциям.
 */
@Composable
internal fun ViewerErrorMessage(error: ViewerError): String = when (error) {
    ViewerError.Network -> stringResource(R.string.viewer_error_network)
    ViewerError.NotAnImage -> stringResource(R.string.viewer_error_not_image)
    ViewerError.Corrupted -> stringResource(R.string.viewer_error_corrupted)
    ViewerError.Storage -> stringResource(R.string.viewer_error_storage)
}
