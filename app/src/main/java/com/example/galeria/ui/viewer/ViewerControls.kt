@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.galeria.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.galeria.R
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.components.drawHeartIcon
import com.example.galeria.ui.components.drawTrashIcon
import com.example.galeria.ui.components.formatDuration

/**
 * Capa de controles del visor. Solo existe mientras los controles están visibles.
 * Contenido (de arriba abajo): botón de pausa central (vídeos), barra de progreso (vídeos),
 * carrusel de miniaturas y fila de botones.
 */
@Composable
internal fun ViewerControlsLayer(
    items: List<MediaItem>,
    pagerState: PagerState,
    currentItem: MediaItem,
    player: Player?,
    isFavorite: Boolean,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfo: () -> Unit,
    trashMode: Boolean,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerUi = if (player != null) rememberPlayerUi(player) else null
    val showVideoControls = currentItem.isVideo && player != null && playerUi != null

    Box(modifier.fillMaxSize()) {
        if (showVideoControls) {
            PlayPauseButton(player!!, playerUi!!, Modifier.align(Alignment.Center))
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                .padding(top = 24.dp, bottom = 12.dp)
        ) {
            if (showVideoControls) {
                VideoSeekBar(player!!, playerUi!!)
            }
            ThumbnailCarousel(items = items, pagerState = pagerState)
            Spacer(Modifier.height(8.dp))
            ButtonsRow(
                trashMode = trashMode,
                isFavorite = isFavorite,
                onShare = onShare,
                onToggleFavorite = onToggleFavorite,
                onInfo = onInfo,
                onTrash = onTrash,
                onRestore = onRestore,
                onDeleteForever = onDeleteForever
            )
        }
    }
}

@Composable
private fun ButtonsRow(
    trashMode: Boolean,
    isFavorite: Boolean,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfo: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Esquina inferior izquierda: compartir (en la papelera, restaurar).
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (trashMode) {
                ViewerCircleButton(stringResource(R.string.viewer_restore), onRestore) { color ->
                    drawRestoreIcon(color)
                }
            } else {
                ViewerCircleButton(stringResource(R.string.viewer_share), onShare) { color -> drawShareIcon(color) }
            }
        }
        // Centro: grupo de botones del elemento (el de ajustes se añadirá más adelante).
        Row(
            Modifier.clip(RoundedCornerShape(percent = 50)).background(Color.White.copy(alpha = 0.12f)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!trashMode) {
                ViewerPillButton(
                    stringResource(if (isFavorite) R.string.viewer_favorite_remove else R.string.viewer_favorite_add),
                    onToggleFavorite
                ) { color -> drawHeartIcon(color, filled = isFavorite) }
            }
            ViewerPillButton(stringResource(R.string.viewer_info), onInfo) { color -> drawInfoIcon(color) }
        }
        // Esquina inferior derecha: enviar a la papelera (en la papelera, eliminar definitivamente).
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (trashMode) {
                ViewerCircleButton(stringResource(R.string.viewer_delete_forever), onDeleteForever) { color ->
                    drawTrashIcon(color)
                }
            } else {
                ViewerCircleButton(stringResource(R.string.viewer_trash), onTrash) { color -> drawTrashIcon(color) }
            }
        }
    }
}

@Composable
private fun ViewerCircleButton(
    description: String,
    onClick: () -> Unit,
    icon: DrawScope.(Color) -> Unit
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) { icon(Color.White) }
    }
}

@Composable
private fun ViewerPillButton(
    description: String,
    onClick: () -> Unit,
    icon: DrawScope.(Color) -> Unit
) {
    Box(
        Modifier
            .size(width = 56.dp, height = 48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) { icon(Color.White) }
    }
}

/** Botón central de reproducir/pausar. Solo actúa al pulsarlo: tocar el resto de la pantalla no pausa. */
@Composable
private fun PlayPauseButton(player: Player, ui: PlayerUi, modifier: Modifier = Modifier) {
    val description = stringResource(if (ui.showPause) R.string.viewer_pause else R.string.viewer_play)
    Box(
        modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable {
                when {
                    player.playbackState == Player.STATE_ENDED -> {
                        player.seekTo(0L)
                        player.play()
                    }

                    player.playWhenReady -> player.pause()
                    else -> player.play()
                }
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(30.dp)) {
            if (ui.showPause) drawPauseIcon(Color.White) else drawPlayIcon(Color.White)
        }
    }
}

/** Barra de reproducción: tiempo actual, barra arrastrable y duración total. */
@Composable
private fun VideoSeekBar(player: Player, ui: PlayerUi, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val duration = ui.durationMs
    val playedFraction = if (duration > 0L) (ui.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownFraction = if (dragging) dragFraction else playedFraction
    val shownPosition = if (dragging) (dragFraction * duration).toLong() else ui.positionMs

    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(formatDuration(shownPosition), color = Color.White, fontSize = 12.sp)
        Slider(
            value = shownFraction,
            onValueChange = {
                dragging = true
                dragFraction = it
            },
            onValueChangeFinished = {
                if (duration > 0L) player.seekTo((dragFraction * duration).toLong())
                dragging = false
            },
            enabled = duration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                disabledThumbColor = Color.White.copy(alpha = 0.5f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.3f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Text(formatDuration(duration), color = Color.White, fontSize = 12.sp)
    }
}
