package com.example.picturegrid.feature.gallery.impl.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.ImageLoader
import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.PlaceholderReason
import com.example.picturegrid.feature.gallery.impl.R

/**
 * Ячейка сетки галереи: изображение, заглушка или действие «Повторить».
 */
@Composable
internal fun GalleryCell(
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
