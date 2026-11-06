package com.example.galeria.ui.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import kotlinx.coroutines.delay

/** Estado observable del reproductor para dibujar la barra de progreso y el botón de pausa. */
@Stable
class PlayerUi {
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)

    /** true si hay que mostrar el icono de pausa (el vídeo está reproduciéndose o a punto de hacerlo). */
    var showPause by mutableStateOf(false)
}

/**
 * Sigue al [player] mientras esta función esté en la composición. Como los controles solo existen
 * cuando están visibles, el sondeo de la posición se detiene al ocultarlos.
 */
@Composable
internal fun rememberPlayerUi(player: Player): PlayerUi {
    val ui = remember(player) { PlayerUi() }

    DisposableEffect(player) {
        fun sync() {
            ui.showPause = player.playWhenReady && player.playbackState != Player.STATE_ENDED
            ui.durationMs = player.duration.coerceAtLeast(0L)
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = sync()
        }
        sync()
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            ui.positionMs = player.currentPosition.coerceAtLeast(0L)
            ui.durationMs = player.duration.coerceAtLeast(0L)
            delay(POSITION_POLL_MS)
        }
    }
    return ui
}

private const val POSITION_POLL_MS = 250L
