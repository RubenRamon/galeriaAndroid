package com.example.galeria

import android.content.Context
import com.example.galeria.data.MediaRepository
import com.example.galeria.data.MediaStoreDataSource
import com.example.galeria.data.permissions.MediaPermissions
import com.example.galeria.data.thumbnails.ThumbnailLoader

/**
 * Contenedor de dependencias sencillo (sin librerías externas).
 * Las futuras funciones añadirán aquí sus repositorios.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val mediaPermissions = MediaPermissions(appContext)
    val mediaRepository = MediaRepository(MediaStoreDataSource(appContext.contentResolver))
    val thumbnailLoader = ThumbnailLoader(appContext.contentResolver)
}
