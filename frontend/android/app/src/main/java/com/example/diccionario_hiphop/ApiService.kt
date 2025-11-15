package com.example.diccionario_hiphop

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // 1️⃣ AUTENTICACIÓN
    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/v1/auth/verify-token")
    suspend fun verifyToken(@Header("Authorization") token: String): Response<VerifyTokenResponse>

    // 2️⃣ BÚSQUEDA
    @GET("/api/v1/lyrics/search")
    suspend fun searchLyrics(
        @Header("Authorization") token: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): Response<SearchResponse>

    // 3️⃣ LETRAS
    @GET("/api/v1/lyrics/")
    suspend fun getLyrics(
        @Header("Authorization") token: String,
        @Query("title") title: String,
        @Query("artist") artist: String
    ): Response<LyricsResponse>

    // 4️⃣ TOP CANCIONES
    @GET("/api/v1/songs/")
    suspend fun getTopSongs(
        @Header("Authorization") token: String,
        @Query("language") language: String = "es"
    ): Response<TopSongsResponse>

    // 5️⃣ ANÁLISIS IA (TU FUNCIONALIDAD PRINCIPAL)
    @GET("/api/v1/lyrics/analyze")
    suspend fun analyzeLyrics(
        @Header("Authorization") token: String,
        @Query("title") title: String,
        @Query("artist") artist: String,
        @Query("user_level") userLevel: String
    ): Response<AnalysisResponse>

    // HEALTH CHECK
    @GET("/api/v1/health")
    suspend fun healthCheck(): Response<HealthResponse>
}
// ❌ ELIMINA TODO ESTO DE AQUÍ HACIA ABAJO