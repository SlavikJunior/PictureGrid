package com.example.picturegrid.feature.gallery.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.PlaceholderReason

@Composable
fun GalleryContent(
    state: GalleryStore.State,
    onRetry: () -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Фон рисует Compose, а не окно: так фон галереи совпадает с
    // MaterialTheme в светлой и тёмной темах.
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
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
                            Text("Повторить")
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
                        Text("Список пуст", textAlign = TextAlign.Center)
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                            Text("Обновить")
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
                    items(state.items, key = { it.id }) { item ->
                        GalleryCell(
                            item = item,
                            isRefreshing = state.isRefreshing,
                            onImageClick = onImageClick,
                        )
                    }
                }
                if (state.refreshError != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Text(
                            text = state.refreshError,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryCell(
    item: GalleryItem,
    isRefreshing: Boolean,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        when (item) {
            is GalleryItem.Image -> ImageCell(item = item, onImageClick = onImageClick)
            is GalleryItem.Placeholder -> PlaceholderCell(reason = item.reason)
        }
    }
}

@Composable
private fun ImageCell(
    item: GalleryItem.Image,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var retryKey by remember { mutableIntStateOf(0) }
    var failure by remember { mutableStateOf<ImageCellFailure?>(null) }
    Box(modifier.fillMaxSize().clickable { onImageClick(item.source) }) {
        key(retryKey) {
            AsyncImage(
                model = item.source,
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
            ) {
                Text("Повторить")
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
        PlaceholderReason.NotUrl -> "Не ссылка"
        PlaceholderReason.NotImage -> "Не изображение"
        PlaceholderReason.LoadFailed -> "Ошибка загрузки"
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, textAlign = TextAlign.Center)
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
