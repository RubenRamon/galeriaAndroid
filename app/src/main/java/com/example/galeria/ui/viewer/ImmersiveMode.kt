package com.example.galeria.ui.viewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Oculta o muestra las barras del sistema mientras el visor está abierto.
 * Al salir del visor las barras siempre se restauran.
 * Si el usuario desliza desde un borde, Android las muestra de forma transitoria.
 */
@Composable
internal fun ViewerSystemBars(hidden: Boolean) {
    val view = LocalView.current
    val controller = remember(view) {
        view.context.findHostActivity()?.window?.let { WindowCompat.getInsetsController(it, view) }
    }

    LaunchedEffect(controller, hidden) {
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(controller) {
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
