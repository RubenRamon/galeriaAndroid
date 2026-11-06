package com.example.galeria.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.thumbnails.ThumbnailLoader
import com.example.galeria.ui.components.MediaThumbnail
import com.example.galeria.ui.components.VideoBadge
import com.example.galeria.ui.theme.GalleryGreyDark
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ItemSize = 48.dp
private val ItemSpacing = 4.dp

/**
 * Carrusel horizontal con las miniaturas de [items] (la misma lista que recorre el visor: la galería
 * general o el álbum de origen). El elemento actual permanece centrado.
 *  - Tocar una miniatura muestra ese elemento.
 *  - Arrastrar el carrusel cambia el contenido principal y, al soltar, se centra la miniatura más cercana.
 *  - Cambiar de elemento en el visor recoloca el carrusel.
 * Solo se compone mientras los controles están visibles.
 */
@Composable
internal fun ThumbnailCarousel(
    items: List<MediaItem>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val stridePx = remember(density) { with(density) { (ItemSize + ItemSpacing).toPx() } }
    val carouselState = rememberLazyListState(initialFirstVisibleItemIndex = pagerState.currentPage)
    var userScrolling by remember { mutableStateOf(false) }
    val currentPage = pagerState.currentPage

    LaunchedEffect(carouselState, pagerState) {
        // 1) Detecta que el usuario ha empezado a arrastrar el carrusel (frente a un desplazamiento automático).
        launch {
            carouselState.interactionSource.interactions.collect {
                if (it is DragInteraction.Start) userScrolling = true
            }
        }
        // 2) Mientras el usuario arrastra, el contenido principal sigue a la miniatura centrada.
        launch {
            snapshotFlow { carouselState.centeredIndex(stridePx, items.size) }
                .distinctUntilChanged()
                .collectLatest { index ->
                    if (userScrolling && index != pagerState.currentPage) pagerState.scrollToPage(index)
                }
        }
        // 3) Al terminar el arrastre (y el impulso posterior), se centra la miniatura más cercana.
        launch {
            snapshotFlow { carouselState.isScrollInProgress }.collect { scrolling ->
                if (!scrolling && userScrolling) {
                    userScrolling = false
                    val index = carouselState.centeredIndex(stridePx, items.size)
                    carouselState.animateScrollToItem(index)
                    if (pagerState.currentPage != index) pagerState.scrollToPage(index)
                }
            }
        }
        // 4) Si el elemento cambia desde el visor o al pulsar una miniatura, el carrusel se recoloca.
        launch {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collectLatest { page ->
                    if (!userScrolling && !carouselState.isScrollInProgress) {
                        carouselState.animateScrollToItem(page)
                    }
                }
        }
    }

    BoxWithConstraints(modifier.fillMaxWidth().height(60.dp)) {
        // Relleno lateral que permite centrar incluso el primer y el último elemento.
        val sidePadding = ((maxWidth - ItemSize) / 2).coerceAtLeast(0.dp)

        LazyRow(
            state = carouselState,
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            items(count = items.size, key = { items[it].key }, contentType = { "thumb" }) { index ->
                CarouselThumb(
                    item = items[index],
                    selected = index == currentPage,
                    onClick = { scope.launch { pagerState.scrollToPage(index) } }
                )
            }
        }
    }
}

@Composable
private fun CarouselThumb(item: MediaItem, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(ItemSize)
            .graphicsLayer {
                val scale = if (selected) 1.12f else 1f
                scaleX = scale
                scaleY = scale
                alpha = if (selected) 1f else 0.6f
            }
            .clip(RoundedCornerShape(4.dp))
            .background(GalleryGreyDark)
            .then(if (selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(4.dp)) else Modifier)
            .clickable(onClick = onClick)
    ) {
        MediaThumbnail(item, ThumbnailLoader.bucketFor(160), Modifier.size(ItemSize))
        if (item.isVideo) {
            VideoBadge(Modifier.align(Alignment.BottomStart).padding(2.dp), size = 14.dp)
        }
    }
}

/**
 * Índice de la miniatura centrada. Todas miden lo mismo y el relleno lateral las centra al alinearlas
 * con el inicio, así que basta con la distancia recorrida dividida entre el paso entre miniaturas.
 */
private fun LazyListState.centeredIndex(stridePx: Float, count: Int): Int {
    val scrolled = firstVisibleItemIndex * stridePx + firstVisibleItemScrollOffset
    return (scrolled / stridePx).roundToInt().coerceIn(0, (count - 1).coerceAtLeast(0))
}
