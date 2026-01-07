package com.example.diccionario_hiphop.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room para almacenamiento offline
 * Versión 3: Añadido campo 'type' a FlashcardEntity para filtrado offline
 */
@Database(
    entities = [
        DictionaryWordEntity::class,
        FlashcardEntity::class,
        LyricsCacheEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class OfflineDatabase : RoomDatabase() {
    
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun lyricsDao(): LyricsDao
    
    companion object {
        @Volatile
        private var INSTANCE: OfflineDatabase? = null
        
        fun getDatabase(context: Context): OfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "offline_database"
                )
                .fallbackToDestructiveMigration() // Para desarrollo, recrear DB si cambia schema
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
