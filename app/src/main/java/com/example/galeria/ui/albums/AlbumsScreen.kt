@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.galeria.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galeria.R
import com.example.galeria.data.model.Album
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.thumbnails.ThumbnailLoader
import com.example.galeria.ui.components.MediaThumbnail
import com.example.galeria.ui.components.drawHeartIcon
import com.example.galeria.ui.components.drawTrashIcon
import com.example.galeria.ui.components.VideoBadge
import com.example.galeria.ui.gallery.GalleryConfig
import com.example.galeria.ui.theme.GalleryGreyDark
import com.example.galeria.ui.theme.GalleryGreyLight
import com.example.galeria.ui.theme.GalleryWhite
import com.example.galeria.viewmodel.AlbumsUiState

private const val ALBUM_COLUMNS = 2

/** Lista de álbumes: una portada (el elemento más reciente), el nombre y el número de elementos. */
@Composable
fun AlbumsScreen(
    state: AlbumsUiState,
    favoritesCount: Int,
    favoritesCover: MediaItem?,
    onFavoritesClick: () -> Unit,
    trashCount: Int,
    trashCover: MediaItem?,
    onTrashClick: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gridState = rememberLazyGridState()
    val insets = WindowInsets.systemBarsIgnoringVisibility.asPaddingValues()

    // Si dos carpetas se llaman igual, se muestra además su ruta para distinguirlas.
    val duplicatedNames = remember(state.albums) {
        state.albums.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val coverSizePx = remember(maxWidth, density) {
            ThumbnailLoader.bucketFor(with(density) { (maxWidth / ALBUM_COLUMNS).roundToPx() })
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(ALBUM_COLUMNS),
            state = gridState,
            contentPadding = PaddingValues(
                top = insets.calculateTopPadding(),
                bottom = insets.calculateBottomPadding() + GalleryConfig.BOTTOM_ISLAND_CLEARANCE,
                start = 12.dp,
                end = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "albums_title", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.albums_title),
                    color = GalleryGreyLight,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(start = 12.dp, top = 56.dp, bottom = 16.dp)
                )
            }

            // Fila de colecciones especiales: Favoritos y Papelera, del mismo tamaño.
            item(key = "tile_favorites", contentType = "tile") {
                CollectionTile(
                    title = stringResource(R.string.favorites_title),
                    count = favoritesCount,
                    cover = favoritesCover,
                    onClick = onFavoritesClick
                ) { color -> drawHeartIcon(color, filled = false) }
            }
            item(key = "tile_trash", contentType = "tile") {
                CollectionTile(
                    title = stringResource(R.string.trash_title),
                    count = trashCount,
                    cover = trashCover,
                    onClick = onTrashClick
                ) { color -> drawTrashIcon(color) }
            }

            if (!state.isLoaded) {
                item(key = "albums_loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.albums.isEmpty()) {
                item(key = "albums_empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.albums_empty),
                        color = GalleryGreyLight,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                items(state.albums, key = { it.bucketId }, contentType = { "album" }) { album ->
                    AlbumCard(
                        album = album,
                        coverSizePx = coverSizePx,
                        showPath = album.name in duplicatedNames,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, coverSizePx: Int, showPath: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(GalleryGreyDark)
        ) {
            MediaThumbnail(album.cover, coverSizePx, Modifier.fillMaxSize())
            if (album.cover.isVideo) {
                VideoBadge(Modifier.align(Alignment.BottomStart).padding(6.dp), size = 24.dp)
            }
        }
        Text(
            text = album.name,
            color = GalleryWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = albumCountText(album.itemCount),
            color = GalleryGreyLight,
            fontSize = 13.sp
        )
        if (showPath) {
            Text(
                text = album.folderPath,
                color = GalleryGreyLight,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun albumCountText(count: Int): String =
    if (count == 1) {
        stringResource(R.string.album_count_one)
    } else {
        stringResource(R.string.album_count_many, count)
    }
