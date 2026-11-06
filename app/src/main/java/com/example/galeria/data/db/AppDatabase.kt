package com.example.galeria.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos propia de la aplicación (SQLite del sistema, sin librerías ni plugins adicionales).
 * Cada función nueva que necesite guardar datos (álbumes propios, etc.) añade aquí su tabla
 * y sube [VERSION] con su migración en [onUpgrade].
 */
class AppDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${FavoritesStore.TABLE} (
                ${FavoritesStore.COL_TYPE} INTEGER NOT NULL,
                ${FavoritesStore.COL_ID} INTEGER NOT NULL,
                ${FavoritesStore.COL_ADDED_AT} INTEGER NOT NULL,
                PRIMARY KEY (${FavoritesStore.COL_TYPE}, ${FavoritesStore.COL_ID})
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Sin migraciones todavía: la versión 1 es la primera.
    }

    companion object {
        const val NAME = "galeria.db"
        const val VERSION = 1
    }
}
