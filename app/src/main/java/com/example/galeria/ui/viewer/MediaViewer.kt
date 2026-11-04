package com.example.galeria.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PLACEHOLDER_SIZE = 512
private const val DISMISS_ANIMATION_MS = 160

/**
 * Visor a pantalla completa. Se puede deslizar a izquierda y derecha entre los elementos de
 * [items], y cerrarlo con la X, el botón atrás o arrastrando hacia abajo.
 * Las fotos admiten zoom; los vídeos se reproducen con controles.
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
    val scope = rememberCoroutineScope()

    var zoomed by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }

    // Al cambiar de elemento, el zoom de la foto anterior ya no cuenta.
    LaunchedEffect(pagerState.settledPage) { zoomed = false }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { heightPx = it.height.toFloat() }
            // El fondo se aclara al arrastrar, dejando ver la galería de debajo.
            .drawBehind {
                val progress = if (size.height > 0f) (dragOffset / size.height).coerceIn(0f, 1f) else 0f
                drawRect(Color.Black, alpha = (1f - progress * 1.5f).coerceIn(0f, 1f))
            }
            .pointerInput(Unit) {
                detectSwipeDownToDismiss(
                    isEnabled = { !zoomed },
                    onDrag = { dragOffset = it },
                    onEnd = { dismiss ->
                        scope.launch {
                            if (dismiss) {
                                animate(dragOffset, heightPx, animationSpec = tween(DISMISS_ANIMATION_MS)) { v, _ ->
                                    dragOffset = v
                                }
                                onClose()
                            } else {
                                animate(dragOffset, 0f, animationSpec = tween(DISMISS_ANIMATION_MS)) { v, _ ->
                                    dragOffset = v
                                }
                            }
                        }
                    }
                )
            }
    ) {
        HorizontalPager(
            state = pagerState,
            key = { items[it].key },
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = dragOffset }
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                // Solo el vídeo de la página en reposo tiene reproductor, para no crear varios a la vez.
                VideoPage(item = item, isActive = pagerState.settledPage == page)
            } else {
                PhotoPage(item = item, onZoomedChange = { zoomed = it })
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
private fun PhotoPage(item: MediaItem, onZoomedChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val loader = LocalThumbnailLoader.current

    val image by produceState<ImageBitmap?>(initialValue = null, item.key) {
        // Primero la miniatura del sistema al instante; después, la foto a tamaño de pantalla.
        value = (loader.getCached(item, PLACEHOLDER_SIZE) ?: loader.load(item, PLACEHOLDER_SIZE))
            ?.asImageBitmap()
        val full = withContext(Dispatchers.IO) {
            ViewerImageDecoder.decode(context.contentResolver, item.uri)
        }
        if (full != null) value = full.asImageBitmap()
    }

    ZoomableImage(
        image = image,
        modifier = Modifier.fillMaxSize(),
        onZoomedChange = onZoomedChange
    )
}

@Composable
private fun VideoPage(item: MediaItem, isActive: Boolean) {
    val loader = LocalThumbnailLoader.current
    val thumb by produceState<ImageBitmap?>(initialValue = null, item.key) {
        value = (loader.getCached(item, PLACEHOLDER_SIZE) ?: loader.load(item, PLACEHOLDER_SIZE))
            ?.asImageBitmap()
    }

    Box(Modifier.fillMaxSize()) {
        // La miniatura queda debajo: se ve mientras el vídeo prepara su primer fotograma.
        thumb?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isActive) {
            VideoPlayer(uri = item.uri, modifier = Modifier.fillMaxSize())
        } else {
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
