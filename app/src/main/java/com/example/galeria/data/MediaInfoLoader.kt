package com.example.galeria.data

import android.content.ContentResolver
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Build
import android.provider.MediaStore
import com.example.galeria.data.model.MediaInfo
import com.example.galeria.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/** Obtiene los datos completos de un elemento: MediaStore y, en fotos, los metadatos EXIF. */
class MediaInfoLoader(private val contentResolver: ContentResolver) {

    suspend fun load(item: MediaItem): MediaInfo = withContext(Dispatchers.IO) {
        var info = MediaInfo.from(item)
        val useRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val pathColumn = if (useRelativePath) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA

        try {
            val projection = buildList {
                add(MediaStore.MediaColumns.SIZE)
                add(MediaStore.MediaColumns.DISPLAY_NAME)
                add(MediaStore.MediaColumns.MIME_TYPE)
                add(MediaStore.MediaColumns.WIDTH)
                add(MediaStore.MediaColumns.HEIGHT)
                add(MediaStore.Images.Media.DATE_TAKEN)
                add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                add(pathColumn)
                if (item.isVideo) add(MediaStore.Video.Media.DURATION)
            }.toTypedArray()

            contentResolver.query(item.uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.stringOrNull(MediaStore.MediaColumns.DISPLAY_NAME) ?: info.displayName
                    val rawPath = c.stringOrNull(pathColumn).orEmpty()
                    info = info.copy(
                        displayName = name,
                        mimeType = c.stringOrNull(MediaStore.MediaColumns.MIME_TYPE) ?: info.mimeType,
                        extension = name.substringAfterLast('.', "").uppercase(),
                        sizeBytes = c.longOrNull(MediaStore.MediaColumns.SIZE) ?: 0L,
                        width = c.longOrNull(MediaStore.MediaColumns.WIDTH)?.toInt() ?: info.width,
                        height = c.longOrNull(MediaStore.MediaColumns.HEIGHT)?.toInt() ?: info.height,
                        capturedMillis = c.longOrNull(MediaStore.Images.Media.DATE_TAKEN)
                            ?.takeIf { it > 0 } ?: info.capturedMillis,
                        bucketName = c.stringOrNull(MediaStore.Images.Media.BUCKET_DISPLAY_NAME) ?: info.bucketName,
                        folderPath = if (useRelativePath) rawPath else rawPath.substringBeforeLast('/', ""),
                        durationMs = if (item.isVideo) {
                            c.longOrNull(MediaStore.Video.Media.DURATION) ?: info.durationMs
                        } else {
                            0L
                        }
                    )
                }
            }
        } catch (e: Exception) {
            // Se conserva la información básica del propio elemento.
        }

        if (!item.isVideo) {
            readExifDate(item)?.let { info = info.copy(capturedMillis = it) }
            if (info.width <= 0 || info.height <= 0) {
                readBounds(item)?.let { (w, h) -> info = info.copy(width = w, height = h) }
            }
        }
        info
    }

    /** Fecha original de captura según EXIF (DateTimeOriginal), si existe. */
    private fun readExifDate(item: MediaItem): Long? = try {
        contentResolver.openInputStream(item.uri)?.use { stream ->
            ExifInterface(stream).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(it)?.time
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun readBounds(item: MediaItem): Pair<Int, Int>? = try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    } catch (e: Exception) {
        null
    }

    private fun Cursor.longOrNull(column: String): Long? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getLong(index)
    }

    private fun Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getString(index)
    }
}
