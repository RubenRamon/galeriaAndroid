package com.example.galeria.ui.albums

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.galeria.ui.components.BackButton
import com.example.galeria.ui.gallery.GridZoomState
import com.example.galeria.ui.gallery.ZoomableMediaGrid
import com.example.galeria.ui.theme.GalleryGreyLight
import com.example.galeria.ui.theme.GalleryWhite
import com.example.galeria.viewmodel.AlbumContent

/**
 * Contenido de un álbum: misma cuadrícula, mismos gestos de zoom y mismo comportamiento
 * de pulsación que la galería principal. Se dibuja encima de la lista de álbumes.
 */
@Composable
fun AlbumDetailScreen(
    content: AlbumContent?,
    onBack: () -> Unit,
    onMediaClick: (item: MediaItem, albumItems: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {} // no deja pasar toques a la lista de álbumes de debajo
    ) {
        if (content == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            BackButton(onBack, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 48.dp))
        } else {
            // Cada álbum guarda su propio nivel de zoom.
            val zoomState = rememberSaveable(content.album.bucketId, saver = GridZoomState.Saver) {
                GridZoomState()
            }
            ZoomableMediaGrid(
                zoomState = zoomState,
                entriesFor = { yearHeaders ->
                    if (yearHeaders) content.entriesByYear else content.entriesByMonth
                },
                leadingItemCount = 1,
                onMediaClick = { item -> onMediaClick(item, content.items) }
            ) {
                item(key = "album_header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BackButton(onBack)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(
                                text = content.album.name,
                                color = GalleryWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = albumCountText(content.items.size),
                                color = GalleryGreyLight,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
