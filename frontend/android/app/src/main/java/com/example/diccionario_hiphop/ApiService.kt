package com.example.diccionario_hiphop

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // =================================================================================
    // 1️⃣ AUTENTICACIÓN (Correcto)
    // =================================================================================

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // Endpoint síncrono para el AuthInterceptor
    @POST("/api/v1/auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<LoginResponse>

    // =================================================================================
    // 2️⃣ CANCIONES Y BÚSQUEDA (Backend: songs.py)
    // =================================================================================

    // Top Grammys / Hits (La lista real de Spotify)
    @GET("/api/v1/songs/top-grammy")
    suspend fun getTopGrammy(@Query("lang") lang: String = "en"): Response<List<SongItem>>

    // Búsqueda Unificada Real
    @GET("/api/v1/songs/search")
    suspend fun searchSongs(@Query("query") query: String): Response<List<SongItem>>

    // =================================================================================
    // 3️⃣ LETRAS E IA (Backend: lyrics.py, ai_analysis.py)
    // =================================================================================

    // Obtener letra y audio preview
    @GET("/api/v1/lyrics/")
    suspend fun getLyrics(
        @Query("title") title: String, 
        @Query("artist") artist: String
    ): Response<LyricsResponse>

    // ✅ CORRECCIÓN CRÍTICA: Nombre del endpoint y tipo de respuesta
    @POST("/api/v1/ai/analyze")
    suspend fun analyzeLyrics(@Body request: AnalyzeLyricsRequest): Response<HighlightWordsResponse>

    // =================================================================================
    // 4️⃣ DICCIONARIO (Backend: dictionary.py)
    // =================================================================================

    // ✅ CORRECCIÓN CRÍTICA: Rutas actualizadas a v4.0 (/dictionary/...)
    @GET("/api/v1/dictionary/list")
    suspend fun getDictionary(@Query("type") type: String? = null): Response<List<UserWord>>

    @POST("/api/v1/dictionary/add")
    suspend fun addWord(@Body request: AddWordRequest): Response<UserWord>

    @DELETE("/api/v1/dictionary/delete/{id}")
    suspend fun deleteWord(@Path("id") id: String): Response<Void>

    // =================================================================================
    // 5️⃣ PERFIL (Backend: progress.py)
    // =================================================================================

    @GET("/api/v1/user/progress")
    suspend fun getUserProgress(): Response<ProgressResponse>

    // ❌ NOTA: Hemos eliminado los endpoints de Flashcards y Sessions porque 
    // no existen en el Backend MVP v4.0. Se añadirán en la Fase 2.

    //(Para ConnectionTester):
    @GET("/api/v1/health")
    suspend fun healthCheck(): Response<HealthResponse>
}


