package com.example.galeria.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.example.galeria.data.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Papelera de la aplicación, basada en la papelera oficial de MediaStore (IS_TRASHED, Android 11+).
 * Enviar a la papelera, restaurar y eliminar definitivamente requieren que el usuario confirme en un
 * diálogo del sistema: aquí solo se preparan esas solicitudes; nunca se borra un archivo directamente.
 * Android elimina por sí mismo los elementos de la papelera pasado un tiempo.
 */
class TrashRepository(
    private val contentResolver: ContentResolver,
    private val dataSource: MediaStoreDataSource
) {
    /** false en Android 10 y anteriores, donde MediaStore no tiene papelera. */
    val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _isLoaded = MutableStateFlow(false)

    /** Elementos actualmente en la papelera. */
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    /** Vuelve a consultar la papelera; solo publica un cambio si el contenido es distinto. */
    suspend fun refresh() {
        val trashed = try {
            dataSource.queryTrashed()
        } catch (e: SecurityException) {
            emptyList()
        }
        if (_items.value != trashed) _items.value = trashed
        _isLoaded.value = true
    }

    /** Solicitud para enviar [items] a la papelera. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createTrashRequest(items: List<MediaItem>): IntentSender =
        MediaStore.createTrashRequest(contentResolver, uris(items), true).intentSender

    /** Solicitud para sacar [items] de la papelera. MediaStore los devuelve a su carpeta original. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createRestoreRequest(items: List<MediaItem>): IntentSender =
        MediaStore.createTrashRequest(contentResolver, uris(items), false).intentSender

    /** Solicitud para eliminar [items] de forma permanente. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createDeleteRequest(items: List<MediaItem>): IntentSender {
        val pending: PendingIntent = MediaStore.createDeleteRequest(contentResolver, uris(items))
        return pending.intentSender
    }

    private fun uris(items: List<MediaItem>): List<Uri> = items.map { dataSource.cleanUri(it.type, it.id) }
}
