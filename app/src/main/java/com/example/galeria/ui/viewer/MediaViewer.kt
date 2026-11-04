package com.example.galeria.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.galeria.R
import com.example.galeria.data.ViewerImageDecoder
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.components.LocalThumbnailLoader
import com.example.galeria.ui.components.VideoBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Visor a pantalla completa. Se puede deslizar a izquierda y derecha entre los
 * elementos de [items]. Las fotos admiten zoom; los vídeos se reproducen con controles.
 */
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    startIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
        pageCount = { items.size }
    )

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {} // evita que los toques lleguen a la galería de debajo
    ) {
        HorizontalPager(
            state = pagerState,
            key = { items[it].key },
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                // Solo el vídeo de la página en reposo tiene reproductor, para no crear varios a la vez.
                VideoPage(item = item, isActive = pagerState.settledPage == page)
            } else {
                PhotoPage(item)
            }
        }

        CloseButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(8.dp)
        )
    }
}

@Composable
private fun PhotoPage(item: MediaItem) {
    val context = LocalContext.current
    val loader = LocalThumbnailLoader.current

    val image by produceState<ImageBitmap?>(initialValue = null, item.key) {
        // 1) Miniatura del sistema al instante, 2) foto a tamaño de pantalla cuando esté lista.
        value = (loader.getCached(item, PLACEHOLDER_SIZE) ?: loader.load(item, PLACEHOLDER_SIZE))
            ?.asImageBitmap()
        val full = withContext(Dispatchers.IO) {
            ViewerImageDecoder.decode(context.contentResolver, item.uri)
        }
        if (full != null) value = full.asImageBitmap()
    }

    ZoomableImage(image = image, modifier = Modifier.fillMaxSize())
}

@Composable
private fun VideoPage(item: MediaItem, isActive: Boolean) {
    Box(Modifier.fillMaxSize()) {
        if (isActive) {
            VideoPlayer(uri = item.uri, modifier = Modifier.fillMaxSize())
        } else {
            val loader = LocalThumbnailLoader.current
            val thumb by produceState<ImageBitmap?>(initialValue = null, item.key) {
                value = (loader.getCached(item, PLACEHOLDER_SIZE) ?: loader.load(item, PLACEHOLDER_SIZE))
                    ?.asImageBitmap()
            }
            thumb?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            VideoBadge(Modifier.align(Alignment.Center), size = 64.dp)
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.viewer_close)
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(Color.White, Offset(0f, 0f), Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}

private const val PLACEHOLDER_SIZE = 512
