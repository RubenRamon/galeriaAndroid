package com.example.galeria.ui.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.galeria.R
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.components.PartialAccessBanner
import com.example.galeria.ui.theme.GalleryGreyLight
import com.example.galeria.viewmodel.GalleryViewModel

/** Galería cronológica continua: título «Fotos» y fotos + vídeos agrupados por mes o año. */
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    showPartialBanner: Boolean,
    onSelectMore: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onMediaClick: (MediaItem) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val zoomState = rememberSaveable(saver = GridZoomState.Saver) { GridZoomState() }

    ZoomableMediaGrid(
        zoomState = zoomState,
        entriesFor = { yearHeaders -> if (yearHeaders) state.entriesByYear else state.entriesByMonth },
        leadingItemCount = if (showPartialBanner) 2 else 1, // título (+ aviso de acceso parcial)
        onMediaClick = onMediaClick,
        modifier = modifier
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }, contentType = "title") {
            GalleryTitle()
        }

        if (showPartialBanner) {
            item(key = "partial", span = { GridItemSpan(maxLineSpan) }, contentType = "banner") {
                PartialAccessBanner(onSelectMore = onSelectMore, onOpenSettings = onOpenSettings)
            }
        }

        if (!state.isLoaded) {
            item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.entriesByMonth.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.gallery_empty),
                    color = GalleryGreyLight,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun GalleryTitle() {
    Text(
        text = stringResource(R.string.gallery_title),
        color = GalleryGreyLight,
        fontSize = 32.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.padding(start = 24.dp, top = 56.dp, bottom = 24.dp)
    )
}
