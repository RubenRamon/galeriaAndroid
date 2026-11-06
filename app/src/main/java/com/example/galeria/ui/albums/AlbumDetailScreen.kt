package com.example.galeria.ui.albums

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.collection.CollectionScreen
import com.example.galeria.ui.components.BackButton
import com.example.galeria.viewmodel.AlbumContent

/** Contenido de un álbum. Mientras se calcula, muestra un indicador de carga. */
@Composable
fun AlbumDetailScreen(
    content: AlbumContent?,
    onBack: () -> Unit,
    onMediaClick: (item: MediaItem, albumItems: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (content == null) {
        BackHandler(onBack = onBack)
        Box(
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {}
        ) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            BackButton(onBack, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 48.dp))
        }
    } else {
        CollectionScreen(
            title = content.album.name,
            count = content.items.size,
            zoomKey = content.album.bucketId,
            items = content.items,
            entriesByMonth = content.entriesByMonth,
            entriesByYear = content.entriesByYear,
            onBack = onBack,
            onMediaClick = onMediaClick,
            modifier = modifier
        )
    }
}
