package com.example.galeria.ui.viewer

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.galeria.R

/**
 * Reproductor a pantalla completa con controles de reproducción, pausa y progreso.
 * Crea un ExoPlayer al entrar en la composición y lo libera al salir.
 */
@Composable
fun VideoPlayer(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasError by remember(uri) { mutableStateOf(false) }

    val player = remember(uri) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
    }
    // Se infla desde XML porque el tipo de superficie (TextureView) solo se puede elegir ahí.
    val playerView = remember(context) {
        LayoutInflater.from(context).inflate(R.layout.view_player, null) as PlayerView
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                hasError = true
            }
        }
        player.addListener(listener)
        playerView.player = player
        onDispose {
            player.removeListener(listener)
            playerView.player = null
            player.release() // libera decodificadores y memoria
        }
    }

    // Pausa el vídeo si la app deja de ser visible.
    val lifecycle = remember(context) { context.findLifecycleOwner()?.lifecycle }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    Box(modifier.windowInsetsPadding(WindowInsets.systemBars)) {
        AndroidView(factory = { playerView }, modifier = Modifier.matchParentSize())
        if (hasError) {
            Text(
                text = stringResource(R.string.viewer_video_error),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}
