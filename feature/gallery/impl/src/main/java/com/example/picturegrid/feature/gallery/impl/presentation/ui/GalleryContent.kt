package com.example.picturegrid.feature.gallery.impl.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.PlaceholderReason
import com.example.picturegrid.feature.gallery.impl.R
import com.example.picturegrid.feature.gallery.impl.presentation.state.GalleryStore

/**
 * Экран галереи.
 *
 * Состояние экрана разнесено по отдельным Composable: загрузка, ошибка,
 * пустой список и сетка (INV-28). Каждое состояние можно показать в
 * превью отдельно — без DI и без сети.
 */
@Composable
internal fun GalleryContent(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader? = null,
    state: GalleryStore.State,
    onRetry: () -> Unit,
    onImageClick: (GalleryItem.Image) -> Unit,
) {
    val context = LocalContext.current
    val actualImageLoader = imageLoader ?: remember {
        ImageLoader.Builder(context).build()
    }
    // Фон рисует Compose, а не окно: так фон галереи совпадает с
    // MaterialTheme в светлой и тёмной темах.
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            GalleryHeader()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    state.isLoading && state.items.isEmpty() -> GalleryLoadingContent()
                    state.error != null && state.items.isEmpty() -> GalleryErrorContent(
                        message = state.error,
                        onRetry = onRetry,
                    )
                    // Список получен, но не содержит ни одной строки: пустая сетка
                    // без объяснения выглядит как дефект, поэтому показываем явное
                    // состояние с возможностью обновить список.
                    state.items.isEmpty() -> GalleryEmptyContent(onRefresh = onRetry)
                    else -> GallerySuccessContent(
                        items = state.items,
                        isRefreshing = state.isRefreshing,
                        refreshError = state.refreshError,
                        imageLoader = actualImageLoader,
                        onImageClick = onImageClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = stringResource(R.string.gallery_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.gallery_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryContentPreview() {
    GalleryContent(
        imageLoader = ImageLoader.Builder(LocalContext.current).build(),
        state = GalleryStore.State(
            isLoading = false,
            items = listOf(
                GalleryItem.Image("image", "https://example.com/image.png"),
                GalleryItem.Placeholder("placeholder", "text", PlaceholderReason.NotUrl),
            ),
        ),
        onRetry = {},
        onImageClick = {},
    )
}
