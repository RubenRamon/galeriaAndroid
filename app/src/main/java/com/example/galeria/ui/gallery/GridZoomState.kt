package com.example.galeria.ui.gallery

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import com.example.galeria.domain.model.GalleryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Elemento de referencia que debe quedarse en la misma altura de pantalla
 * mientras cambian las columnas.
 */
class ZoomAnchor(
    val key: String,
    val entryIndex: Int,
    val yearHeaders: Boolean,
    /** Posición vertical del borde superior del elemento en el momento de empezar. */
    val topPx: Int
)

/**
 * Estado del zoom de una cuadrícula: número de columnas y lógica para cambiarlas
 * manteniendo la posición. Reutilizable en la galería y en los álbumes.
 */
@Stable
class GridZoomState(initialColumns: Int = GalleryConfig.DEFAULT_COLUMNS) {

    var columns by mutableIntStateOf(initialColumns.coerceIn(GalleryConfig.MIN_COLUMNS, GalleryConfig.MAX_COLUMNS))
        private set

    /** true si pulsar una miniatura debe ampliar en lugar de abrir. */
    val isCompact: Boolean get() = columns >= GalleryConfig.COMPACT_MIN_COLUMNS

    val showsYearHeaders: Boolean get() = columns >= GalleryConfig.YEAR_HEADERS_MIN_COLUMNS

    /** Escala gráfica temporal de la animación de zoom (1 = en reposo). Solo se lee al dibujar. */
    var zoomScale by mutableFloatStateOf(1f)
        internal set

    /** Punto (en píxeles) alrededor del cual se anima el zoom: los dedos o la miniatura pulsada. */
    var zoomOrigin by mutableStateOf(Offset.Unspecified)
        private set

    /** Se incrementa en cada cambio de columnas para lanzar la animación. */
    var zoomChangeCount by mutableIntStateOf(0)
        private set

    // Los actualiza ZoomableMediaGrid en cada recomposición.
    internal var gridState: LazyGridState? = null
    internal var entriesProvider: (yearHeaders: Boolean) -> List<GalleryEntry> = { emptyList() }
    internal var leadingCount: Int = 0

    private var pinchStartColumns = columns.toFloat()
    private var animationJob: Job? = null

    internal fun beginPinch(centroid: Offset): ZoomAnchor? {
        animationJob?.cancel()
        pinchStartColumns = columns.toFloat()
        if (centroid.isSpecified) zoomOrigin = centroid
        return findAnchor(centroid)
    }

    /** Separar dedos (zoom > 1) = menos columnas; juntarlos = más columnas. */
    internal fun updatePinch(totalZoom: Float, anchor: ZoomAnchor?) {
        if (totalZoom > 0f) applyColumns(pinchStartColumns / totalZoom, anchor)
    }

    /** Amplía un nivel con animación alrededor de la miniatura pulsada (modo compacto). */
    internal fun zoomInFrom(scope: CoroutineScope, itemKey: String) {
        val target = tapZoomTarget(columns)
        if (target >= columns) return

        val info = visibleInfo(itemKey)
        val anchor = info?.let { anchorFrom(it) }
        if (info != null) {
            zoomOrigin = Offset(
                info.offset.x + info.size.width / 2f,
                info.offset.y + info.size.height / 2f
            )
        }

        animationJob?.cancel()
        animationJob = scope.launch {
            animate(
                initialValue = columns.toFloat(),
                targetValue = target.toFloat(),
                animationSpec = tween(GalleryConfig.ZOOM_ANIMATION_MS)
            ) { value, _ -> applyColumns(value, anchor) }
        }
    }

    private fun tapZoomTarget(current: Int): Int =
        GalleryConfig.TAP_ZOOM_LEVELS.firstOrNull { it < current }
            ?: (current - 1).coerceAtLeast(GalleryConfig.MIN_COLUMNS)

    private fun applyColumns(value: Float, anchor: ZoomAnchor?) {
        val newColumns = value
            .coerceIn(GalleryConfig.MIN_COLUMNS.toFloat(), GalleryConfig.MAX_COLUMNS.toFloat())
            .roundToInt()
        val oldColumns = columns
        if (newColumns == oldColumns) return

        columns = newColumns

        // Animación: las celdas nuevas miden (viejo / nuevo) veces el tamaño anterior. Se parte de
        // esa escala (limitada) y la capa vuelve a 1. Se escribe en el mismo instante que el cambio
        // de columnas para que el primer dibujado ya salga con la escala aplicada.
        zoomScale = (zoomScale * newColumns / oldColumns)
            .coerceIn(GalleryConfig.ZOOM_STEP_MIN_SCALE, GalleryConfig.ZOOM_STEP_MAX_SCALE)
        zoomChangeCount++

        // Se pide el desplazamiento en el mismo instante que el cambio de columnas,
        // así el siguiente dibujado ya sale colocado y no hay saltos visibles.
        val state = gridState ?: return
        if (anchor == null) return
        val index = if (showsYearHeaders == anchor.yearHeaders) {
            anchor.entryIndex
        } else {
            // Al cambiar entre encabezados de mes y de año los índices cambian: se busca por clave.
            entriesProvider(showsYearHeaders).indexOfFirst { it.key == anchor.key }
        }
        if (index >= 0) {
            state.requestScrollToItem(
                index = leadingCount + index,
                scrollOffset = state.layoutInfo.beforeContentPadding - anchor.topPx
            )
        }
    }

    /** Busca la miniatura bajo [position] (o, si es un encabezado, la primera de debajo). */
    private fun findAnchor(position: Offset): ZoomAnchor? {
        if (position.isUnspecified) return null
        val state = gridState ?: return null
        val visible = state.layoutInfo.visibleItemsInfo
        val x = position.x.toInt()
        val y = position.y.toInt()

        val hit = visible.firstOrNull {
            x >= it.offset.x && x < it.offset.x + it.size.width &&
                y >= it.offset.y && y < it.offset.y + it.size.height
        } ?: visible.lastOrNull { it.offset.y <= y } ?: return null

        return anchorFrom(hit)
    }

    /** Convierte un elemento visible en referencia; si es un encabezado, usa la miniatura de debajo. */
    private fun anchorFrom(info: LazyGridItemInfo): ZoomAnchor? {
        val entries = entriesProvider(showsYearHeaders)
        var entryIndex = info.index - leadingCount
        if (entryIndex < 0 || entryIndex >= entries.size) return null // título, aviso o carga

        var topPx = info.offset.y
        if (entries[entryIndex] is GalleryEntry.Header) {
            entryIndex += 1
            if (entryIndex >= entries.size) return null
            topPx = info.offset.y + info.size.height
        }
        return ZoomAnchor(entries[entryIndex].key, entryIndex, showsYearHeaders, topPx)
    }

    private fun visibleInfo(key: String): LazyGridItemInfo? =
        gridState?.layoutInfo?.visibleItemsInfo?.firstOrNull { it.key == key }

    companion object {
        /** Conserva el número de columnas al girar la pantalla. */
        val Saver: Saver<GridZoomState, Int> = Saver(
            save = { it.columns },
            restore = { GridZoomState(it) }
        )
    }
}

/**
 * Detecta el pellizco de dos dedos sin interferir con el scroll de un dedo ni con los toques.
 * Se observa en el paso inicial de eventos: con dos dedos se consume el gesto para que ni la
 * lista se desplace ni se abra ninguna miniatura.
 */
internal suspend fun PointerInputScope.detectGridPinch(zoomState: GridZoomState) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        var pinching = false
        var anchor: ZoomAnchor? = null
        var totalZoom = 1f

        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedCount = event.changes.count { it.pressed }

            if (pressedCount >= 2) {
                if (!pinching) {
                    pinching = true
                    totalZoom = 1f
                    anchor = zoomState.beginPinch(event.calculateCentroid())
                }
                totalZoom *= event.calculateZoom()
                zoomState.updatePinch(totalZoom, anchor)
            }

            // Una vez empezado el pellizco se consume todo hasta soltar los dedos.
            if (pinching) event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}
