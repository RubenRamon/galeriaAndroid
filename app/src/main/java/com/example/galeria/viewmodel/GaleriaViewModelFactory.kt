package com.example.galeria.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.galeria.AppContainer

/** Crea los ViewModels con sus dependencias. Cada fase añade aquí los suyos. */
class GaleriaViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) ->
            MainViewModel(container.mediaRepository, container.mediaPermissions) as T

        modelClass.isAssignableFrom(GalleryViewModel::class.java) ->
            GalleryViewModel(container.mediaRepository) as T

        else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
