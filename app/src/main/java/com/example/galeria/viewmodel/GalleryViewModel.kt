package com.example.galeria.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galeria.data.MediaRepository
import com.example.galeria.domain.GroupMediaByDateUseCase
import com.example.galeria.domain.model.GalleryEntry
import com.example.galeria.domain.model.GroupedGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GalleryUiState(
    val entriesByMonth: List<GalleryEntry>,
    val entriesByYear: List<GalleryEntry>,
    val isLoaded: Boolean
) {
    companion object {
        val Loading = GalleryUiState(emptyList(), emptyList(), isLoaded = false)
    }
}

/** Prepara la galería cronológica: fotos y vídeos mezclados y agrupados por mes y por año. */
class GalleryViewModel(
    repository: MediaRepository,
    private val groupByDate: GroupMediaByDateUseCase = GroupMediaByDateUseCase()
) : ViewModel() {

    val uiState: StateFlow<GalleryUiState> = repository.library
        .map { library ->
            if (!library.isLoaded) {
                GalleryUiState.Loading
            } else {
                val grouped: GroupedGallery = groupByDate(library.items)
                GalleryUiState(
                    entriesByMonth = grouped.byMonth,
                    entriesByYear = grouped.byYear,
                    isLoaded = true
                )
            }
        }
        .flowOn(Dispatchers.Default) // el agrupado de miles de elementos no toca el hilo principal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState.Loading)
}
