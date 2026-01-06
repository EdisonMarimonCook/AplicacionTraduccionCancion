package com.example.diccionario_hiphop.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * Cache manager para URLs de fragmentos de audio extraídas con yt-dlp
 * Guarda las URLs extraídas para evitar extracciones repetidas
 */
class AudioFragmentCache(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("audio_fragment_cache", Context.MODE_PRIVATE)
    private val cacheDir = File(context.cacheDir, "audio_fragments")
    
    companion object {
        private const val TAG = "AudioFragmentCache"
        private const val CACHE_EXPIRY_MS = 3600000L // 1 hora (las URLs de YouTube expiran)
        private const val MAX_CACHE_ENTRIES = 100 // Máximo de entradas en cache
    }
    
    init {
        // Crear directorio de cache si no existe
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        // Limpiar cache expirado al iniciar
        cleanExpiredCache()
    }
    
    /**
     * Obtiene la URL de audio cacheada si existe y no ha expirado
     * @param youtubeUrl URL de búsqueda de YouTube (ytsearch:Artist Song)
     * @param startTime Timestamp de inicio en segundos
     * @param endTime Timestamp de fin en segundos
     * @return URL del fragmento de audio o null si no está cacheada/expirada
     */
    fun getCachedUrl(youtubeUrl: String, startTime: Float, endTime: Float): String? {
        val cacheKey = generateCacheKey(youtubeUrl, startTime, endTime)
        val cachedData = prefs.getString(cacheKey, null) ?: return null
        
        val parts = cachedData.split("|")
        if (parts.size != 2) return null
        
        val timestamp = parts[0].toLongOrNull() ?: return null
        val url = parts[1]
        
        // Verificar si la entrada ha expirado
        if (System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS) {
            Log.d(TAG, "⏰ Cache expirado para: $cacheKey")
            prefs.edit().remove(cacheKey).apply()
            return null
        }
        
        Log.d(TAG, "✅ URL recuperada de cache: $cacheKey")
        return url
    }
    
    /**
     * Guarda una URL de audio en el cache
     * @param youtubeUrl URL de búsqueda de YouTube
     * @param startTime Timestamp de inicio
     * @param endTime Timestamp de fin
     * @param audioUrl URL del fragmento extraído
     */
    fun cacheUrl(youtubeUrl: String, startTime: Float, endTime: Float, audioUrl: String) {
        val cacheKey = generateCacheKey(youtubeUrl, startTime, endTime)
        val cacheValue = "${System.currentTimeMillis()}|$audioUrl"
        
        // Verificar límite de entradas
        if (prefs.all.size >= MAX_CACHE_ENTRIES) {
            cleanOldestEntries(10) // Eliminar las 10 más antiguas
        }
        
        prefs.edit().putString(cacheKey, cacheValue).apply()
        Log.d(TAG, "💾 URL guardada en cache: $cacheKey")
    }
    
    /**
     * Limpia todas las entradas del cache
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "🗑️ Cache completo limpiado")
    }
    
    /**
     * Limpia entradas expiradas del cache
     */
    private fun cleanExpiredCache() {
        val editor = prefs.edit()
        var cleanedCount = 0
        
        prefs.all.forEach { (key, value) ->
            val stringValue = value as? String ?: return@forEach
            val timestamp = stringValue.split("|").getOrNull(0)?.toLongOrNull() ?: return@forEach
            
            if (System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS) {
                editor.remove(key)
                cleanedCount++
            }
        }
        
        if (cleanedCount > 0) {
            editor.apply()
            Log.d(TAG, "🧹 Limpiadas $cleanedCount entradas expiradas")
        }
    }
    
    /**
     * Elimina las N entradas más antiguas
     */
    private fun cleanOldestEntries(count: Int) {
        val entries = prefs.all.mapNotNull { (key, value) ->
            val stringValue = value as? String ?: return@mapNotNull null
            val timestamp = stringValue.split("|").getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            key to timestamp
        }.sortedBy { it.second }
        
        val toRemove = entries.take(count)
        val editor = prefs.edit()
        toRemove.forEach { (key, _) -> editor.remove(key) }
        editor.apply()
        
        Log.d(TAG, "🧹 Eliminadas ${toRemove.size} entradas antiguas")
    }
    
    /**
     * Genera una clave única para el cache basada en la canción y timestamps
     */
    private fun generateCacheKey(youtubeUrl: String, startTime: Float, endTime: Float): String {
        return "${youtubeUrl.hashCode()}_${startTime}_${endTime}"
    }
    
    /**
     * Obtiene estadísticas del cache
     */
    fun getCacheStats(): CacheStats {
        val all = prefs.all
        val now = System.currentTimeMillis()
        var validCount = 0
        var expiredCount = 0
        
        all.forEach { (_, value) ->
            val stringValue = value as? String ?: return@forEach
            val timestamp = stringValue.split("|").getOrNull(0)?.toLongOrNull() ?: return@forEach
            
            if (now - timestamp > CACHE_EXPIRY_MS) {
                expiredCount++
            } else {
                validCount++
            }
        }
        
        return CacheStats(
            totalEntries = all.size,
            validEntries = validCount,
            expiredEntries = expiredCount
        )
    }
}

data class CacheStats(
    val totalEntries: Int,
    val validEntries: Int,
    val expiredEntries: Int
)
