package com.example.galeria.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.galeria.R

/** Botón circular con una flecha hacia la izquierda. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.back_description)
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = 2.dp.toPx()
            val w = size.width
            val h = size.height
            drawLine(Color.White, Offset(w, h / 2f), Offset(0f, h / 2f), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.45f, 0f), Offset(0f, h / 2f), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.45f, h), Offset(0f, h / 2f), stroke, StrokeCap.Round)
        }
    }
}
