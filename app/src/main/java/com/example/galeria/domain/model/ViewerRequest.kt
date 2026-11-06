package com.example.galeria.domain.model

import com.example.galeria.data.model.MediaItem

/** Petición de abrir el visor: la lista por la que se puede deslizar y el elemento inicial. */
data class ViewerRequest(
    val items: List<MediaItem>,
    val startIndex: Int,
    /** true si se abrió desde la papelera: el visor ofrece restaurar y eliminar en lugar de compartir y enviar. */
    val fromTrash: Boolean = false
)
