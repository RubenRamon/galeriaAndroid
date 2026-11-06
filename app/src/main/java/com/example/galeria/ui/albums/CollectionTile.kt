package com.example.galeria.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galeria.data.model.MediaItem
import com.example.galeria.data.thumbnails.ThumbnailLoader
import com.example.galeria.ui.components.MediaThumbnail
import com.example.galeria.ui.theme.GalleryGreyDark
import com.example.galeria.ui.theme.GalleryGreyLight
import com.example.galeria.ui.theme.GalleryWhite
import androidx.compose.foundation.Canvas

private val TileHeight = 76.dp
private val CoverSize = 60.dp

/**
 * Rectángulo de una colección especial: imagen representativa a la izquierda, nombre y cantidad
 * en el centro e icono identificativo a la derecha.
 */
@Composable
internal fun CollectionTile(
    title: String,
    count: Int,
    cover: MediaItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawScope.(Color) -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(TileHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(GalleryGreyDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(CoverSize)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            if (cover != null) {
                MediaThumbnail(cover, ThumbnailLoader.bucketFor(160), Modifier.size(CoverSize))
            } else {
                // Sin elementos: el icono de la colección ocupa el lugar de la imagen.
                Canvas(Modifier.align(Alignment.Center).size(24.dp)) { icon(Color.White.copy(alpha = 0.35f)) }
            }
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = title,
                color = GalleryWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = count.toString(), color = GalleryGreyLight, fontSize = 13.sp)
        }
        Spacer(Modifier.width(4.dp))
        Canvas(Modifier.size(20.dp)) { icon(GalleryGreyLight) }
        Spacer(Modifier.width(4.dp))
    }
}
