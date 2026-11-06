package com.example.galeria

import android.content.Context
import com.example.galeria.data.FavoritesRepository
import com.example.galeria.data.MediaInfoLoader
import com.example.galeria.data.MediaRepository
import com.example.galeria.data.MediaStoreDataSource
import com.example.galeria.data.TrashRepository
import com.example.galeria.data.db.AppDatabase
import com.example.galeria.data.db.FavoritesStore
import com.example.galeria.data.permissions.MediaPermissions
import com.example.galeria.data.thumbnails.ThumbnailLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contenedor de dependencias sencillo (sin librerías externas).
 * Las futuras funciones añadirán aquí sus repositorios.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Ámbito que vive lo mismo que la aplicación, para escribir en la base de datos en segundo plano. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val mediaPermissions = MediaPermissions(appContext)

    private val mediaStoreDataSource = MediaStoreDataSource(appContext.contentResolver)
    val mediaRepository = MediaRepository(mediaStoreDataSource)

    val thumbnailLoader = ThumbnailLoader(appContext.contentResolver)
    val mediaInfoLoader = MediaInfoLoader(appContext.contentResolver)

    val trashRepository = TrashRepository(appContext.contentResolver, mediaStoreDataSource)

    private val appDatabase = AppDatabase(appContext)
    val favoritesRepository = FavoritesRepository(FavoritesStore(appDatabase), mediaStoreDataSource, appScope)
}
