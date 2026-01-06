package com.example.diccionario_hiphop

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import java.io.IOException

class DictionaryRepository(private val context: Context) {
    private val apiService = RetrofitService.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("dictionary_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getDictionary(
        language: String? = null,
        type: String? = null
    ): Response<List<UserWord>> {
        return try {
            val response = apiService.getDictionary(language, type)
            if (response.isSuccessful) {
                // Guardar en caché específica
                response.body()?.let { words ->
                    saveDictionaryCache(words, language, type)
                }
            }
            response
        } catch (e: IOException) {
            // Error de red - devolver caché si existe
            Log.d("DictionaryRepository", "📴 Sin conexión - buscando caché")
            val cached = getDictionaryCache(language, type)
            if (cached.isEmpty()) {
                Log.d("DictionaryRepository", "⚠️ No hay caché disponible")
            }
            Response.success(cached)
        }
    }
    
    suspend fun deleteWord(wordId: String): Response<Void> {
        return try {
            val response = apiService.deleteWord(wordId)
            if (!response.isSuccessful) {
                // Si falla, guardar para sincronizar después
                addPendingDeletion(wordId)
            }
            response
        } catch (e: IOException) {
            // Offline - guardar borrado pendiente
            Log.d("DictionaryRepository", "📴 Borrado offline - sincronizará después")
            addPendingDeletion(wordId)
            // Devolver éxito ficticio para actualizar UI
            Response.success(null)
        }
    }
    
    suspend fun syncPendingDeletions(): Int {
        val pending = getPendingDeletions()
        var synced = 0
        
        pending.forEach { wordId ->
            try {
                val response = apiService.deleteWord(wordId)
                if (response.isSuccessful) {
                    removePendingDeletion(wordId)
                    synced++
                }
            } catch (e: Exception) {
                Log.e("DictionaryRepository", "Error sincronizando borrado: ${e.message}")
            }
        }
        
        return synced
    }
    
    private fun saveDictionaryCache(words: List<UserWord>, language: String?, type: String?) {
        val key = "dict_${language}_${type}"
        prefs.edit()
            .putString(key, gson.toJson(words))
            .apply()
        Log.d("DictionaryRepository", "💾 Caché guardada: $key (${words.size} palabras)")
    }
    
    private fun getDictionaryCache(language: String?, type: String?): List<UserWord> {
        val key = "dict_${language}_${type}"
        val json = prefs.getString(key, null) ?: return emptyList()
        
        return try {
            gson.fromJson(json, object : TypeToken<List<UserWord>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun addPendingDeletion(wordId: String) {
        val pending = getPendingDeletions().toMutableSet()
        pending.add(wordId)
        prefs.edit().putStringSet("pending_deletions", pending).apply()
    }
    
    private fun removePendingDeletion(wordId: String) {
        val pending = getPendingDeletions().toMutableSet()
        pending.remove(wordId)
        prefs.edit().putStringSet("pending_deletions", pending).apply()
    }
    
    private fun getPendingDeletions(): Set<String> {
        return prefs.getStringSet("pending_deletions", emptySet()) ?: emptySet()
    }
}