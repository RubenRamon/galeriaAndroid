@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.galeria.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galeria.R
import com.example.galeria.data.MediaInfoLoader
import com.example.galeria.data.model.MediaInfo
import com.example.galeria.data.model.MediaItem
import com.example.galeria.ui.components.formatDuration
import java.text.DateFormat
import java.util.Date
import java.util.Locale

val LocalMediaInfoLoader = staticCompositionLocalOf<MediaInfoLoader> {
    error("No se ha proporcionado MediaInfoLoader")
}

private val PanelBackground = Color(0xFF1C1C1E)
private val LabelColor = Color(0xFF9A9A9A)

/** Panel inferior con los datos del elemento. Pulsar fuera del panel lo cierra. */
@Composable
internal fun MediaInfoPanel(
    item: MediaItem,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(tween(200)) { it },
                exit = slideOutVertically(tween(150)) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                InfoContent(item)
            }
        }
    }
}

@Composable
private fun InfoContent(item: MediaItem) {
    val context = LocalContext.current
    val loader = LocalMediaInfoLoader.current
    // Se muestra enseguida lo que ya se sabe del elemento y se completa al consultar MediaStore.
    val info by produceState(initialValue = MediaInfo.from(item), item.key) {
        value = loader.load(item)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(PanelBackground)
            // Absorbe los toques para que pulsar dentro del panel no lo cierre.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.info_title),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        InfoRow(stringResource(R.string.info_name), info.displayName)
        InfoRow(
            stringResource(R.string.info_type),
            stringResource(if (info.isVideo) R.string.info_type_video else R.string.info_type_photo)
        )
        InfoRow(
            stringResource(R.string.info_captured),
            DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(Date(info.capturedMillis))
        )
        if (info.sizeBytes > 0L) {
            InfoRow(
                stringResource(R.string.info_size),
                android.text.format.Formatter.formatFileSize(context, info.sizeBytes)
            )
        }
        if (info.width > 0 && info.height > 0) {
            InfoRow(stringResource(R.string.info_resolution), resolutionText(info.width, info.height))
        }
        if (info.isVideo && info.durationMs > 0L) {
            InfoRow(stringResource(R.string.info_duration), formatDuration(info.durationMs))
        }
        val format = listOfNotNull(info.extension.takeIf { it.isNotBlank() }, info.mimeType).joinToString(" · ")
        if (format.isNotBlank()) {
            InfoRow(stringResource(R.string.info_format), format)
        }
        InfoRow(stringResource(R.string.info_album), info.bucketName)
        if (info.folderPath.isNotBlank()) {
            InfoRow(stringResource(R.string.info_path), info.folderPath)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = label, color = LabelColor, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 15.sp)
    }
}

private fun resolutionText(width: Int, height: Int): String {
    val megapixels = width.toLong() * height / 1_000_000.0
    return if (megapixels >= 0.1) {
        String.format(Locale.getDefault(), "%d × %d · %.1f MP", width, height, megapixels)
    } else {
        "$width × $height"
    }
}
