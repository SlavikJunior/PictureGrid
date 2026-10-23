package com.example.picturegrid.feature.viewer.impl.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.picturegrid.feature.viewer.impl.presentation.state.ViewerStore

/**
 * Экран просмотра оригинала.
 *
 * Состояния разнесены по отдельным Composable: загрузка, ошибка и
 * изображение (INV-28). Каждое состояние показывается в превью отдельно.
 */
@Composable
internal fun ViewerContent(
    modifier: Modifier = Modifier,
    state: ViewerStore.State,
    onIntent: (ViewerStore.Intent) -> Unit,
    onShare: (String) -> Unit = {},
    onOpenInBrowser: (String) -> Unit = {},
) {
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
                when {
                    state.isLoading && state.file == null -> ViewerLoadingContent()
                    state.error != null && state.file == null -> ViewerErrorContent(
                        error = state.error,
                        onRetry = { onIntent(ViewerStore.Intent.Retry) },
                    )
                    else -> ViewerSuccessContent(
                        state = state,
                        onIntent = onIntent,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ViewerContentPreview() {
    ViewerContent(
        state = ViewerStore.State(urls = listOf("https://example.com/image.png"), index = 0),
        onIntent = {},
    )
}
