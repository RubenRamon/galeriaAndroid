package com.example.galeria.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.galeria.R
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.albums.AlbumDetailScreen
import com.example.galeria.ui.albums.AlbumsScreen
import com.example.galeria.ui.collection.CollectionScreen
import com.example.galeria.ui.gallery.GalleryScreen
import com.example.galeria.ui.trash.TrashScreen
import com.example.galeria.viewmodel.AlbumsViewModel
import com.example.galeria.viewmodel.FavoritesViewModel
import com.example.galeria.viewmodel.GalleryViewModel
import com.example.galeria.viewmodel.TrashViewModel
import kotlinx.coroutines.launch

private const val PAGE_GALLERY = 0
private const val PAGE_ALBUMS = 1

/**
 * Pantalla principal con dos páginas horizontales: [fotos | álbumes].
 * Se abre en las fotos; deslizar el dedo hacia la izquierda muestra los álbumes
 * y deslizarlo hacia la derecha vuelve a las fotos.
 * Los álbumes y los favoritos se abren como pantallas superpuestas.
 */
@Composable
fun HomeScreen(
    galleryViewModel: GalleryViewModel,
    albumsViewModel: AlbumsViewModel,
    favoritesViewModel: FavoritesViewModel,
    trashViewModel: TrashViewModel,
    showPartialBanner: Boolean,
    onSelectMore: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFromGallery: (MediaItem) -> Unit,
    onOpenFromAlbum: (item: MediaItem, albumItems: List<MediaItem>) -> Unit,
    onOpenFromTrash: (item: MediaItem, trashItems: List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = PAGE_GALLERY, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    val albumsState by albumsViewModel.uiState.collectAsStateWithLifecycle()
    val selectedBucketId by albumsViewModel.selectedBucketId.collectAsStateWithLifecycle()
    val albumContent by albumsViewModel.albumContent.collectAsStateWithLifecycle()
    val favoritesState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    val trashState by trashViewModel.uiState.collectAsStateWithLifecycle()
    var showFavorites by rememberSaveable { mutableStateOf(false) }
    var showTrash by rememberSaveable { mutableStateOf(false) }

    // Atrás en la lista de álbumes vuelve a las fotos.
    BackHandler(
        enabled = selectedBucketId == null && !showFavorites && !showTrash &&
            pagerState.currentPage == PAGE_ALBUMS
    ) {
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
                    favoritesCount = favoritesState.items.size,
                    favoritesCover = favoritesState.items.firstOrNull(),
                    onFavoritesClick = { showFavorites = true },
                    trashCount = trashState.items.size,
                    trashCover = trashState.items.firstOrNull(),
                    onTrashClick = { showTrash = true },
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

        // La isla queda por debajo del álbum abierto y del visor: solo se ve en las dos pantallas principales.
        BottomIsland(
            isGallerySelected = pagerState.currentPage == PAGE_GALLERY,
            onGalleryClick = { scope.launch { pagerState.animateScrollToPage(PAGE_GALLERY) } },
            onAlbumsClick = { scope.launch { pagerState.animateScrollToPage(PAGE_ALBUMS) } },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (selectedBucketId != null) {
            AlbumDetailScreen(
                // Evita mostrar un instante el contenido del álbum anterior.
                content = albumContent?.takeIf { it.album.bucketId == selectedBucketId },
                onBack = albumsViewModel::closeAlbum,
                onMediaClick = onOpenFromAlbum
            )
        }

        if (showTrash) {
            TrashScreen(
                state = trashState,
                onBack = { showTrash = false },
                onMediaClick = onOpenFromTrash,
                onRestoreAll = { trashViewModel.restore(trashState.items) },
                onEmptyTrash = { trashViewModel.requestDeleteConfirmation(trashState.items) }
            )
        }

        if (showFavorites) {
            CollectionScreen(
                title = stringResource(R.string.favorites_title),
                count = favoritesState.items.size,
                zoomKey = "favorites",
                items = favoritesState.items,
                entriesByMonth = favoritesState.entriesByMonth,
                entriesByYear = favoritesState.entriesByYear,
                onBack = { showFavorites = false },
                onMediaClick = onOpenFromAlbum,
                emptyMessage = stringResource(R.string.favorites_empty)
            )
        }
    }
}
