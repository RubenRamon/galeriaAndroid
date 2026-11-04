package com.example.galeria.domain.model

import com.example.galeria.data.model.MediaItem

/** Fila de la cuadrícula cronológica: un encabezado de mes o un elemento multimedia. */
sealed interface GalleryEntry {
    /** Clave estable para LazyGrid. */
    val key: String

    data class Header(override val key: String, val title: String) : GalleryEntry

    data class Media(val item: MediaItem) : GalleryEntry {
        override val key: String get() = item.key
    }
}
