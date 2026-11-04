package com.example.galeria.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.Locale

// Sin fondo: solo texto blanco. La sombra suave mantiene la lectura sobre fotos claras.
private val DurationTextStyle = TextStyle(
    color = Color.White,
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.75f), offset = Offset(0f, 1f), blurRadius = 4f)
)

/** Duración del vídeo («0:42», «12:05», «1:02:30») en blanco. */
@Composable
fun VideoDurationLabel(durationMs: Long, modifier: Modifier = Modifier) {
    if (durationMs <= 0L) return // duración desconocida: no se muestra nada
    val text = remember(durationMs) { formatDuration(durationMs) }
    Text(
        text = text,
        style = DurationTextStyle,
        maxLines = 1,
        modifier = modifier
    )
}

internal fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs + 500L) / 1000L // redondeo al segundo más cercano
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
