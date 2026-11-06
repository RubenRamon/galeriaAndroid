package com.example.galeria.data.model

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

/**
 * Un elemento multimedia (foto o vídeo) del teléfono.
 *
 * Los ids de imágenes y vídeos pueden coincidir entre sí, por eso [key]
 * incluye el tipo. Úsala como clave estable en listas y cuadrículas.
 */
data class MediaItem(
    val id: Long,
    val type: MediaType,
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    /** Fecha usada para ordenar y agrupar (milisegundos). */
    val dateMillis: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    /** Identificador de carpeta de MediaStore. Distingue carpetas con el mismo nombre. */
    val bucketId: Long,
    val bucketName: String,
    /** Ruta relativa (Android 10+) o ruta de la carpeta (anteriores). */
    val folderPath: String
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO

    val key: String = keyOf(type, id)

    companion object {
        /** Clave estable de un elemento: el tipo evita choques entre ids de fotos y de vídeos. */
        fun keyOf(type: MediaType, id: Long): String = (if (type == MediaType.VIDEO) "v_" else "i_") + id
    }
}
