package com.example.galeria.ui.debug

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.galeria.data.model.Album
import com.example.galeria.data.model.MediaLibrary
import com.example.galeria.data.permissions.MediaAccess
import com.example.galeria.ui.components.PartialAccessBanner

/**
 * PANTALLA TEMPORAL de la Fase 1. Sirve para comprobar que los permisos y la
 * lectura de MediaStore funcionan. La Fase 2 la reemplaza por la galería real.
 */
@Composable
fun LibraryStatusScreen(
    access: MediaAccess,
    library: MediaLibrary,
    onSelectMore: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoCount = remember(library) { library.items.count { !it.isVideo } }
    val videoCount = library.items.size - photoCount

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = WindowInsets.systemBars.asPaddingValues()
    ) {
        item {
            Text(
                text = "Fase 1 · comprobación de acceso",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (access == MediaAccess.PARTIAL) {
            item {
                PartialAccessBanner(onSelectMore = onSelectMore, onOpenSettings = onOpenSettings)
            }
        }

        if (!library.isLoaded) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text("Acceso: $access", color = MaterialTheme.colorScheme.onBackground)
                    Text("Fotos: $photoCount", color = MaterialTheme.colorScheme.onBackground)
                    Text("Vídeos: $videoCount", color = MaterialTheme.colorScheme.onBackground)
                    Text("Álbumes: ${library.albums.size}", color = MaterialTheme.colorScheme.onBackground)
                }
            }
            items(library.albums, key = { it.bucketId }) { album ->
                AlbumDebugRow(album)
            }
        }
    }
}

@Composable
private fun AlbumDebugRow(album: Album) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "${album.name} · ${album.itemCount} elementos",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = album.folderPath,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
