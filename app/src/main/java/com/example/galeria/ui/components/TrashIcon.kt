package com.example.galeria.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Papelera: tapa con asa y cubo con dos líneas. */
internal fun DrawScope.drawTrashIcon(color: Color) {
    val w = size.width
    val stroke = 1.8.dp.toPx()
    val style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Tapa y asa
    drawLine(color, Offset(w * 0.14f, w * 0.25f), Offset(w * 0.86f, w * 0.25f), stroke, StrokeCap.Round)
    val handle = Path().apply {
        moveTo(w * 0.38f, w * 0.25f)
        lineTo(w * 0.38f, w * 0.13f)
        lineTo(w * 0.62f, w * 0.13f)
        lineTo(w * 0.62f, w * 0.25f)
    }
    drawPath(handle, color, style = style)

    // Cubo
    val bin = Path().apply {
        moveTo(w * 0.24f, w * 0.31f)
        lineTo(w * 0.30f, w * 0.88f)
        lineTo(w * 0.70f, w * 0.88f)
        lineTo(w * 0.76f, w * 0.31f)
    }
    drawPath(bin, color, style = style)
    drawLine(color, Offset(w * 0.42f, w * 0.44f), Offset(w * 0.42f, w * 0.75f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.58f, w * 0.44f), Offset(w * 0.58f, w * 0.75f), stroke, StrokeCap.Round)
}
