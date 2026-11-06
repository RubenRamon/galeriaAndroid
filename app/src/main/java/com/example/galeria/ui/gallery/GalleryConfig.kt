package com.example.galeria.ui.gallery

import androidx.compose.ui.unit.dp

/** Valores ajustables de la cuadrícula y del zoom. */
object GalleryConfig {
    /** Columnas al abrir la galería o un álbum. */
    const val DEFAULT_COLUMNS = 5

    /** Límites del zoom: 2 columnas = miniaturas muy grandes; 15 = muy pequeñas. */
    const val MIN_COLUMNS = 2
    const val MAX_COLUMNS = 15

    /**
     * UMBRAL DEL MODO COMPACTO.
     * Con este número de columnas o más, pulsar una miniatura NO la abre: amplía la cuadrícula.
     * Con menos (hasta 5 columnas), pulsar abre la foto o el vídeo.
     */
    const val COMPACT_MIN_COLUMNS = 6

    /**
     * Niveles a los que salta la cuadrícula al pulsar en modo compacto. Se elige el primer nivel
     * con menos columnas que las actuales (de 15 a 12, de 12 a 9, de 9 a 7, de 7 a 5).
     * Debe estar ordenada de mayor a menor y su último valor debe ser COMPACT_MIN_COLUMNS - 1,
     * para que al pulsar se acabe llegando al modo normal.
     */
    val TAP_ZOOM_LEVELS = intArrayOf(12, 9, 7, 5)

    /** Con este número de columnas o más, los encabezados pasan de «Nov 2026» a «2026». */
    const val YEAR_HEADERS_MIN_COLUMNS = 12

    /** Con este número de columnas o más (miniaturas muy pequeñas), los vídeos no muestran su duración. */
    const val HIDE_VIDEO_INFO_MIN_COLUMNS = 10

    /** Duración de la transición al pulsar en modo compacto. */
    const val ZOOM_ANIMATION_MS = 250

    /** Al soltar los dedos, tiempo que tarda la cuadrícula en asentarse en el número de columnas más cercano. */
    const val ZOOM_SETTLE_ANIMATION_MS = 180

    /** Espacio libre al final de las listas para que la isla inferior no tape las últimas miniaturas. */
    val BOTTOM_ISLAND_CLEARANCE = 84.dp

    /** Separación entre miniaturas. */
    val CELL_SPACING = 1.dp
}
