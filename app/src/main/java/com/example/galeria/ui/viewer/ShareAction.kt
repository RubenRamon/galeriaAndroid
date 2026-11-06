package com.example.galeria.ui.viewer

import android.content.Context
import android.widget.Toast
import com.example.galeria.R
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.sharing.MediaSharer

/** Abre el menú nativo de compartir. Si no puede abrirse, se avisa sin cerrar la app. */
internal fun shareMedia(context: Context, item: MediaItem) {
    try {
        context.startActivity(MediaSharer.createShareIntent(item))
    } catch (e: Exception) {
        Toast.makeText(context, R.string.share_error, Toast.LENGTH_SHORT).show()
    }
}
