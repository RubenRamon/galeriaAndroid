package com.example.galeria.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Pequeño icono de reproducción (círculo oscuro con triángulo) para miniaturas de vídeo. */
@Composable
fun VideoBadge(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        drawCircle(Color.Black.copy(alpha = 0.55f))
        val w = this.size.width
        val triangle = Path().apply {
            moveTo(w * 0.40f, w * 0.28f)
            lineTo(w * 0.40f, w * 0.72f)
            lineTo(w * 0.75f, w * 0.50f)
            close()
        }
        drawPath(triangle, Color.White)
    }
}
