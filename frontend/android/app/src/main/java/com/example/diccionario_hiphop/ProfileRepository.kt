package com.example.diccionario_hiphop

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

class ProfileRepository(private val context: Context) {
    private val apiService = RetrofitService.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getProfile(): Response<User> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful) {
                // Guardar en caché
                response.body()?.let { user ->
                    saveProfileCache(user)
                }
            }
            response
        } catch (e: IOException) {
            // Error de red - devolver caché
            Log.d("ProfileRepository", "📴 Modo offline - usando caché de perfil")
            val cached = getProfileCache()
            if (cached != null) {
                Response.success(cached)
            } else {
                throw e // Si no hay caché, propagar el error
            }
        }
    }
    
    /**
     * Invalida la caché del perfil para forzar recarga
     * Usar cuando: se añade/borra palabra, se cambian idiomas, se actualiza nivel, etc.
     */
    fun invalidateCache() {
        prefs.edit().remove("cached_profile").apply()
        Log.d("ProfileRepository", "🗑️ Caché de perfil invalidada")
    }
    
    /**
     * Actualiza un contador específico en la caché sin recargar todo
     */
    fun updateCachedCounter(counterType: String, newValue: Int) {
        val cached = getProfileCache() ?: return
        
        val updated = when (counterType) {
            "words" -> cached.copy(wordsCount = newValue)
            "reviews" -> cached.copy(reviewsCount = newValue)
            "streak" -> cached.copy(currentStreak = newValue)
            else -> cached
        }
        
        saveProfileCache(updated)
        Log.d("ProfileRepository", "✅ Contador $counterType actualizado en caché: $newValue")
    }
    
    /**
     * Incrementa un contador en la caché
     */
    fun incrementCachedCounter(counterType: String, amount: Int = 1) {
        val cached = getProfileCache() ?: return
        
        when (counterType) {
            "words" -> updateCachedCounter("words", cached.wordsCount + amount)
            "reviews" -> updateCachedCounter("reviews", cached.reviewsCount + amount)
        }
    }
    
    /**
     * Decrementa un contador en la caché
     */
    fun decrementCachedCounter(counterType: String, amount: Int = 1) {
        val cached = getProfileCache() ?: return
        
        when (counterType) {
            "words" -> updateCachedCounter("words", (cached.wordsCount - amount).coerceAtLeast(0))
        }
    }
    
    private fun saveProfileCache(user: User) {
        prefs.edit().putString("cached_profile", gson.toJson(user)).apply()
        prefs.edit().putLong("cache_timestamp", System.currentTimeMillis()).apply()
        Log.d("ProfileRepository", "💾 Perfil cacheado")
    }
    
    private fun getProfileCache(): User? {
        val json = prefs.getString("cached_profile", null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error deserializando caché de perfil: ${e.message}")
            null
        }
    }
    
    /**
     * Verifica si la caché es reciente (menos de 5 minutos)
     */
    fun isCacheRecent(): Boolean {
        val timestamp = prefs.getLong("cache_timestamp", 0)
        val ageMillis = System.currentTimeMillis() - timestamp
        val fiveMinutes = 5 * 60 * 1000
        return ageMillis < fiveMinutes
    }
}
