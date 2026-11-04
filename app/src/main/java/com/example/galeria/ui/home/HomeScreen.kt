package com.example.galeria.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.albums.AlbumDetailScreen
import com.example.galeria.ui.albums.AlbumsScreen
import com.example.galeria.ui.gallery.GalleryScreen
import com.example.galeria.viewmodel.AlbumsViewModel
import com.example.galeria.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

private const val PAGE_ALBUMS = 0
private const val PAGE_GALLERY = 1

/**
 * Pantalla principal con dos páginas horizontales: [álbumes | fotos].
 * Se abre en las fotos; deslizar el dedo hacia la derecha muestra los álbumes
 * y deslizarlo hacia la izquierda vuelve a las fotos.
 */
@Composable
fun HomeScreen(
    galleryViewModel: GalleryViewModel,
    albumsViewModel: AlbumsViewModel,
    showPartialBanner: Boolean,
    onSelectMore: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFromGallery: (MediaItem) -> Unit,
    onOpenFromAlbum: (item: MediaItem, albumItems: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = PAGE_GALLERY, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    val albumsState by albumsViewModel.uiState.collectAsStateWithLifecycle()
    val selectedBucketId by albumsViewModel.selectedBucketId.collectAsStateWithLifecycle()
    val albumContent by albumsViewModel.albumContent.collectAsStateWithLifecycle()

    // Atrás en la lista de álbumes vuelve a las fotos.
    BackHandler(enabled = selectedBucketId == null && pagerState.currentPage == PAGE_ALBUMS) {
        scope.launch { pagerState.animateScrollToPage(PAGE_GALLERY) }
    }

    Box(modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1, // ambas páginas siguen vivas: conservan su posición
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == PAGE_ALBUMS) {
                AlbumsScreen(
                    state = albumsState,
                    onAlbumClick = { album -> albumsViewModel.openAlbum(album.bucketId) }
                )
            } else {
                GalleryScreen(
                    viewModel = galleryViewModel,
                    showPartialBanner = showPartialBanner,
                    onSelectMore = onSelectMore,
                    onOpenSettings = onOpenSettings,
                    onMediaClick = onOpenFromGallery
                )
            }
        }

        if (selectedBucketId != null) {
            AlbumDetailScreen(
                // Evita mostrar un instante el contenido del álbum anterior.
                content = albumContent?.takeIf { it.album.bucketId == selectedBucketId },
                onBack = albumsViewModel::closeAlbum,
                onMediaClick = onOpenFromAlbum
            )
        }
    }
}
