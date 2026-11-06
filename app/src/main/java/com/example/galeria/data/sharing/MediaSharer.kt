package com.example.galeria.data.sharing

import android.content.ClipData
import android.content.Intent
import com.example.galeria.data.model.MediaItem

/** Prepara el menú nativo de compartir de Android para un elemento multimedia. */
object MediaSharer {

    /**
     * Intent de compartir el archivo ORIGINAL (no una miniatura). Se pasa su URI de MediaStore y
     * se concede permiso temporal de lectura a la aplicación elegida.
     */
    fun createShareIntent(item: MediaItem): Intent {
        val mime = item.mimeType?.takeIf { it.contains('/') }
            ?: if (item.isVideo) "video/*" else "image/*"

        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, item.uri)
            clipData = ClipData.newRawUri(item.displayName, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
