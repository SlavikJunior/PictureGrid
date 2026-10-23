package com.example.picturegrid.feature.gallery.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.picturegrid.feature.gallery.api.domain.GalleryError
import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.NetworkFailure
import com.example.picturegrid.feature.gallery.api.domain.PlaceholderReason
import com.example.picturegrid.feature.gallery.impl.R
import com.example.picturegrid.feature.gallery.impl.presentation.state.GalleryStore

/**
 * Состояния экрана галереи.
 *
 * Каждое состояние — отдельный Composable с собственным превью: так видно,
 * как экран выглядит при загрузке, ошибке, пустом списке и сетке.
 */

/** Список загружается. */
@Composable
internal fun GalleryLoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Список не загрузился: сообщение и действие «Повторить». */
@Composable
internal fun GalleryErrorContent(
    error: GalleryError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ColumnCentered {
            Text(GalleryErrorMessage(error), textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.gallery_retry))
            }
        }
    }
}

/**
 * Текст ошибки для пользователя.
 *
 * Строки берутся только из ресурсов: store и репозиторий не знают, как
 * ошибка называется на экране (INV-14, INV-20). Имя в PascalCase, потому что
 * функция композируемая — этого требует ComposeConventionTest.
 */
@Composable
internal fun GalleryErrorMessage(error: GalleryError): String = when (error) {
    is GalleryError.Network -> when (error.reason) {
        NetworkFailure.IO -> stringResource(R.string.gallery_error_io)
        NetworkFailure.TIMEOUT -> stringResource(R.string.gallery_error_timeout)
        NetworkFailure.EMPTY_BODY -> stringResource(R.string.gallery_error_empty_body)
        NetworkFailure.HTTP -> stringResource(R.string.gallery_error_http, error.httpCode ?: 0)
        NetworkFailure.TOO_LARGE -> stringResource(R.string.gallery_error_too_large)
    }
    is GalleryError.Storage -> stringResource(R.string.gallery_error_storage)
}

/** Список пуст: предложение обновить. */
@Composable
internal fun GalleryEmptyContent(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ColumnCentered {
            Text(stringResource(R.string.gallery_empty), textAlign = TextAlign.Center)
            Button(onClick = onRefresh, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.gallery_refresh))
            }
        }
    }
}

/** Сетка элементов и баннер ошибки фонового обновления. */
@Composable
internal fun GallerySuccessContent(
    state: GalleryStore.State,
    onImageClick: (GalleryItem.Image) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(
                items = state.items,
                key = { it.id },
                contentType = { item ->
                    when (item) {
                        is GalleryItem.Image -> "image"
                        is GalleryItem.Placeholder -> "placeholder"
                    }
                },
            ) { item ->
                GalleryCell(
                    item = item,
                    onImageClick = onImageClick,
                )
            }
        }
        // Ссылка на параметр стабилизируется локальной переменной: иначе
        // внутри лямбды умное приведение к ненулевому типу недоступно.
        val bannerError = state.refreshError
        if (bannerError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                // Фон обязателен: без него сообщение ложилось прямо на
                // превью и не читалось.
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = GalleryErrorMessage(bannerError),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnCentered(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryLoadingContentPreview() {
    GalleryLoadingContent()
}

@Preview(showBackground = true)
@Composable
private fun GalleryErrorContentPreview() {
    GalleryErrorContent(
        error = GalleryError.Network(NetworkFailure.IO, retryable = true),
        onRetry = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun GalleryEmptyContentPreview() {
    GalleryEmptyContent(onRefresh = {})
}

@Preview(showBackground = true)
@Composable
private fun GallerySuccessContentPreview() {
    GallerySuccessContent(
        state = GalleryStore.State(
            items = listOf(
                GalleryItem.Image("image", "https://example.com/image.png"),
                GalleryItem.Placeholder("placeholder", "text", PlaceholderReason.NotUrl),
            ),
        ),
        onImageClick = {},
    )
}
