package com.example.galeria.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.galeria.R

/**
 * Tres situaciones:
 *  1. Primera vez: explica para qué se necesita el acceso.
 *  2. Denegado pero Android aún mostrará el diálogo: permite volver a solicitarlo.
 *  3. Denegado definitivamente: lleva a Ajustes.
 */
@Composable
fun PermissionScreen(
    requestedBefore: Boolean,
    canAskAgain: Boolean,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blocked = requestedBefore && !canAskAgain

    val titleRes: Int
    val bodyRes: Int
    val buttonRes: Int
    when {
        !requestedBefore -> {
            titleRes = R.string.permission_title
            bodyRes = R.string.permission_body
            buttonRes = R.string.permission_button_grant
        }

        blocked -> {
            titleRes = R.string.permission_blocked_title
            bodyRes = R.string.permission_blocked_body
            buttonRes = R.string.permission_button_settings
        }

        else -> {
            titleRes = R.string.permission_denied_title
            bodyRes = R.string.permission_denied_body
            buttonRes = R.string.permission_button_retry
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = if (blocked) onOpenSettings else onRequestAccess) {
            Text(stringResource(buttonRes))
        }
    }
}
