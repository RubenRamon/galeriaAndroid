package com.example.galeria.data

import com.example.galeria.data.model.Album
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.model.MediaLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Fuente única de verdad del contenido multimedia.
 * La galería y los álbumes leen de [library]; las funciones futuras
 * (favoritos, búsqueda, copias...) se apoyarán en este mismo repositorio.
 */
class MediaRepository(private val dataSource: MediaStoreDataSource) {

    private val _library = MutableStateFlow(MediaLibrary.Empty)
    val library: StateFlow<MediaLibrary> = _library.asStateFlow()

    private val refreshMutex = Mutex()

    /** Vuelve a consultar MediaStore. Solo publica un cambio si el contenido es distinto. */
    suspend fun refresh() {
        refreshMutex.withLock {
            val items = try {
                dataSource.queryAll()
            } catch (e: SecurityException) {
                emptyList()
            }
            val current = _library.value
            if (current.isLoaded && current.items == items) return

            val albums = withContext(Dispatchers.Default) { buildAlbums(items) }
            _library.value = MediaLibrary(items = items, albums = albums, isLoaded = true)
        }
    }

    /** Descarta el contenido (por ejemplo, al perder el permiso). */
    fun clear() {
        _library.value = MediaLibrary.Empty
    }

    /** Avisa de cambios en el almacenamiento, agrupando ráfagas de eventos. */
    @OptIn(FlowPreview::class)
    fun observeChanges(): Flow<Unit> = dataSource.observeChanges().debounce(CHANGE_DEBOUNCE_MS)

    /** [items] llega ordenado de más reciente a más antiguo, así que el primero de cada grupo es la portada. */
    private fun buildAlbums(items: List<MediaItem>): List<Album> =
        items.groupBy { it.bucketId }
            .map { (bucketId, group) ->
                val cover = group.first()
                Album(
                    bucketId = bucketId,
                    name = cover.bucketName,
                    folderPath = cover.folderPath,
                    cover = cover,
                    itemCount = group.size
                )
            }
            .sortedByDescending { it.cover.dateMillis }

    private companion object {
        const val CHANGE_DEBOUNCE_MS = 800L
    }
}
