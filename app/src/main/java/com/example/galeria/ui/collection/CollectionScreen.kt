package com.example.galeria.ui.collection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galeria.data.model.MediaItem
import com.example.galeria.domain.model.GalleryEntry
import com.example.galeria.ui.albums.albumCountText
import com.example.galeria.ui.components.BackButton
import com.example.galeria.ui.gallery.GridZoomState
import com.example.galeria.ui.gallery.ZoomableMediaGrid
import com.example.galeria.ui.theme.GalleryGreyLight
import com.example.galeria.ui.theme.GalleryWhite

/**
 * Pantalla con una colección de fotos y vídeos (un álbum, los favoritos...): misma cuadrícula,
 * mismos gestos de zoom y mismo comportamiento de pulsación que la galería principal.
 * Se dibuja encima de la pantalla desde la que se abrió.
 *
 * @param zoomKey identifica la colección: cada una conserva su propio nivel de zoom.
 * @param onMediaClick recibe el elemento pulsado y la lista completa de la colección,
 *   que es la que recorrerá el visor.
 */
@Composable
fun CollectionScreen(
    title: String,
    count: Int,
    zoomKey: Any,
    items: List<MediaItem>,
    entriesByMonth: List<GalleryEntry>,
    entriesByYear: List<GalleryEntry>,
    onBack: () -> Unit,
    onMediaClick: (item: MediaItem, collectionItems: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
    headerActions: (@Composable () -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {} // no deja pasar toques a la pantalla de debajo
    ) {
        val zoomState = rememberSaveable(zoomKey, saver = GridZoomState.Saver) { GridZoomState() }

        ZoomableMediaGrid(
            zoomState = zoomState,
            entriesFor = { yearHeaders -> if (yearHeaders) entriesByYear else entriesByMonth },
            leadingItemCount = 1,
            onMediaClick = { item -> onMediaClick(item, items) }
        ) {
            item(key = "collection_header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onBack)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            text = title,
                            color = GalleryWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = albumCountText(count), color = GalleryGreyLight, fontSize = 13.sp)
                    }
                }
                headerActions?.invoke()
                }
            }
            if (items.isEmpty() && emptyMessage != null) {
                item(key = "collection_empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(text = emptyMessage, color = GalleryGreyLight, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}
