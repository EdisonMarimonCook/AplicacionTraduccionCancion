package com.example.diccionario_hiphop

import android.content.Context
import retrofit2.Response

// ✅ CORRECCIÓN: Ahora la clase recibe 'context' en el constructor
class SongRepository(context: Context) {

    // ✅ Inicializamos el servicio pasando el contexto
    private val apiService = RetrofitService.getInstance(context)

    // --- Búsqueda ---
    suspend fun searchSongs(query: String): Response<SearchResponse> {
        return apiService.searchLyrics(query)
    }

    // --- Top Canciones ---
    suspend fun getTopSongs(language: String = "es"): Response<TopSongsResponse> {
        return apiService.getTopSongs(language)
    }

    // --- Análisis de Letra (El núcleo de tu app) ---
    suspend fun getAnalyzedLyrics(title: String, artist: String, userLevel: String): Response<AnalysisResponse> {
        return apiService.analyzeLyrics(title, artist, userLevel)
    }

    // --- Letra Plana ---
    suspend fun getLyrics(title: String, artist: String): Response<LyricsResponse> {
        return apiService.getLyrics(title, artist)
    }
}