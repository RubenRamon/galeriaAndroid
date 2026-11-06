package com.example.galeria.ui.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Iconos del visor dibujados a mano, sin librerías de iconos. */

/** Compartir: tres puntos unidos por dos líneas. */
internal fun DrawScope.drawShareIcon(color: Color) {
    val w = size.width
    val stroke = 1.8.dp.toPx()
    val top = Offset(w * 0.76f, w * 0.20f)
    val middle = Offset(w * 0.24f, w * 0.50f)
    val bottom = Offset(w * 0.76f, w * 0.80f)
    drawLine(color, middle, top, stroke, StrokeCap.Round)
    drawLine(color, middle, bottom, stroke, StrokeCap.Round)
    val radius = w * 0.13f
    drawCircle(color, radius, top)
    drawCircle(color, radius, middle)
    drawCircle(color, radius, bottom)
}

/** Información: «i» dentro de un círculo. */
internal fun DrawScope.drawInfoIcon(color: Color) {
    val w = size.width
    val stroke = 1.8.dp.toPx()
    drawCircle(color, radius = w / 2f - stroke / 2f, style = Stroke(stroke))
    drawCircle(color, radius = stroke * 0.75f, center = Offset(w / 2f, w * 0.31f))
    drawLine(color, Offset(w / 2f, w * 0.46f), Offset(w / 2f, w * 0.75f), stroke, StrokeCap.Round)
}

/** Restaurar: flecha circular. */
internal fun DrawScope.drawRestoreIcon(color: Color) {
    val w = size.width
    val stroke = 1.8.dp.toPx()
    drawArc(
        color = color,
        startAngle = 225f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(w * 0.18f, w * 0.18f),
        size = androidx.compose.ui.geometry.Size(w * 0.64f, w * 0.64f),
        style = Stroke(stroke, cap = StrokeCap.Round)
    )
    // Punta de la flecha en el extremo inicial del arco, apuntando hacia el hueco.
    val tip = Offset(w * 0.274f, w * 0.274f)
    drawLine(color, tip, Offset(tip.x + w * 0.226f, tip.y + w * 0.028f), stroke, StrokeCap.Round)
    drawLine(color, tip, Offset(tip.x + w * 0.028f, tip.y + w * 0.226f), stroke, StrokeCap.Round)
}

internal fun DrawScope.drawPlayIcon(color: Color) {
    val w = size.width
    val triangle = Path().apply {
        moveTo(w * 0.30f, w * 0.18f)
        lineTo(w * 0.30f, w * 0.82f)
        lineTo(w * 0.82f, w * 0.50f)
        close()
    }
    drawPath(triangle, color)
}

internal fun DrawScope.drawPauseIcon(color: Color) {
    val w = size.width
    val barWidth = w * 0.20f
    val barHeight = w * 0.64f
    drawRect(color, Offset(w * 0.22f, w * 0.18f), androidx.compose.ui.geometry.Size(barWidth, barHeight))
    drawRect(color, Offset(w * 0.58f, w * 0.18f), androidx.compose.ui.geometry.Size(barWidth, barHeight))
}
