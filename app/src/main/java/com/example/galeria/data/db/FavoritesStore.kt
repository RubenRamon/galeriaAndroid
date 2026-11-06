package com.example.galeria.data.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Acceso a la tabla de favoritos. Un favorito se identifica por (tipo, id de MediaStore); la clave
 * primaria impide duplicados. Todas las funciones son bloqueantes: llamarlas fuera del hilo principal.
 */
class FavoritesStore(private val database: AppDatabase) {

    /** Devuelve pares (tipo, id): tipo 0 = imagen, 1 = vídeo. */
    fun loadAll(): List<Pair<Int, Long>> {
        val result = ArrayList<Pair<Int, Long>>()
        database.readableDatabase.query(
            TABLE, arrayOf(COL_TYPE, COL_ID), null, null, null, null, null
        ).use { cursor ->
            val typeCol = cursor.getColumnIndexOrThrow(COL_TYPE)
            val idCol = cursor.getColumnIndexOrThrow(COL_ID)
            while (cursor.moveToNext()) {
                result.add(cursor.getInt(typeCol) to cursor.getLong(idCol))
            }
        }
        return result
    }

    fun insert(type: Int, id: Long) {
        val values = ContentValues().apply {
            put(COL_TYPE, type)
            put(COL_ID, id)
            put(COL_ADDED_AT, System.currentTimeMillis())
        }
        database.writableDatabase.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun delete(type: Int, id: Long) {
        database.writableDatabase.delete(
            TABLE, "$COL_TYPE = ? AND $COL_ID = ?", arrayOf(type.toString(), id.toString())
        )
    }

    companion object {
        const val TABLE = "favorites"
        const val COL_TYPE = "media_type"
        const val COL_ID = "media_id"
        const val COL_ADDED_AT = "added_at"
    }
}
