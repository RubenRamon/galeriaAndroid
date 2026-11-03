package com.example.galeria.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// La galería es siempre negra, independientemente del tema del sistema.
private val GalleryColorScheme = darkColorScheme(
    primary = GalleryAccent,
    onPrimary = GalleryBlack,
    background = GalleryBlack,
    onBackground = GalleryWhite,
    surface = GalleryBlack,
    onSurface = GalleryWhite,
    surfaceVariant = GalleryGreyDark,
    onSurfaceVariant = GalleryGreyLight,
    outline = GalleryGreyLight
)

@Composable
fun GaleriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GalleryColorScheme,
        typography = Typography,
        content = content
    )
}
