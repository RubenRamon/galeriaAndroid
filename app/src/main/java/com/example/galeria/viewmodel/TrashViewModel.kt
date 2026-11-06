package com.example.galeria.viewmodel

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galeria.data.FavoritesRepository
import com.example.galeria.data.MediaRepository
import com.example.galeria.data.TrashRepository
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.permissions.MediaAccess
import com.example.galeria.data.permissions.MediaPermissions
import com.example.galeria.domain.GroupMediaByDateUseCase
import com.example.galeria.domain.model.GalleryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TrashAction { TRASH, RESTORE, DELETE }

/** Resultado de una operación de papelera, para avisar al usuario y actualizar el visor. */
sealed interface TrashEvent {
    data class Completed(val action: TrashAction, val items: List<MediaItem>) : TrashEvent
    data class Failed(val action: TrashAction) : TrashEvent
    data object Unsupported : TrashEvent
}

data class TrashUiState(
    val items: List<MediaItem>,
    val entriesByMonth: List<GalleryEntry>,
    val entriesByYear: List<GalleryEntry>
) {
    companion object {
        val Empty = TrashUiState(emptyList(), emptyList(), emptyList())
    }
}

/**
 * Papelera: contenido de la pantalla Papelera y operaciones de enviar, restaurar y eliminar.
 *
 * Cada operación pasa por el diálogo de confirmación del sistema, que la interfaz lanza al recibir
 * una solicitud por [requests] y cuyo resultado devuelve con [onSystemResult].
 */
class TrashViewModel(
    private val trashRepository: TrashRepository,
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val permissions: MediaPermissions,
    private val groupByDate: GroupMediaByDateUseCase = GroupMediaByDateUseCase()
) : ViewModel() {

    val uiState: StateFlow<TrashUiState> = trashRepository.items
        .map { items ->
            val grouped = groupByDate(items)
            TrashUiState(items, grouped.byMonth, grouped.byYear)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrashUiState.Empty)

    private val _requests = MutableSharedFlow<IntentSender>(extraBufferCapacity = 4)

    /** Solicitudes del sistema pendientes de lanzar. */
    val requests: SharedFlow<IntentSender> = _requests.asSharedFlow()

    private val _events = MutableSharedFlow<TrashEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TrashEvent> = _events.asSharedFlow()

    private val _deleteConfirmation = MutableStateFlow<List<MediaItem>?>(null)

    /** Elementos para los que se pide confirmar el borrado definitivo, o null. */
    val deleteConfirmation: StateFlow<List<MediaItem>?> = _deleteConfirmation.asStateFlow()

    // Vive en el ViewModel: si se gira la pantalla mientras el sistema pide confirmación, no se pierde.
    private var inFlight: Pair<TrashAction, List<MediaItem>>? = null

    init {
        // La papelera se actualiza al cambiar la galería (un elemento entra o sale) y ante
        // cualquier cambio de MediaStore (por ejemplo, cuando Android vacía elementos antiguos).
        viewModelScope.launch {
            mediaRepository.library.collect { refreshIfAllowed() }
        }
        viewModelScope.launch {
            mediaRepository.observeChanges().collect { refreshIfAllowed() }
        }
    }

    private suspend fun refreshIfAllowed() {
        if (permissions.currentAccess() != MediaAccess.DENIED) trashRepository.refresh()
    }

    fun trash(items: List<MediaItem>) =
        startRequest(TrashAction.TRASH, items) { trashRepository.createTrashRequest(it) }

    fun restore(items: List<MediaItem>) =
        startRequest(TrashAction.RESTORE, items) { trashRepository.createRestoreRequest(it) }

    /** Pide confirmación antes de eliminar definitivamente. */
    fun requestDeleteConfirmation(items: List<MediaItem>) {
        if (items.isNotEmpty()) _deleteConfirmation.value = items
    }

    fun dismissDeleteConfirmation() {
        _deleteConfirmation.value = null
    }

    fun confirmDelete() {
        val items = _deleteConfirmation.value ?: return
        _deleteConfirmation.value = null
        startRequest(TrashAction.DELETE, items) { trashRepository.createDeleteRequest(it) }
    }

    private fun startRequest(
        action: TrashAction,
        items: List<MediaItem>,
        create: (List<MediaItem>) -> IntentSender
    ) {
        if (items.isEmpty()) return
        if (!trashRepository.isSupported) {
            _events.tryEmit(TrashEvent.Unsupported)
            return
        }
        try {
            val sender = create(items)
            inFlight = action to items
            _requests.tryEmit(sender)
        } catch (e: Exception) {
            _events.tryEmit(TrashEvent.Failed(action))
        }
    }

    /** Resultado del diálogo del sistema: true si el usuario aceptó. */
    fun onSystemResult(accepted: Boolean) {
        val current = inFlight ?: return
        inFlight = null
        if (!accepted) return // el usuario canceló: no ha cambiado nada

        val (action, items) = current
        viewModelScope.launch {
            if (action == TrashAction.DELETE) favoritesRepository.remove(items)
            mediaRepository.refresh()
            trashRepository.refresh()
            _events.emit(TrashEvent.Completed(action, items))
        }
    }
}
