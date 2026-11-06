package com.example.galeria.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.galeria.data.ViewerImageDecoder
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.components.LocalThumbnailLoader
import com.example.galeria.ui.components.VideoBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PLACEHOLDER_SIZE = 512
private const val DISMISS_ANIMATION_MS = 160
private const val CONTROLS_ANIMATION_MS = 160

/** Espera antes de decodificar la foto completa: evita decodificar al pasar rápido por el carrusel. */
private const val FULL_DECODE_DELAY_MS = 120L

/**
 * Visor a pantalla completa en modo de visualización limpia: sin ningún control a la vista y con las
 * barras del sistema ocultas. Un toque muestra los controles (carrusel, compartir, información y, en
 * vídeos, barra de progreso y pausa); otro toque en una zona libre los oculta.
 * Se puede deslizar a izquierda y derecha entre los elementos de [items] y cerrar arrastrando hacia abajo.
 * Las fotos admiten zoom y doble toque.
 */
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    startIndex: Int,
    onClose: () -> Unit,
    favoriteKeys: Set<String>,
    onToggleFavorite: (MediaItem) -> Unit,
    onTrash: (MediaItem) -> Unit,
    onRestore: (MediaItem) -> Unit,
    onDeleteForever: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    trashMode: Boolean = false
) {
    val context = LocalContext.current
    BackHandler(onBack = onClose)

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
        pageCount = { items.size }
    )
    val scope = rememberCoroutineScope()

    var zoomed by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(false) } // visualización limpia al abrir
    var infoOpen by remember { mutableStateOf(false) }
    var activePlayer by remember { mutableStateOf<Player?>(null) }

    // Atrás cierra primero el panel de información, si está abierto.
    BackHandler(enabled = infoOpen) { infoOpen = false }

    // Barras del sistema ocultas en visualización limpia; visibles junto a los controles.
    ViewerSystemBars(hidden = !controlsVisible)

    // Al cambiar de elemento, el zoom de la foto anterior ya no cuenta.
    LaunchedEffect(pagerState.settledPage) { zoomed = false }

    val toggleControls: () -> Unit = remember { { controlsVisible = !controlsVisible } }
    val currentItem = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]

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
                    isEnabled = { !zoomed && !infoOpen },
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
                VideoPage(
                    item = item,
                    isActive = pagerState.settledPage == page,
                    onTap = toggleControls,
                    onPlayerReady = { activePlayer = it },
                    onPlayerReleased = { if (activePlayer === it) activePlayer = null }
                )
            } else {
                PhotoPage(item = item, onZoomedChange = { zoomed = it }, onTap = toggleControls)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(CONTROLS_ANIMATION_MS)),
            exit = fadeOut(tween(CONTROLS_ANIMATION_MS)),
            modifier = Modifier
                .fillMaxSize()
                // Los controles se desvanecen mientras se arrastra para cerrar el visor.
                .graphicsLayer {
                    val progress = if (size.height > 0f) dragOffset / size.height else 0f
                    alpha = (1f - progress * 3f).coerceIn(0f, 1f)
                }
        ) {
            ViewerControlsLayer(
                items = items,
                pagerState = pagerState,
                currentItem = currentItem,
                player = activePlayer,
                isFavorite = currentItem.key in favoriteKeys,
                onShare = { shareMedia(context, currentItem) },
                onToggleFavorite = { onToggleFavorite(currentItem) },
                trashMode = trashMode,
                onTrash = { onTrash(currentItem) },
                onRestore = { onRestore(currentItem) },
                onDeleteForever = { onDeleteForever(currentItem) },
                onInfo = { infoOpen = true }
            )
        }

        MediaInfoPanel(item = currentItem, visible = infoOpen, onDismiss = { infoOpen = false })
    }
}

@Composable
private fun PhotoPage(item: MediaItem, onZoomedChange: (Boolean) -> Unit, onTap: () -> Unit) {
    val context = LocalContext.current
    val loader = LocalThumbnailLoader.current

    val image by produceState<ImageBitmap?>(initialValue = null, item.key) {
        // Primero la miniatura del sistema al instante; después, la foto a tamaño de pantalla.
        value = (loader.getCached(item, PLACEHOLDER_SIZE) ?: loader.load(item, PLACEHOLDER_SIZE))
            ?.asImageBitmap()
        delay(FULL_DECODE_DELAY_MS)
        val full = withContext(Dispatchers.IO) {
            ViewerImageDecoder.decode(context.contentResolver, item.uri)
        }
        if (full != null) value = full.asImageBitmap()
    }

    ZoomableImage(
        image = image,
        modifier = Modifier.fillMaxSize(),
        onZoomedChange = onZoomedChange,
        onTap = onTap
    )
}

@Composable
private fun VideoPage(
    item: MediaItem,
    isActive: Boolean,
    onTap: () -> Unit,
    onPlayerReady: (Player) -> Unit,
    onPlayerReleased: (Player) -> Unit
) {
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
            VideoPlayer(
                uri = item.uri,
                onPlayerReady = onPlayerReady,
                onPlayerReleased = onPlayerReleased,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            VideoBadge(Modifier.align(Alignment.Center), size = 64.dp)
        }
        // Capa transparente que recoge el toque para mostrar u ocultar los controles.
        // Un toque NO pausa el vídeo: la pausa solo la gestiona el botón de los controles.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
        )
    }
}
