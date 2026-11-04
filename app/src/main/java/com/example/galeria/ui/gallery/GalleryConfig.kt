package com.example.galeria.ui.gallery

import androidx.compose.ui.unit.dp

/** Valores ajustables de la cuadrícula y del zoom. */
object GalleryConfig {
    /** Columnas iniciales (la captura de referencia muestra 8). */
    const val DEFAULT_COLUMNS = 8

    /** Límites del zoom: 2 columnas = miniaturas muy grandes; 15 = muy pequeñas. */
    const val MIN_COLUMNS = 2
    const val MAX_COLUMNS = 15

    /**
     * UMBRAL DEL MODO COMPACTO.
     * Con este número de columnas o más, pulsar una miniatura NO la abre:
     * amplía la cuadrícula. Con menos columnas, pulsar abre la foto o el vídeo.
     */
    const val COMPACT_MIN_COLUMNS = 11

    /** Columnas a las que salta la cuadrícula al pulsar en modo compacto. */
    const val COMPACT_TAP_TARGET_COLUMNS = 8

    /** Con este número de columnas o más, los encabezados pasan de «Nov 2026» a «2026». */
    const val YEAR_HEADERS_MIN_COLUMNS = 12

    /** Duración de la animación al pulsar en modo compacto. */
    const val ZOOM_ANIMATION_MS = 250

    /** Separación entre miniaturas. */
    val CELL_SPACING = 1.dp
}
