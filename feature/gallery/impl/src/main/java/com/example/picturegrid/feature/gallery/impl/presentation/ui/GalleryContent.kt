package com.example.picturegrid.feature.gallery.impl.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import com.example.picturegrid.core.designsystem.PictureGridTopAppBar
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
@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Фон рисует Scaffold, а не окно: так фон галереи совпадает с
    // MaterialTheme в светлой и тёмной темах.
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PictureGridTopAppBar(
                title = stringResource(R.string.gallery_title),
                subtitle = stringResource(R.string.gallery_subtitle),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Панель сворачивается по прокрутке сетки: связь передаётся
                // снизу вверх, сама сетка о поведении не знает.
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> GalleryLoadingContent()
                state.error != null && state.items.isEmpty() -> GalleryErrorContent(
                    error = state.error,
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
