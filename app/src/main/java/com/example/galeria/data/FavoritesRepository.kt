package com.example.galeria.data

import com.example.galeria.data.db.FavoritesStore
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Fuente única de verdad de los favoritos. Los favoritos se guardan en la base de datos de la app y
 * se mantienen en memoria como un conjunto de claves de [MediaItem.key], de modo que cualquier pantalla
 * ve el cambio al instante. La base de datos se escribe en segundo plano.
 */
class FavoritesRepository(
    private val store: FavoritesStore,
    private val dataSource: MediaStoreDataSource,
    private val scope: CoroutineScope
) {
    private val _keys = MutableStateFlow<Set<String>>(emptySet())

    /** Claves de los elementos favoritos. */
    val keys: StateFlow<Set<String>> = _keys.asStateFlow()

    private val writeMutex = Mutex()

    init {
        scope.launch {
            val stored = try {
                store.loadAll()
            } catch (e: Exception) {
                emptyList()
            }
            _keys.update { current -> current + stored.map { (type, id) -> keyOf(type, id) } }
        }
    }

    /** Añade el elemento a favoritos, o lo quita si ya lo estaba. El cambio es inmediato. */
    fun toggle(item: MediaItem) {
        val key = item.key
        val add = key !in _keys.value
        _keys.update { if (add) it + key else it - key }

        val type = typeCode(item.type)
        scope.launch {
            writeMutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        if (add) store.insert(type, item.id) else store.delete(type, item.id)
                    }
                } catch (e: Exception) {
                    // Si no se pudo guardar, se deshace el cambio para no mostrar un estado falso.
                    _keys.update { if (add) it - key else it + key }
                }
            }
        }
    }

    /** Quita de favoritos los elementos eliminados definitivamente. */
    suspend fun remove(items: List<MediaItem>) {
        if (items.isEmpty()) return
        _keys.update { it - items.map { item -> item.key }.toSet() }
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                items.forEach { store.delete(typeCode(it.type), it.id) }
            }
        }
    }

    /**
     * Elimina los favoritos cuyo archivo ya no existe en MediaStore (ni siquiera en la papelera).
     * Un elemento que simplemente no aparece en [libraryKeys] NO se borra: solo se descarta si una
     * consulta directa confirma que ya no existe. Si no se puede comprobar, se conserva.
     */
    suspend fun pruneMissing(libraryKeys: Set<String>) {
        val candidates = _keys.value.filter { it !in libraryKeys }
        if (candidates.isEmpty()) return

        val gone = withContext(Dispatchers.IO) {
            candidates.filter { key ->
                val parsed = parse(key) ?: return@filter false
                dataSource.existsIncludingTrashed(parsed.first, parsed.second) == false
            }
        }
        if (gone.isEmpty()) return

        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                gone.forEach { key ->
                    parse(key)?.let { (type, id) -> store.delete(typeCode(type), id) }
                }
            }
        }
        _keys.update { it - gone.toSet() }
    }

    private fun typeCode(type: MediaType) = if (type == MediaType.VIDEO) 1 else 0

    private fun keyOf(typeCode: Int, id: Long) =
        MediaItem.keyOf(if (typeCode == 1) MediaType.VIDEO else MediaType.IMAGE, id)

    private fun parse(key: String): Pair<MediaType, Long>? {
        val id = key.drop(2).toLongOrNull() ?: return null
        return when {
            key.startsWith("v_") -> MediaType.VIDEO to id
            key.startsWith("i_") -> MediaType.IMAGE to id
            else -> null
        }
    }
}
