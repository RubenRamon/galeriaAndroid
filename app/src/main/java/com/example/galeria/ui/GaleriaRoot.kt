package com.example.galeria.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.galeria.R
import com.example.galeria.data.permissions.MediaAccess
import com.example.galeria.ui.home.HomeScreen
import com.example.galeria.ui.viewer.MediaViewer
import com.example.galeria.ui.permissions.PermissionScreen
import com.example.galeria.ui.trash.DeleteConfirmDialog
import com.example.galeria.viewmodel.AlbumsViewModel
import com.example.galeria.viewmodel.FavoritesViewModel
import com.example.galeria.viewmodel.GalleryViewModel
import com.example.galeria.viewmodel.MainViewModel
import com.example.galeria.viewmodel.TrashAction
import com.example.galeria.viewmodel.TrashEvent
import com.example.galeria.viewmodel.TrashViewModel

/** Raíz de la interfaz: decide si mostrar la pantalla de permisos o el contenido. */
@Composable
fun GaleriaRoot(
    viewModel: MainViewModel,
    galleryViewModel: GalleryViewModel,
    albumsViewModel: AlbumsViewModel,
    favoritesViewModel: FavoritesViewModel,
    trashViewModel: TrashViewModel
) {
    val context = LocalContext.current
    val access by viewModel.access.collectAsStateWithLifecycle()
    val viewer by viewModel.viewer.collectAsStateWithLifecycle()
    val favoriteKeys by favoritesViewModel.favoriteKeys.collectAsStateWithLifecycle()

    val deleteConfirmation by trashViewModel.deleteConfirmation.collectAsStateWithLifecycle()

    // Diálogo del sistema que pide confirmar enviar a la papelera, restaurar o eliminar.
    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        trashViewModel.onSystemResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(trashViewModel) {
        trashViewModel.requests.collect { sender ->
            trashLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }
    LaunchedEffect(trashViewModel) {
        trashViewModel.events.collect { event ->
            when (event) {
                is TrashEvent.Completed -> {
                    // El visor deja de mostrar lo que ya no está donde estaba.
                    viewModel.removeFromViewer(event.items)
                    val message = when (event.action) {
                        TrashAction.TRASH -> R.string.trash_done_trashed
                        TrashAction.RESTORE -> R.string.trash_done_restored
                        TrashAction.DELETE -> R.string.trash_done_deleted
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                is TrashEvent.Failed -> Toast.makeText(context, R.string.trash_failed, Toast.LENGTH_LONG).show()
                TrashEvent.Unsupported -> Toast.makeText(context, R.string.trash_unsupported, Toast.LENGTH_LONG).show()
            }
        }
    }

    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        requestedOnce = true
        viewModel.onPermissionResult()
    }

    val requestAccess: () -> Unit = { launcher.launch(viewModel.permissionsToRequest) }
    val openSettings: () -> Unit = remember(context) {
        {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (access == MediaAccess.DENIED) {
            val activity = context.findActivity()
            val canAskAgain = !requestedOnce || (activity != null && viewModel.canRequestAgain(activity))
            PermissionScreen(
                requestedBefore = requestedOnce,
                canAskAgain = canAskAgain,
                onRequestAccess = requestAccess,
                onOpenSettings = openSettings
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    galleryViewModel = galleryViewModel,
                    albumsViewModel = albumsViewModel,
                    favoritesViewModel = favoritesViewModel,
                    trashViewModel = trashViewModel,
                    showPartialBanner = access == MediaAccess.PARTIAL,
                    onSelectMore = requestAccess,
                    onOpenSettings = openSettings,
                    onOpenFromGallery = { item -> viewModel.openViewer(item) },
                    onOpenFromAlbum = { item, albumItems -> viewModel.openViewer(item, albumItems) },
                    onOpenFromTrash = { item, trashItems -> viewModel.openViewer(item, trashItems, fromTrash = true) }
                )
                // El visor se dibuja encima: la galería y los álbumes de debajo conservan su posición.
                viewer?.let { request ->
                    MediaViewer(
                        items = request.items,
                        startIndex = request.startIndex,
                        onClose = viewModel::closeViewer,
                        favoriteKeys = favoriteKeys,
                        onToggleFavorite = favoritesViewModel::toggle,
                        onTrash = { trashViewModel.trash(listOf(it)) },
                        onRestore = { trashViewModel.restore(listOf(it)) },
                        onDeleteForever = { trashViewModel.requestDeleteConfirmation(listOf(it)) },
                        trashMode = request.fromTrash
                    )
                }
            }
        }
    }

    DeleteConfirmDialog(
        items = deleteConfirmation,
        onConfirm = trashViewModel::confirmDelete,
        onDismiss = trashViewModel::dismissDeleteConfirmation
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
