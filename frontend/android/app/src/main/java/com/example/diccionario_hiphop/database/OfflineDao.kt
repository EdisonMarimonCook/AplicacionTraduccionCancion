package com.example.diccionario_hiphop.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con el diccionario offline
 */
@Dao
interface DictionaryDao {
    
    @Query("SELECT * FROM dictionary_cache ORDER BY createdAt DESC")
    fun getAllWords(): Flow<List<DictionaryWordEntity>>
    
    @Query("SELECT * FROM dictionary_cache WHERE id = :wordId")
    suspend fun getWordById(wordId: String): DictionaryWordEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<DictionaryWordEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: DictionaryWordEntity)
    
    @Query("DELETE FROM dictionary_cache WHERE id = :wordId")
    suspend fun deleteWord(wordId: String)
    
    @Query("DELETE FROM dictionary_cache")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM dictionary_cache")
    suspend fun getWordCount(): Int
}

/**
 * DAO para operaciones con flashcards offline
 */
@Dao
interface FlashcardDao {
    
    @Query("SELECT * FROM flashcard_cache ORDER BY nextReviewDate ASC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>
    
    @Query("SELECT * FROM flashcard_cache WHERE nextReviewDate <= :today ORDER BY nextReviewDate ASC")
    fun getDueFlashcards(today: String): Flow<List<FlashcardEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)
    
    @Query("DELETE FROM flashcard_cache WHERE id = :flashcardId")
    suspend fun deleteFlashcard(flashcardId: String)
    
    @Query("DELETE FROM flashcard_cache")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM flashcard_cache WHERE nextReviewDate <= :today")
    suspend fun getDueCount(today: String): Int
}

/**
 * DAO para operaciones con letras cacheadas
 */
@Dao
interface LyricsDao {
    
    @Query("SELECT * FROM lyrics_cache WHERE spotifyId = :spotifyId")
    suspend fun getLyrics(spotifyId: String): LyricsCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsCacheEntity)
    
    @Query("DELETE FROM lyrics_cache WHERE cachedAt < :cutoffTime")
    suspend fun deleteOldCache(cutoffTime: Long)
    
    @Query("DELETE FROM lyrics_cache")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM lyrics_cache")
    suspend fun getCacheCount(): Int
}
