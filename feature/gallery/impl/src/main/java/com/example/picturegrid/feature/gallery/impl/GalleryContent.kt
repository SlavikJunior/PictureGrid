package com.example.picturegrid.feature.gallery.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.ImageLoader
import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.PlaceholderReason

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
            state.isLoading && state.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ColumnCentered {
                        Text(state.error, textAlign = TextAlign.Center)
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                            Text(stringResource(R.string.gallery_retry))
                        }
                    }
                }
            }
            // Список получен, но не содержит ни одной строки: пустая сетка
            // без объяснения выглядит как дефект, поэтому показываем явное
            // состояние с возможностью обновить список.
            state.items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ColumnCentered {
                        Text(stringResource(R.string.gallery_empty), textAlign = TextAlign.Center)
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                            Text(stringResource(R.string.gallery_refresh))
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
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
                            imageLoader = actualImageLoader,
                            isRefreshing = state.isRefreshing,
                            onImageClick = onImageClick,
                        )
                    }
                }
                if (state.refreshError != null) {
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
                                text = state.refreshError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
                }
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

@Composable
private fun GalleryCell(
    item: GalleryItem,
    imageLoader: ImageLoader,
    isRefreshing: Boolean,
    onImageClick: (GalleryItem.Image) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        when (item) {
            is GalleryItem.Image -> ImageCell(item = item, imageLoader = imageLoader, onImageClick = onImageClick)
            is GalleryItem.Placeholder -> PlaceholderCell(reason = item.reason)
        }
    }
}

@Composable
private fun ImageCell(
    item: GalleryItem.Image,
    imageLoader: ImageLoader,
    onImageClick: (GalleryItem.Image) -> Unit,
    modifier: Modifier = Modifier,
) {
    var retryKey by remember { mutableIntStateOf(0) }
    var failure by remember { mutableStateOf<ImageCellFailure?>(null) }
    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onImageClick(item) },
    ) {
        key(retryKey) {
            AsyncImage(
                model = item.source,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    failure = if (state is coil3.compose.AsyncImagePainter.State.Error) {
                        classifyImageCellFailure(state.result.throwable)
                    } else {
                        null
                    }
                },
            )
        }
        when (failure) {
            // Ресурс не является изображением: повтор ничего не изменит,
            // поэтому показывается заглушка, а не кнопка «Повторить».
            ImageCellFailure.NotImage -> PlaceholderCell(reason = PlaceholderReason.NotImage)
            ImageCellFailure.LoadFailed -> Button(
                onClick = { retryKey++; failure = null },
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(14.dp),
                // Ячейка узкая: без этих параметров подпись переносилась
                // по-середине слова («Повтори / ть»).
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.gallery_retry), maxLines = 1, softWrap = false)
            }
            null -> Unit
        }
    }
}

@Composable
private fun PlaceholderCell(
    reason: PlaceholderReason,
    modifier: Modifier = Modifier,
) {
    val label = when (reason) {
        PlaceholderReason.NotUrl -> stringResource(R.string.gallery_not_url)
        PlaceholderReason.NotImage -> stringResource(R.string.gallery_not_image)
        PlaceholderReason.LoadFailed -> stringResource(R.string.gallery_load_failed)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryContentPreview() {
    GalleryContent(
        imageLoader = ImageLoader.Builder(androidx.compose.ui.platform.LocalContext.current).build(),
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

@Composable
private fun ColumnCentered(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}
