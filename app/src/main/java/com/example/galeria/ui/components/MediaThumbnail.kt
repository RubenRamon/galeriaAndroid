package com.example.galeria.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.thumbnails.ThumbnailLoader

val LocalThumbnailLoader = staticCompositionLocalOf<ThumbnailLoader> {
    error("No se ha proporcionado ThumbnailLoader")
}

/** Miniatura cuadrada recortada. Si ya está en caché aparece sin parpadeo. */
@Composable
fun MediaThumbnail(item: MediaItem, sizePx: Int, modifier: Modifier = Modifier) {
    val loader = LocalThumbnailLoader.current
    val bitmap by produceState<ImageBitmap?>(
        initialValue = loader.getCached(item, sizePx)?.asImageBitmap(),
        item.key,
        sizePx
    ) {
        val loaded = loader.getCached(item, sizePx) ?: loader.load(item, sizePx)
        if (loaded != null) value = loaded.asImageBitmap()
    }

    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
