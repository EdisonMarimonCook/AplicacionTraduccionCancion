package com.example.diccionario_hiphop.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenar palabras del diccionario offline
 */
@Entity(tableName = "dictionary_cache")
data class DictionaryWordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val translation: String,
    val context: String?,
    val language: String,
    val songName: String?,
    val artistName: String?,
    val imageUrl: String?,
    val songYoutubeUrl: String?,
    val timestampStart: Float?,
    val timestampEnd: Float?,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Entidad Room para almacenar flashcards offline
 */
@Entity(tableName = "flashcard_cache")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val wordId: String,
    val word: String,
    val translation: String,
    val language: String,
    val type: String? = "word",  // 🔥 word o expression
    val songContext: String?,
    val easeFactor: Float,
    val interval: Int,
    val repetitions: Int,
    val nextReviewDate: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Entidad Room para almacenar letras cacheadas offline
 */
@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val spotifyId: String,
    val songName: String,
    val artistName: String,
    val imageUrl: String?,
    val lyrics: String,
    val syncedLyrics: String?,
    val language: String,
    val cachedAt: Long = System.currentTimeMillis()
)
