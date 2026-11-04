package com.example.galeria.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlin.math.max

/**
 * Decodifica una foto para el visor a pantalla completa, reducida a un tamaño máximo
 * para no agotar la memoria con fotos de muchos megapíxeles.
 */
object ViewerImageDecoder {

    const val DEFAULT_MAX_DIMENSION = 3072

    /** Devuelve null si el archivo ya no existe o no se puede leer. */
    fun decode(resolver: ContentResolver, uri: Uri, maxDimension: Int = DEFAULT_MAX_DIMENSION): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            // ImageDecoder aplica automáticamente la orientación EXIF.
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val sample = sampleSizeFor(info.size.width, info.size.height, maxDimension)
                if (sample > 1) decoder.setTargetSampleSize(sample)
            }
        } else {
            decodeLegacy(resolver, uri, maxDimension)
        }
    } catch (e: Exception) {
        null
    }

    private fun decodeLegacy(resolver: ContentResolver, uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        val longest = max(width, height)
        var sample = 1
        while (longest / sample > maxDimension) sample *= 2
        return sample
    }
}
