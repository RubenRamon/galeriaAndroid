package com.example.galeria.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Corazón: relleno si [filled] (es favorito) o solo el contorno si no lo es. */
internal fun DrawScope.drawHeartIcon(color: Color, filled: Boolean) {
    val w = size.width
    val heart = Path().apply {
        moveTo(w * 0.50f, w * 0.90f)
        cubicTo(w * 0.12f, w * 0.62f, w * 0.04f, w * 0.30f, w * 0.25f, w * 0.16f)
        cubicTo(w * 0.36f, w * 0.08f, w * 0.50f, w * 0.18f, w * 0.50f, w * 0.30f)
        cubicTo(w * 0.50f, w * 0.18f, w * 0.64f, w * 0.08f, w * 0.75f, w * 0.16f)
        cubicTo(w * 0.96f, w * 0.30f, w * 0.88f, w * 0.62f, w * 0.50f, w * 0.90f)
        close()
    }
    if (filled) {
        drawPath(heart, color)
    } else {
        drawPath(heart, color, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round))
    }
}
