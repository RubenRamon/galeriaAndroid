package com.example.galeria.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galeria.data.MediaRepository
import com.example.galeria.data.model.Album
import com.example.galeria.data.model.MediaItem
import com.example.galeria.domain.GroupMediaByDateUseCase
import com.example.galeria.domain.model.GalleryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumsUiState(val albums: List<Album>, val isLoaded: Boolean)

/** Contenido de un álbum abierto, ya agrupado por mes y por año. */
data class AlbumContent(
    val album: Album,
    val items: List<MediaItem>,
    val entriesByMonth: List<GalleryEntry>,
    val entriesByYear: List<GalleryEntry>
)

/** Lista de álbumes (carpetas reales) y álbum abierto. */
class AlbumsViewModel(
    private val repository: MediaRepository,
    private val groupByDate: GroupMediaByDateUseCase = GroupMediaByDateUseCase()
) : ViewModel() {

    val uiState: StateFlow<AlbumsUiState> = repository.library
        .map { AlbumsUiState(albums = it.albums, isLoaded = it.isLoaded) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repository.library.value.let { AlbumsUiState(it.albums, it.isLoaded) }
        )

    private val _selectedBucketId = MutableStateFlow<Long?>(null)

    /** Identificador de carpeta (BUCKET_ID) del álbum abierto, o null. Sobrevive a giros de pantalla. */
    val selectedBucketId: StateFlow<Long?> = _selectedBucketId.asStateFlow()

    val albumContent: StateFlow<AlbumContent?> =
        combine(repository.library, _selectedBucketId) { library, bucketId ->
            val album = library.albums.firstOrNull { it.bucketId == bucketId }
            if (bucketId == null || album == null) {
                null
            } else {
                // Se filtra por identificador de carpeta, nunca por nombre: carpetas con el
                // mismo nombre en rutas distintas no se mezclan.
                val items = library.items.filter { it.bucketId == bucketId }
                val grouped = groupByDate(items)
                AlbumContent(album, items, grouped.byMonth, grouped.byYear)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Si el álbum abierto desaparece (se borran o mueven todos sus archivos), se vuelve a la lista.
        viewModelScope.launch {
            repository.library.collect { library ->
                val id = _selectedBucketId.value
                if (id != null && library.isLoaded && library.albums.none { it.bucketId == id }) {
                    _selectedBucketId.value = null
                }
            }
        }
    }

    fun openAlbum(bucketId: Long) {
        _selectedBucketId.value = bucketId
    }

    fun closeAlbum() {
        _selectedBucketId.value = null
    }
}
