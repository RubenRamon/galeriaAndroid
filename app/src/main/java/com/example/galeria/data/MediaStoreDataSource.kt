package com.example.galeria.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

/**
 * Único punto de la app que habla con MediaStore.
 * Todas las consultas se ejecutan fuera del hilo principal.
 */
class MediaStoreDataSource(private val contentResolver: ContentResolver) {

    private val imagesUri: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    private val videosUri: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    /**
     * Devuelve fotos y vídeos mezclados, del más reciente al más antiguo.
     * Si el acceso es parcial (Android 14+), MediaStore solo devuelve lo autorizado.
     * Lanza SecurityException si no hay ningún permiso concedido.
     */
    suspend fun queryAll(): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = query(imagesUri, MediaType.IMAGE)
        val videos = query(videosUri, MediaType.VIDEO)
        (images + videos).sortedWith(
            compareByDescending<MediaItem> { it.dateMillis }.thenByDescending { it.id }
        )
    }

    /** Emite cada vez que cambia el contenido multimedia (alta, baja o movimiento de archivos). */
    fun observeChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        contentResolver.registerContentObserver(imagesUri, true, observer)
        contentResolver.registerContentObserver(videosUri, true, observer)
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.conflate()

    @Suppress("DEPRECATION")
    private suspend fun query(collection: Uri, type: MediaType): List<MediaItem> {
        val isVideo = type == MediaType.VIDEO
        val useRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(COL_DATE_TAKEN)
            add(COL_BUCKET_ID)
            add(COL_BUCKET_NAME)
            add(if (useRelativePath) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA)
            if (isVideo) add(MediaStore.Video.Media.DURATION)
        }.toTypedArray()

        val cursor = contentResolver.query(collection, projection, null, null, null)
            ?: return emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val addedCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val modifiedCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val takenCol = it.getColumnIndexOrThrow(COL_DATE_TAKEN)
            val bucketIdCol = it.getColumnIndexOrThrow(COL_BUCKET_ID)
            val bucketNameCol = it.getColumnIndexOrThrow(COL_BUCKET_NAME)
            val pathCol = it.getColumnIndexOrThrow(
                if (useRelativePath) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
            )
            val durationCol = if (isVideo) it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION) else -1

            val result = ArrayList<MediaItem>(it.count)
            var rows = 0
            while (it.moveToNext()) {
                if (++rows % CANCELLATION_CHECK_EVERY == 0) currentCoroutineContext().ensureActive()

                val id = it.getLong(idCol)
                val taken = it.getLong(takenCol)
                val modified = it.getLong(modifiedCol)
                val added = it.getLong(addedCol)
                // Fecha de captura; si no existe, fecha de modificación; si no, fecha de alta.
                val dateMillis = when {
                    taken > 0 -> taken
                    modified > 0 -> modified * 1000
                    else -> added * 1000
                }

                val rawPath = it.getString(pathCol).orEmpty()
                val folderPath = if (useRelativePath) rawPath else rawPath.substringBeforeLast('/', "")

                result.add(
                    MediaItem(
                        id = id,
                        type = type,
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = it.getString(nameCol).orEmpty(),
                        mimeType = it.getString(mimeCol),
                        dateMillis = dateMillis,
                        durationMs = if (durationCol >= 0) it.getLong(durationCol) else 0L,
                        width = it.getInt(widthCol),
                        height = it.getInt(heightCol),
                        bucketId = it.getLong(bucketIdCol),
                        bucketName = it.getString(bucketNameCol)?.takeIf { n -> n.isNotBlank() } ?: UNKNOWN_BUCKET,
                        folderPath = folderPath
                    )
                )
            }
            return result
        }
    }

    private companion object {
        // Mismos nombres de columna para imágenes y vídeos; se usan las constantes
        // de Images porque existen desde API 1 (las de MediaColumns requieren API 29).
        const val COL_DATE_TAKEN = MediaStore.Images.Media.DATE_TAKEN
        const val COL_BUCKET_ID = MediaStore.Images.Media.BUCKET_ID
        const val COL_BUCKET_NAME = MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        const val UNKNOWN_BUCKET = "Sin carpeta"
        const val CANCELLATION_CHECK_EVERY = 500
    }
}
