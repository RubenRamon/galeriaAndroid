@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.galeria.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
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

/**
 * Galería cronológica continua: título «Bienvenido…» y fotos + vídeos agrupados por mes o año.
 * Al desplazarse, el título se desvanece y baja una barra translúcida «Fototeca».
 */
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
    val gridState = rememberLazyGridState()
    var titleHeightPx by remember { mutableIntStateOf(0) }

    // 0 = título completo a la vista, 1 = título fuera de pantalla. Se lee solo al dibujar.
    val collapseProgress: () -> Float = { titleCollapseProgress(gridState, titleHeightPx) }

    Box(modifier.fillMaxSize()) {
        ZoomableMediaGrid(
            zoomState = zoomState,
            entriesFor = { yearHeaders -> if (yearHeaders) state.entriesByYear else state.entriesByMonth },
            leadingItemCount = if (showPartialBanner) 2 else 1, // título (+ aviso de acceso parcial)
            onMediaClick = onMediaClick,
            bottomPadding = GalleryConfig.BOTTOM_ISLAND_CLEARANCE,
            gridState = gridState
        ) {
            item(key = "title", span = { GridItemSpan(maxLineSpan) }, contentType = "title") {
                GalleryTitle(
                    Modifier
                        .onSizeChanged { titleHeightPx = it.height }
                        .graphicsLayer { alpha = 1f - collapseProgress() }
                )
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

        CollapsedTitleBar(
            title = stringResource(R.string.gallery_collapsed_title),
            progress = collapseProgress,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

/** Cuánto del título ha salido de pantalla, de 0 a 1. */
private fun titleCollapseProgress(state: LazyGridState, titleHeightPx: Int): Float {
    if (state.firstVisibleItemIndex > 0) return 1f
    if (titleHeightPx <= 0) return 0f
    return (state.firstVisibleItemScrollOffset / titleHeightPx.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun GalleryTitle(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.gallery_title),
            color = Color(0xFFFFFFFF), // blanco totalmente opaco
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp)
        )
    }
}

/**
 * Barra superior translúcida con el título «Fototeca». Baja desde arriba y se vuelve visible a la
 * vez que el título grande sale de pantalla; las miniaturas pasan por debajo.
 * No intercepta toques: los gestos llegan a la cuadrícula.
 */
@Composable
private fun CollapsedTitleBar(title: String, progress: () -> Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = progress()
                alpha = p
                translationY = -(1f - p) * size.height
            }
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.80f), Color.Black.copy(alpha = 0.50f))
                )
            )
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 12.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFFFFFFFF),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
