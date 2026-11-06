package com.example.galeria.data.model

/** Datos detallados de un elemento multimedia, para el panel de información. */
data class MediaInfo(
    val displayName: String,
    val isVideo: Boolean,
    val mimeType: String?,
    /** Extensión en mayúsculas, p. ej. «JPG». Vacía si el archivo no tiene. */
    val extension: String,
    val sizeBytes: Long,
    /** Fecha original de captura (EXIF o MediaStore); si no existe, la fecha usada por la galería. */
    val capturedMillis: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val bucketName: String,
    val folderPath: String
) {
    companion object {
        /** Información básica ya disponible en el propio [MediaItem], sin consultar nada más. */
        fun from(item: MediaItem) = MediaInfo(
            displayName = item.displayName,
            isVideo = item.isVideo,
            mimeType = item.mimeType,
            extension = item.displayName.substringAfterLast('.', "").uppercase(),
            sizeBytes = 0L,
            capturedMillis = item.dateMillis,
            width = item.width,
            height = item.height,
            durationMs = item.durationMs,
            bucketName = item.bucketName,
            folderPath = item.folderPath
        )
    }
}
