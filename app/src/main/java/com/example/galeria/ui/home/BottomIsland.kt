@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.galeria.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.galeria.R

private val IslandBackground = Color(0xF21E1E1E)
private val SelectedBackground = Color.White.copy(alpha = 0.16f)
private val SelectedIcon = Color.White
private val UnselectedIcon = Color(0xFF9A9A9A)

/**
 * Isla flotante inferior con dos destinos: fotos (cuadrícula) y álbumes.
 * El destino actual aparece resaltado.
 */
@Composable
fun BottomIsland(
    isGallerySelected: Boolean,
    onGalleryClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(IslandBackground)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IslandItem(
            selected = isGallerySelected,
            description = stringResource(R.string.tab_photos),
            onClick = onGalleryClick
        ) { color -> drawPhotosIcon(color) }

        IslandItem(
            selected = !isGallerySelected,
            description = stringResource(R.string.albums_title),
            onClick = onAlbumsClick
        ) { color -> drawAlbumsIcon(color) }
    }
}

@Composable
private fun IslandItem(
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    icon: DrawScope.(Color) -> Unit
) {
    val color = if (selected) SelectedIcon else UnselectedIcon
    Box(
        Modifier
            .size(width = 88.dp, height = 46.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) SelectedBackground else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(24.dp)) { icon(color) }
    }
}

/** Icono de imagen: marco redondeado con sol y montañas. */
private fun DrawScope.drawPhotosIcon(color: Color) {
    val w = size.width
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(w - stroke, w - stroke),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(stroke)
    )
    drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.32f, w * 0.34f))
    val mountains = Path().apply {
        moveTo(w * 0.14f, w * 0.82f)
        lineTo(w * 0.42f, w * 0.52f)
        lineTo(w * 0.58f, w * 0.68f)
        lineTo(w * 0.70f, w * 0.56f)
        lineTo(w * 0.88f, w * 0.82f)
    }
    drawPath(mountains, color, style = Stroke(stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Icono de álbumes: pila de dos hojas con la delantera enmarcada. */
private fun DrawScope.drawAlbumsIcon(color: Color) {
    val w = size.width
    val stroke = 2.dp.toPx()
    drawLine(color, Offset(w * 0.20f, w * 0.08f), Offset(w * 0.80f, w * 0.08f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.10f, w * 0.22f), Offset(w * 0.90f, w * 0.22f), stroke, StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2f, w * 0.34f),
        size = Size(w - stroke, w * 0.62f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(stroke)
    )
}
