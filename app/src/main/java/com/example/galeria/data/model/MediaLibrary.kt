package com.example.galeria.data.model

/** Instantánea de todo el contenido multimedia accesible. */
data class MediaLibrary(
    /** Todos los elementos, del más reciente al más antiguo. */
    val items: List<MediaItem>,
    val albums: List<Album>,
    /** false hasta que termina la primera consulta. */
    val isLoaded: Boolean
) {
    companion object {
        val Empty = MediaLibrary(emptyList(), emptyList(), isLoaded = false)
    }
}
