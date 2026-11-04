package com.example.galeria.domain.model

/**
 * La misma lista de fotos y vídeos agrupada de dos formas.
 * Comparten los mismos objetos [GalleryEntry.Media]; solo cambian los encabezados.
 */
data class GroupedGallery(
    /** Encabezados «Nov 2026». */
    val byMonth: List<GalleryEntry>,
    /** Encabezados «2026», para la vista más alejada. */
    val byYear: List<GalleryEntry>
) {
    companion object {
        val Empty = GroupedGallery(emptyList(), emptyList())
    }
}
