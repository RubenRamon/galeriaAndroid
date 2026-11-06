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
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Margen para que un valor entero exacto (5,0) no se trate como 5,0001. */
private const val EPSILON = 0.001f

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
 * Estado del zoom de una cuadrícula. Reutilizable en la galería y en los álbumes.
 *
 * Cómo funciona el zoom fluido:
 *  - Existe un número de columnas CONTINUO (p. ej. 5,4) que sigue a los dedos.
 *  - La cuadrícula real usa el entero inmediatamente superior ([columns], p. ej. 6).
 *  - La diferencia se compensa con una escala gráfica ([zoomScale] = columns / continuo), de modo que
 *    el tamaño visible de las miniaturas cambia de forma continua. Como el entero es siempre >= al
 *    continuo, la escala es >= 1 y la cuadrícula nunca deja bordes vacíos.
 *  - Solo al cruzar un entero se vuelve a calcular la cuadrícula; el resto del tiempo es solo dibujo.
 */
@Stable
class GridZoomState(initialColumns: Int = GalleryConfig.DEFAULT_COLUMNS) {

    /** Columnas de la cuadrícula real (entero). */
    var columns by mutableIntStateOf(initialColumns.coerceIn(GalleryConfig.MIN_COLUMNS, GalleryConfig.MAX_COLUMNS))
        private set

    /** true si pulsar una miniatura debe ampliar en lugar de abrir. */
    val isCompact: Boolean get() = columns >= GalleryConfig.COMPACT_MIN_COLUMNS

    val showsYearHeaders: Boolean get() = columns >= GalleryConfig.YEAR_HEADERS_MIN_COLUMNS

    /** Escala gráfica que mantiene continuo el tamaño visible (1 = en reposo). Solo se lee al dibujar. */
    var zoomScale by mutableFloatStateOf(1f)
        private set

    /** Punto (en píxeles) alrededor del cual se escala: los dedos o la miniatura pulsada. */
    var zoomOrigin by mutableStateOf(Offset.Unspecified)
        private set

    // Los actualiza ZoomableMediaGrid en cada recomposición.
    internal var gridState: LazyGridState? = null
    internal var entriesProvider: (yearHeaders: Boolean) -> List<GalleryEntry> = { emptyList() }
    internal var leadingCount: Int = 0

    private var columnsFloat = columns.toFloat()
    private var pinchStartColumns = columnsFloat
    private var animationJob: Job? = null

    internal fun beginPinch(centroid: Offset): ZoomAnchor? {
        animationJob?.cancel()
        pinchStartColumns = columnsFloat
        val anchor = findAnchor(centroid)
        if (centroid.isSpecified) setOrigin(centroid.x, anchor, centroid.y)
        return anchor
    }

    /** Separar dedos (zoom > 1) = menos columnas; juntarlos = más columnas. */
    internal fun updatePinch(totalZoom: Float, anchor: ZoomAnchor?) {
        if (totalZoom > 0f) setContinuous(pinchStartColumns / totalZoom, anchor)
    }

    /** Al soltar, la cuadrícula se asienta con suavidad en el número entero de columnas más cercano. */
    internal fun endPinch(scope: CoroutineScope, anchor: ZoomAnchor?) {
        val target = columnsFloat.roundToInt()
            .coerceIn(GalleryConfig.MIN_COLUMNS, GalleryConfig.MAX_COLUMNS)
            .toFloat()
        animateTo(scope, target, anchor, GalleryConfig.ZOOM_SETTLE_ANIMATION_MS)
    }

    /** Amplía un nivel con transición alrededor de la miniatura pulsada (modo compacto). */
    internal fun zoomInFrom(scope: CoroutineScope, itemKey: String) {
        val target = tapZoomTarget(columns)
        if (target >= columns) return

        val info = visibleInfo(itemKey)
        val anchor = info?.let { anchorFrom(it) }
        if (info != null) setOrigin(info.offset.x + info.size.width / 2f, anchor, info.offset.y.toFloat())

        animateTo(scope, target.toFloat(), anchor, GalleryConfig.ZOOM_ANIMATION_MS)
    }

    private fun animateTo(scope: CoroutineScope, target: Float, anchor: ZoomAnchor?, durationMs: Int) {
        animationJob?.cancel()
        if (columnsFloat == target) {
            setContinuous(target, anchor)
            return
        }
        animationJob = scope.launch {
            animate(
                initialValue = columnsFloat,
                targetValue = target,
                animationSpec = tween(durationMs)
            ) { value, _ -> setContinuous(value, anchor) }
        }
    }

    private fun tapZoomTarget(current: Int): Int =
        GalleryConfig.TAP_ZOOM_LEVELS.firstOrNull { it < current }
            ?: (current - 1).coerceAtLeast(GalleryConfig.MIN_COLUMNS)

    /** Los ejes se escalan alrededor de la x de los dedos y de la altura de la fila de referencia. */
    private fun setOrigin(x: Float, anchor: ZoomAnchor?, fallbackY: Float) {
        zoomOrigin = Offset(x, anchor?.topPx?.toFloat() ?: fallbackY)
    }

    /**
     * Aplica un número de columnas continuo: actualiza la escala gráfica y, solo si cambia el entero,
     * la cuadrícula real (manteniendo en su sitio el elemento de referencia).
     */
    private fun setContinuous(value: Float, anchor: ZoomAnchor?) {
        val f = value.coerceIn(GalleryConfig.MIN_COLUMNS.toFloat(), GalleryConfig.MAX_COLUMNS.toFloat())
        columnsFloat = f
        val committed = ceil(f - EPSILON).toInt()
            .coerceIn(GalleryConfig.MIN_COLUMNS, GalleryConfig.MAX_COLUMNS)

        // Misma escritura de estado que el cambio de columnas: el primer dibujado ya sale compensado.
        zoomScale = committed / f

        if (committed == columns) return
        columns = committed

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
internal suspend fun PointerInputScope.detectGridPinch(zoomState: GridZoomState, scope: CoroutineScope) {
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

        if (pinching) zoomState.endPinch(scope, anchor)
    }
}
