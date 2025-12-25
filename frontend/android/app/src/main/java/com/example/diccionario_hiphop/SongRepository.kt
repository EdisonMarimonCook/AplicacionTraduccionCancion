package com.example.diccionario_hiphop

import android.content.Context
import retrofit2.Response

class SongRepository(context: Context) {

    private val apiService = RetrofitService.getInstance(context)

    suspend fun searchSongs(query: String): Response<List<SongItem>> {
        return apiService.searchSongs(query)
    }

    // 🔥 NUEVO: Función para obtener solo la URL del audio
    suspend fun getStreamUrl(query: String): String? {
        return try {
            val response = apiService.getAudioStream(query)
            if (response.isSuccessful && response.body() != null) {
                // Devolvemos la URL directa para que el ViewModel la use
                response.body()!!.streamUrl 
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getTopGrammy(language: String = "en"): Response<List<SongItem>> {
        return apiService.getTopGrammy(language)
    }

    // 🔥 CORRECCIÓN CLAVE: Añadimos el parámetro 'lyrics'
    suspend fun analyzeLyrics(title: String, artist: String, userLevel: String, lyrics: String): Response<HighlightWordsResponse> {
        val request = AnalyzeLyricsRequest(
            title = title,
            artist = artist,
            lyrics = lyrics, // ✅ Ahora enviamos la letra real, no vacía
            userLevel = userLevel
        )
        return apiService.analyzeLyrics(request)
    }

    suspend fun getLyrics(title: String, artist: String): Response<LyricsResponse> {
        return apiService.getLyrics(title, artist)
    }

    suspend fun addWord(request: AddWordRequest): Response<UserWord> {
        return apiService.addWord(request)
    }
}