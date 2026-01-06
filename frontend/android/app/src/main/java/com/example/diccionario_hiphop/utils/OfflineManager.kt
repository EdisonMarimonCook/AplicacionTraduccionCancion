package com.example.diccionario_hiphop.utils

import android.content.Context
import android.util.Log
import com.example.diccionario_hiphop.UserWord
import com.example.diccionario_hiphop.FlashcardData
import com.example.diccionario_hiphop.database.DictionaryWordEntity
import com.example.diccionario_hiphop.database.FlashcardEntity
import com.example.diccionario_hiphop.database.LyricsCacheEntity
import com.example.diccionario_hiphop.database.OfflineDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor de almacenamiento offline y sincronización
 */
class OfflineManager(context: Context) {
    
    private val database = OfflineDatabase.getDatabase(context)
    private val dictionaryDao = database.dictionaryDao()
    private val flashcardDao = database.flashcardDao()
    private val lyricsDao = database.lyricsDao()
    private val prefs = context.getSharedPreferences("offline_sync", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "OfflineManager"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        private fun getTodayString(): String {
            return dateFormat.format(Date())
        }
    }
    
    // ==================== DICCIONARIO ====================
    
    /**
     * Sincroniza palabras del diccionario desde el servidor
     */
    suspend fun syncDictionary(words: List<UserWord>) {
        try {
            val entities = words.map { word ->
                DictionaryWordEntity(
                    id = word.id,
                    word = word.word,
                    translation = word.translation ?: "",
                    context = word.context,
                    language = word.language,
                    songName = null,
                    artistName = null,
                    imageUrl = null,
                    songYoutubeUrl = word.songYoutubeUrl,
                    timestampStart = word.timestampStart,
                    timestampEnd = word.timestampEnd,
                    lastSyncedAt = System.currentTimeMillis()
                )
            }
            
            dictionaryDao.insertWords(entities)
            Log.d(TAG, "✅ Diccionario sincronizado: ${entities.size} palabras")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando diccionario: ${e.message}")
        }
    }
    
    /**
     * Obtiene el diccionario desde la base de datos local
     */
    fun getDictionary(): Flow<List<DictionaryWordEntity>> {
        return dictionaryDao.getAllWords()
    }
    
    /**
     * Elimina una palabra del diccionario local y marca para sincronización
     */
    suspend fun deleteWord(wordId: String) {
        dictionaryDao.deleteWord(wordId)
        // Guardar ID para sincronizar eliminación cuando haya conexión
        savePendingDeletion(wordId)
    }
    
    /**
     * Guarda una eliminación pendiente para sincronizar
     */
    private fun savePendingDeletion(wordId: String) {
        try {
            val currentDeletions = getPendingDeletions().toMutableSet()
            currentDeletions.add(wordId)
            prefs.edit()
                .putStringSet("pending_deletions", currentDeletions)
                .apply()
            Log.d(TAG, "💾 Eliminación pendiente guardada: $wordId")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando eliminación pendiente: ${e.message}")
        }
    }
    
    /**
     * Obtiene IDs de palabras eliminadas localmente pendientes de sincronizar
     */
    fun getPendingDeletions(): Set<String> {
        return prefs.getStringSet("pending_deletions", emptySet()) ?: emptySet()
    }
    
    /**
     * Marca una eliminación como sincronizada (la borra de pendientes)
     */
    fun markDeletionSynced(wordId: String) {
        try {
            val currentDeletions = getPendingDeletions().toMutableSet()
            currentDeletions.remove(wordId)
            prefs.edit()
                .putStringSet("pending_deletions", currentDeletions)
                .apply()
            Log.d(TAG, "✅ Eliminación sincronizada: $wordId")
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando eliminación sincronizada: ${e.message}")
        }
    }
    
    /**
     * Borra todas las eliminaciones pendientes
     */
    fun clearPendingDeletions() {
        prefs.edit().remove("pending_deletions").apply()
        Log.d(TAG, "🗑️ Eliminaciones pendientes borradas")
    }
    
    // ==================== FLASHCARDS ====================
    
    /**
     * Sincroniza flashcards desde el servidor
     */
    suspend fun syncFlashcards(flashcards: List<FlashcardData>) {
        try {
            val entities = flashcards.map { card ->
                FlashcardEntity(
                    id = card.id,
                    wordId = card.wordId,
                    word = card.word,
                    translation = card.translation,
                    language = card.language,
                    type = card.type,  // 🔥 Guardar el tipo (word o expression)
                    songContext = card.songYoutubeUrl,
                    easeFactor = card.easinessFactor,
                    interval = card.interval,
                    repetitions = card.repetitions,
                    nextReviewDate = card.nextReviewDate,
                    lastSyncedAt = System.currentTimeMillis()
                )
            }
            
            flashcardDao.insertFlashcards(entities)
            Log.d(TAG, "✅ Flashcards sincronizadas: ${entities.size} tarjetas")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando flashcards: ${e.message}")
        }
    }
    
    /**
     * Obtiene flashcards pendientes de repaso
     */
    /**
     * Obtiene flashcards que deben revisarse hoy filtradas por idioma y tipo
     */
    fun getDueFlashcards(language: String? = null, type: String? = null): Flow<List<FlashcardEntity>> {
        val today = getTodayString()
        val allFlashcards = flashcardDao.getDueFlashcards(today)
        
        return allFlashcards.map { cards ->
            cards.filter { card ->
                (language == null || card.language == language) &&
                (type == null || card.type == type)
            }
        }
    }
    
    /**
     * Elimina una flashcard de la caché offline (cuando se borra una palabra)
     */
    suspend fun deleteFlashcard(wordId: String) {
        try {
            flashcardDao.deleteFlashcard(wordId)
            Log.d(TAG, "🗑️ Flashcard eliminada de cache offline: $wordId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando flashcard: ${e.message}")
        }
    }
    
    /**
     * Obtiene todas las flashcards
     */
    fun getAllFlashcards(): Flow<List<FlashcardEntity>> {
        return flashcardDao.getAllFlashcards()
    }
    
    /**
     * Obtiene el contador de flashcards pendientes
     */
    suspend fun getDueFlashcardCount(): Int {
        val today = getTodayString()
        return flashcardDao.getDueCount(today)
    }
    
    /**
     * Guarda una reseña de flashcard pendiente para sincronizar
     */
    fun savePendingReview(wordId: String, quality: Int) {
        try {
            val reviews = getPendingReviews().toMutableMap()
            reviews[wordId] = quality
            val json = com.google.gson.Gson().toJson(reviews)
            prefs.edit().putString("pending_reviews", json).apply()
            Log.d(TAG, "💾 Reseña pendiente guardada: $wordId = $quality")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando reseña pendiente: ${e.message}")
        }
    }
    
    /**
     * Obtiene reseñas pendientes de sincronizar
     */
    fun getPendingReviews(): Map<String, Int> {
        return try {
            val json = prefs.getString("pending_reviews", null) ?: return emptyMap()
            com.google.gson.Gson().fromJson(json, object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo reseñas pendientes: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Marca una reseña como sincronizada
     */
    fun markReviewSynced(wordId: String) {
        try {
            val reviews = getPendingReviews().toMutableMap()
            reviews.remove(wordId)
            val json = com.google.gson.Gson().toJson(reviews)
            prefs.edit().putString("pending_reviews", json).apply()
            Log.d(TAG, "✅ Reseña sincronizada: $wordId")
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando reseña sincronizada: ${e.message}")
        }
    }
    
    /**
     * Borra todas las reseñas pendientes
     */
    fun clearPendingReviews() {
        prefs.edit().remove("pending_reviews").apply()
        Log.d(TAG, "🗑️ Reseñas pendientes borradas")
    }
    
    // ==================== LETRAS ====================
    
    /**
     * Guarda letras en cache local
     */
    suspend fun cacheLyrics(
        spotifyId: String,
        songName: String,
        artistName: String,
        imageUrl: String?,
        lyrics: String,
        syncedLyrics: String?,
        language: String
    ) {
        try {
            val entity = LyricsCacheEntity(
                spotifyId = spotifyId,
                songName = songName,
                artistName = artistName,
                imageUrl = imageUrl,
                lyrics = lyrics,
                syncedLyrics = syncedLyrics,
                language = language
            )
            
            lyricsDao.insertLyrics(entity)
            Log.d(TAG, "💾 Letras cacheadas: $songName")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cacheando letras: ${e.message}")
        }
    }
    
    /**
     * Obtiene letras desde cache local
     */
    suspend fun getCachedLyrics(spotifyId: String): LyricsCacheEntity? {
        return lyricsDao.getLyrics(spotifyId)
    }
    
    /**
     * Limpia cache de letras antiguas (más de 7 días)
     */
    suspend fun cleanOldLyricsCache() {
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 días
        lyricsDao.deleteOldCache(cutoffTime)
        Log.d(TAG, "🧹 Cache de letras antiguas limpiado")
    }
    
    // ==================== ESTADÍSTICAS ====================
    
    /**
     * Obtiene estadísticas del almacenamiento offline
     */
    suspend fun getOfflineStats(): OfflineStats {
        val today = getTodayString()
        return OfflineStats(
            dictionaryWords = dictionaryDao.getWordCount(),
            flashcards = flashcardDao.getDueCount(today),
            cachedLyrics = lyricsDao.getCacheCount()
        )
    }
    
    /**
     * Limpia todos los datos offline
     */
    suspend fun clearAll() {
        dictionaryDao.deleteAll()
        flashcardDao.deleteAll()
        lyricsDao.deleteAll()
        Log.d(TAG, "🗑️ Todos los datos offline eliminados")
    }
}

data class OfflineStats(
    val dictionaryWords: Int,
    val flashcards: Int,
    val cachedLyrics: Int
)
