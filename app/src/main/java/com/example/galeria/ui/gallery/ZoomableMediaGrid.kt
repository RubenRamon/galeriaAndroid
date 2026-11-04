package com.example.galeria.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.thumbnails.ThumbnailLoader
import com.example.galeria.domain.model.GalleryEntry
import com.example.galeria.ui.components.MediaThumbnail
import com.example.galeria.ui.components.VideoDurationLabel
import com.example.galeria.ui.theme.GalleryGreyDark

/**
 * Cuadrícula de fotos y vídeos con zoom por pellizco, reutilizable en la galería
 * cronológica y en el contenido de los álbumes.
 *
 * @param entriesFor lista a mostrar según si se usan encabezados de año (true) o de mes (false).
 * @param leadingItemCount número de elementos que añade [leadingContent] antes de la lista.
 * @param onMediaClick se llama solo en modo normal; en modo compacto, pulsar amplía la cuadrícula.
 */
@Composable
fun ZoomableMediaGrid(
    zoomState: GridZoomState,
    entriesFor: (yearHeaders: Boolean) -> List<GalleryEntry>,
    leadingItemCount: Int,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    leadingContent: LazyGridScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val columns = zoomState.columns
    val showVideoInfo = columns < GalleryConfig.HIDE_VIDEO_INFO_MIN_COLUMNS
    val entries = entriesFor(zoomState.showsYearHeaders)

    SideEffect {
        zoomState.gridState = gridState
        zoomState.entriesProvider = entriesFor
        zoomState.leadingCount = leadingItemCount
    }

    val handleClick: (MediaItem) -> Unit = remember(zoomState, onMediaClick, scope) {
        { item ->
            if (zoomState.isCompact) {
                zoomState.zoomInFrom(scope, item.key)
            } else {
                onMediaClick(item)
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        // Tamaño de miniatura que se pide al sistema según el ancho real de cada celda.
        val thumbSizePx = remember(maxWidth, columns, density) {
            ThumbnailLoader.bucketFor(with(density) { (maxWidth / columns).roundToPx() })
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            contentPadding = WindowInsets.systemBars.asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(GalleryConfig.CELL_SPACING),
            verticalArrangement = Arrangement.spacedBy(GalleryConfig.CELL_SPACING),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomState) { detectGridPinch(zoomState) }
        ) {
            leadingContent()

            items(
                items = entries,
                key = { it.key },
                span = { entry ->
                    GridItemSpan(if (entry is GalleryEntry.Header) maxLineSpan else 1)
                },
                contentType = { it::class }
            ) { entry ->
                when (entry) {
                    is GalleryEntry.Header -> SectionHeader(entry.title)
                    is GalleryEntry.Media -> MediaCell(entry.item, thumbSizePx, showVideoInfo, handleClick)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 10.dp)
    )
}

@Composable
private fun MediaCell(
    item: MediaItem,
    thumbSizePx: Int,
    showVideoInfo: Boolean,
    onClick: (MediaItem) -> Unit
) {
    Box(
        Modifier
            .aspectRatio(1f)
            .background(GalleryGreyDark)
            .clickable { onClick(item) }
    ) {
        MediaThumbnail(item = item, sizePx = thumbSizePx, modifier = Modifier.fillMaxSize())
        if (item.isVideo && showVideoInfo) {
            VideoDurationLabel(
                durationMs = item.durationMs,
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
            )
        }
    }
}
