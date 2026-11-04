package com.example.galeria.data.thumbnails

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import com.example.galeria.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Carga miniaturas pequeñas con las miniaturas del propio sistema (MediaStore),
 * que Android mantiene en caché, en lugar de decodificar las fotos originales.
 * Incluye una caché en memoria limitada y un máximo de cargas simultáneas.
 */
class ThumbnailLoader(private val contentResolver: ContentResolver) {

    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    // Las peticiones que esperan turno se cancelan si la miniatura sale de pantalla.
    private val limiter = Semaphore(MAX_PARALLEL_LOADS)

    fun getCached(item: MediaItem, sizePx: Int): Bitmap? = cache.get(cacheKey(item, sizePx))

    suspend fun load(item: MediaItem, sizePx: Int): Bitmap? {
        val key = cacheKey(item, sizePx)
        cache.get(key)?.let { return it }

        return limiter.withPermit {
            cache.get(key)?.let { return@withPermit it }
            withContext(Dispatchers.IO) {
                val bitmap = decode(item, sizePx)
                if (bitmap != null) cache.put(key, bitmap)
                bitmap
            }
        }
    }

    private fun decode(item: MediaItem, sizePx: Int): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(item.uri, Size(sizePx, sizePx), null)
        } else {
            legacyThumbnail(item)
        }
    } catch (e: Exception) {
        // Archivo borrado o movido mientras se cargaba, o sin permiso: se deja el hueco gris.
        null
    }

    @Suppress("DEPRECATION")
    private fun legacyThumbnail(item: MediaItem): Bitmap? =
        if (item.isVideo) {
            MediaStore.Video.Thumbnails.getThumbnail(
                contentResolver, item.id, MediaStore.Video.Thumbnails.MINI_KIND, null
            )
        } else {
            MediaStore.Images.Thumbnails.getThumbnail(
                contentResolver, item.id, MediaStore.Images.Thumbnails.MINI_KIND, null
            )
        }

    private fun cacheKey(item: MediaItem, sizePx: Int) = "${item.key}@$sizePx"

    companion object {
        private const val MAX_PARALLEL_LOADS = 4

        // Tamaños de miniatura posibles. Pocos tamaños evitan recargar al cambiar el zoom ligeramente.
        private const val SIZE_SMALL = 160
        private const val SIZE_MEDIUM = 320
        private const val SIZE_LARGE = 512

        /** Elige el tamaño de miniatura a pedir según el tamaño real de la celda en píxeles. */
        fun bucketFor(cellSizePx: Int): Int = when {
            cellSizePx <= SIZE_SMALL -> SIZE_SMALL
            cellSizePx <= SIZE_MEDIUM -> SIZE_MEDIUM
            else -> SIZE_LARGE
        }

        // Un sexto de la memoria máxima de la app.
        private fun cacheSizeKb(): Int = (Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()
    }
}
