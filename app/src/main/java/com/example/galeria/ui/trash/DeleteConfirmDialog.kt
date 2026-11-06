package com.example.galeria.ui.trash

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.galeria.R
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.theme.GalleryGreyLight

/** Confirmación previa al borrado definitivo. Se muestra mientras [items] no sea null. */
@Composable
fun DeleteConfirmDialog(
    items: List<MediaItem>?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (items == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        titleContentColor = Color.White,
        textContentColor = GalleryGreyLight,
        title = { Text(stringResource(R.string.delete_dialog_title)) },
        text = {
            Text(
                if (items.size == 1) {
                    stringResource(R.string.delete_dialog_one)
                } else {
                    stringResource(R.string.delete_dialog_many, items.size)
                }
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete_dialog_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.delete_dialog_cancel)) } }
    )
}
