package com.example.galeria.ui.viewer

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val DOUBLE_TAP_ANIMATION_MS = 200

/**
 * Imagen con zoom de dos dedos, desplazamiento al estar ampliada y doble toque.
 * Sin ampliar, el desplazamiento de un dedo no se consume: así el visor puede cambiar de foto.
 * Ampliada, al llegar al borde lateral el gesto vuelve a quedar libre para cambiar de foto.
 */
@Composable
fun ZoomableImage(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
    onZoomedChange: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    var doubleTapJob by remember { mutableStateOf<Job?>(null) }

    // Informa al visor de si la foto está ampliada (sin recomponer en cada fotograma del gesto).
    val currentOnZoomedChange by rememberUpdatedState(onZoomedChange)
    val currentOnTap by rememberUpdatedState(onTap)
    LaunchedEffect(Unit) {
        snapshotFlow { scale > 1.01f }
            .distinctUntilChanged()
            .collect { currentOnZoomedChange(it) }
    }

    BoxWithConstraints(modifier) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()

        if (image == null || boxW <= 0f || boxH <= 0f) {
            // Aún sin imagen: el toque sigue mostrando u ocultando los controles.
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { currentOnTap() }) })
            return@BoxWithConstraints
        }

        // Tamaño real con el que se dibuja la foto (ajustada a la pantalla, sin recortar).
        val fit = min(boxW / image.width, boxH / image.height)
        val displayW = image.width * fit
        val displayH = image.height * fit

        fun maxOffsetX(s: Float) = max(0f, (displayW * s - boxW) / 2f)
        fun maxOffsetY(s: Float) = max(0f, (displayH * s - boxH) / 2f)
        fun clamp(o: Offset, s: Float) = Offset(
            o.x.coerceIn(-maxOffsetX(s), maxOffsetX(s)),
            o.y.coerceIn(-maxOffsetY(s), maxOffsetY(s))
        )

        val center = Offset(boxW / 2f, boxH / 2f)

        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(boxW, boxH, displayW, displayH) {
                    detectTapGestures(onTap = { currentOnTap() }, onDoubleTap = { tap ->
                        doubleTapJob?.cancel()
                        val startScale = scale
                        val startOffset = offset
                        val target = if (startScale > 1.05f) 1f else DOUBLE_TAP_SCALE
                        val c = tap - center
                        doubleTapJob = scope.launch {
                            animate(0f, 1f, animationSpec = tween(DOUBLE_TAP_ANIMATION_MS)) { f, _ ->
                                val s = startScale + (target - startScale) * f
                                val z = s / startScale
                                scale = s
                                offset = clamp(c - (c - startOffset) * z, s)
                            }
                        }
                    })
                }
                .pointerInput(boxW, boxH, displayW, displayH) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }

                            if (pressedCount >= 2) {
                                doubleTapJob?.cancel()
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val c = event.calculateCentroid() - center
                                val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                                val z = newScale / scale
                                // El punto bajo los dedos se mantiene fijo al ampliar.
                                offset = clamp(c - (c - offset) * z + pan, newScale)
                                scale = newScale
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (pressedCount == 1 && scale > 1f) {
                                val pan = event.calculatePan()
                                val maxX = maxOffsetX(scale)
                                val atEdge = (pan.x > 0f && offset.x >= maxX) || (pan.x < 0f && offset.x <= -maxX)
                                // En el borde y arrastrando en horizontal: se deja pasar el gesto al visor.
                                if (!(atEdge && abs(pan.x) > abs(pan.y))) {
                                    offset = clamp(offset + pan, scale)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
