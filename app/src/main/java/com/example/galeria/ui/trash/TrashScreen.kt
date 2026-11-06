package com.example.galeria.ui.trash

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.galeria.R
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.collection.CollectionScreen
import com.example.galeria.viewmodel.TrashUiState

/**
 * Elementos enviados a la papelera. Misma cuadrícula que el resto de colecciones; al abrir un
 * elemento, el visor ofrece restaurarlo o eliminarlo definitivamente. Arriba hay acciones para todo.
 */
@Composable
fun TrashScreen(
    state: TrashUiState,
    onBack: () -> Unit,
    onMediaClick: (item: MediaItem, trashItems: List<MediaItem>) -> Unit,
    onRestoreAll: () -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    CollectionScreen(
        title = stringResource(R.string.trash_title),
        count = state.items.size,
        zoomKey = "trash",
        items = state.items,
        entriesByMonth = state.entriesByMonth,
        entriesByYear = state.entriesByYear,
        onBack = onBack,
        onMediaClick = onMediaClick,
        modifier = modifier,
        emptyMessage = stringResource(R.string.trash_empty),
        headerActions = if (state.items.isEmpty()) {
            null
        } else {
            {
                Row(Modifier.padding(start = 8.dp)) {
                    TextButton(onClick = onRestoreAll) { Text(stringResource(R.string.trash_restore_all)) }
                    TextButton(onClick = onEmptyTrash) { Text(stringResource(R.string.trash_empty_action)) }
                }
            }
        }
    )
}
