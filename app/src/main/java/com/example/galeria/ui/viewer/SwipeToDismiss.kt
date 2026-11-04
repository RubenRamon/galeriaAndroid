package com.example.galeria.ui.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs

/** Fracción de la altura de pantalla a partir de la cual soltar cierra el visor. */
private const val DISMISS_DISTANCE_FRACTION = 0.2f

/** Velocidad (px/s) de un gesto rápido hacia abajo que cierra el visor. */
private const val DISMISS_VELOCITY = 1500f

/**
 * Detecta el arrastre de un dedo hacia abajo. Se observa en el paso inicial de eventos para
 * funcionar también sobre el reproductor de vídeo. No actúa si:
 *  - hay más de un dedo (zoom),
 *  - la foto está ampliada ([isEnabled] devuelve false),
 *  - el gesto empieza siendo horizontal (se deja al visor para cambiar de elemento) o hacia arriba.
 *
 * @param onDrag distancia arrastrada hacia abajo, en píxeles.
 * @param onEnd al soltar o cancelar; true si hay que cerrar el visor.
 */
internal suspend fun PointerInputScope.detectSwipeDownToDismiss(
    isEnabled: () -> Boolean,
    onDrag: (Float) -> Unit,
    onEnd: (dismiss: Boolean) -> Unit
) {
    awaitEachGesture {
        val slop = viewConfiguration.touchSlop
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)

        var total = Offset.Zero
        var dragging = false
        var aborted = !isEnabled()

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } > 1 || !isEnabled()) aborted = true

            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null) {
                if (dragging) onEnd(false)
                return@awaitEachGesture
            }

            if (aborted) {
                if (dragging) {
                    dragging = false
                    onEnd(false)
                }
                if (event.changes.none { it.pressed }) return@awaitEachGesture
                continue
            }

            if (!change.pressed) {
                if (dragging) {
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val velocityY = tracker.calculateVelocity().y
                    onEnd(total.y > size.height * DISMISS_DISTANCE_FRACTION || velocityY > DISMISS_VELOCITY)
                }
                return@awaitEachGesture
            }

            tracker.addPosition(change.uptimeMillis, change.position)
            total += change.positionChange()

            if (!dragging) {
                if (abs(total.x) > slop && abs(total.x) >= abs(total.y)) {
                    aborted = true // horizontal: lo gestiona el visor para cambiar de elemento
                    continue
                }
                if (total.y < -slop) {
                    aborted = true // hacia arriba: no hace nada
                    continue
                }
                if (total.y > slop && total.y > abs(total.x)) dragging = true
            }

            if (dragging) {
                onDrag(total.y.coerceAtLeast(0f))
                change.consume()
            }
        }
    }
}
