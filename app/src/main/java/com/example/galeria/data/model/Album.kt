package com.example.galeria.data.model

/** Una carpeta real del teléfono con fotos o vídeos. */
data class Album(
    val bucketId: Long,
    val name: String,
    val folderPath: String,
    /** Elemento más reciente del álbum. */
    val cover: MediaItem,
    val itemCount: Int
)
