package com.example.galeria

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.example.galeria.ui.GaleriaRoot
import com.example.galeria.ui.components.LocalThumbnailLoader
import com.example.galeria.ui.theme.GaleriaTheme
import com.example.galeria.ui.viewer.LocalMediaInfoLoader
import com.example.galeria.viewmodel.AlbumsViewModel
import com.example.galeria.viewmodel.FavoritesViewModel
import com.example.galeria.viewmodel.GalleryViewModel
import com.example.galeria.viewmodel.GaleriaViewModelFactory
import com.example.galeria.viewmodel.MainViewModel
import com.example.galeria.viewmodel.TrashViewModel

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as GaleriaApp).container

    private val viewModel: MainViewModel by viewModels { GaleriaViewModelFactory(container) }
    private val galleryViewModel: GalleryViewModel by viewModels { GaleriaViewModelFactory(container) }
    private val albumsViewModel: AlbumsViewModel by viewModels { GaleriaViewModelFactory(container) }
    private val favoritesViewModel: FavoritesViewModel by viewModels { GaleriaViewModelFactory(container) }
    private val trashViewModel: TrashViewModel by viewModels { GaleriaViewModelFactory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras transparentes con iconos claros: el fondo de la app es siempre negro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            GaleriaTheme {
                CompositionLocalProvider(
                    LocalThumbnailLoader provides container.thumbnailLoader,
                    LocalMediaInfoLoader provides container.mediaInfoLoader
                ) {
                    GaleriaRoot(viewModel, galleryViewModel, albumsViewModel, favoritesViewModel, trashViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Vuelve a comprobar permisos y contenido al regresar a la app.
        viewModel.onAppResumed()
    }
}
