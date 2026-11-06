package com.example.galeria.data.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class MediaAccess {
    /** Acceso a todo el contenido multimedia. */
    FULL,

    /** Android 14+: solo los elementos que el usuario ha seleccionado. */
    PARTIAL,

    DENIED
}

/** Decide qué permisos hacen falta según la versión de Android y en qué estado están. */
class MediaPermissions(private val context: Context) {

    /** Permisos a solicitar en esta versión de Android. */
    val permissionsToRequest: Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun currentAccess(): MediaAccess = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> when {
            // Si se concede imágenes o vídeos completos, hay acceso total a ese tipo.
            isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
                isGranted(Manifest.permission.READ_MEDIA_VIDEO) -> MediaAccess.FULL

            isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.PARTIAL
            else -> MediaAccess.DENIED
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            if (isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
                isGranted(Manifest.permission.READ_MEDIA_VIDEO)
            ) MediaAccess.FULL else MediaAccess.DENIED

        else ->
            if (isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) MediaAccess.FULL else MediaAccess.DENIED
    }

    /**
     * true solo si se puede ver TODO el contenido (imágenes y vídeos). Se usa para decidir si es seguro
     * dar por desaparecido un archivo: con acceso parcial, un archivo ausente no implica que se haya borrado.
     */
    fun hasFullMediaAccess(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            isGranted(Manifest.permission.READ_MEDIA_IMAGES) && isGranted(Manifest.permission.READ_MEDIA_VIDEO)

        else -> isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * true si Android todavía mostrará el diálogo de permiso.
     * Si es false tras una denegación, solo se puede activar desde Ajustes.
     */
    fun canRequestAgain(activity: Activity): Boolean =
        permissionsToRequest.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
