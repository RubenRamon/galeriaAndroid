package com.example.galeria.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galeria.data.FavoritesRepository
import com.example.galeria.data.MediaRepository
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.permissions.MediaPermissions
import com.example.galeria.domain.GroupMediaByDateUseCase
import com.example.galeria.domain.model.GalleryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Elementos favoritos que existen en la galería, ya agrupados por mes y por año. */
data class FavoritesUiState(
    val items: List<MediaItem>,
    val entriesByMonth: List<GalleryEntry>,
    val entriesByYear: List<GalleryEntry>
) {
    companion object {
        val Empty = FavoritesUiState(emptyList(), emptyList(), emptyList())
    }
}

/** Favoritos: estado del corazón de cada elemento y contenido de la pantalla Favoritos. */
class FavoritesViewModel(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val permissions: MediaPermissions,
    private val groupByDate: GroupMediaByDateUseCase = GroupMediaByDateUseCase()
) : ViewModel() {

    /** Claves de todos los favoritos: sirve para pintar el corazón de cualquier elemento. */
    val favoriteKeys: StateFlow<Set<String>> = favoritesRepository.keys

    val uiState: StateFlow<FavoritesUiState> =
        combine(mediaRepository.library, favoritesRepository.keys) { library, keys ->
            if (!library.isLoaded || keys.isEmpty()) {
                FavoritesUiState.Empty
            } else {
                // Los favoritos que no están en la galería (por ejemplo, en la papelera) no se muestran.
                val items = library.items.filter { it.key in keys }
                val grouped = groupByDate(items)
                FavoritesUiState(items, grouped.byMonth, grouped.byYear)
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState.Empty)

    init {
        // Si un archivo se elimina definitivamente, su favorito desaparece. Solo con acceso total,
        // porque con acceso parcial un archivo ausente no significa que se haya borrado.
        viewModelScope.launch {
            mediaRepository.library.collect { library ->
                if (library.isLoaded && library.items.isNotEmpty() && permissions.hasFullMediaAccess()) {
                    val keys = withContext(Dispatchers.Default) { library.items.mapTo(HashSet()) { it.key } }
                    favoritesRepository.pruneMissing(keys)
                }
            }
        }
    }

    fun toggle(item: MediaItem) = favoritesRepository.toggle(item)
}
