package com.example.galeria.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galeria.data.MediaRepository
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.model.MediaLibrary
import com.example.galeria.data.permissions.MediaAccess
import com.example.galeria.data.permissions.MediaPermissions
import com.example.galeria.domain.model.ViewerRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado global: permisos, contenido multimedia disponible y visor abierto. */
class MainViewModel(
    private val repository: MediaRepository,
    private val permissions: MediaPermissions
) : ViewModel() {

    private val _access = MutableStateFlow(permissions.currentAccess())
    val access: StateFlow<MediaAccess> = _access.asStateFlow()

    val library: StateFlow<MediaLibrary> = repository.library

    private val _viewer = MutableStateFlow<ViewerRequest?>(null)

    /** Visor abierto, o null. Sobrevive a giros de pantalla. */
    val viewer: StateFlow<ViewerRequest?> = _viewer.asStateFlow()

    val permissionsToRequest: Array<String> get() = permissions.permissionsToRequest

    init {
        // Reacciona a archivos nuevos, borrados o movidos mientras la app está abierta.
        viewModelScope.launch {
            repository.observeChanges().collect {
                if (_access.value != MediaAccess.DENIED) repository.refresh()
            }
        }
    }

    /** Se llama cada vez que la app vuelve a primer plano. */
    fun onAppResumed() {
        val newAccess = permissions.currentAccess()
        _access.value = newAccess
        if (newAccess == MediaAccess.DENIED) {
            closeViewer()
            repository.clear()
        } else {
            viewModelScope.launch { repository.refresh() }
        }
    }

    fun onPermissionResult() = onAppResumed()

    fun canRequestAgain(activity: Activity): Boolean = permissions.canRequestAgain(activity)

    /**
     * Abre el visor en [item]. [items] es la lista por la que se podrá deslizar:
     * toda la galería por defecto, o el contenido de un álbum.
     */
    fun openViewer(
        item: MediaItem,
        items: List<MediaItem> = library.value.items,
        fromTrash: Boolean = false
    ) {
        val index = items.indexOfFirst { it.key == item.key }
        if (index >= 0) _viewer.value = ViewerRequest(items, index, fromTrash)
    }

    /**
     * Quita elementos de la lista del visor (se han enviado a la papelera, restaurado o eliminado).
     * El visor pasa al elemento siguiente y se cierra si ya no queda ninguno.
     */
    fun removeFromViewer(removed: List<MediaItem>) {
        val keys = removed.mapTo(HashSet()) { it.key }
        _viewer.update { request ->
            if (request == null) {
                null
            } else {
                val remaining = request.items.filter { it.key !in keys }
                if (remaining.isEmpty()) {
                    null
                } else {
                    request.copy(items = remaining, startIndex = request.startIndex.coerceAtMost(remaining.lastIndex))
                }
            }
        }
    }

    fun closeViewer() {
        _viewer.value = null
    }
}
