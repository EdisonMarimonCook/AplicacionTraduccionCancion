package com.example.diccionario_hiphop

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // =================================================================================
    // 1️⃣ AUTENTICACIÓN
    // =================================================================================

    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // 🔥 Endpoint SÍNCRONO para el AuthInterceptor (devuelve Call, no Response)
    @POST("/api/v1/auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<LoginResponse>

    @POST("/api/v1/auth/verify-token")
    suspend fun verifyToken(): Response<VerifyTokenResponse>


    // =================================================================================
    // 2️⃣ CANCIONES Y BÚSQUEDA
    // =================================================================================

    @GET("/api/v1/songs/")
    suspend fun getTopSongs(@Query("language") language: String = "es"): Response<TopSongsResponse>

    @GET("/api/v1/lyrics/search")
    suspend fun searchLyrics(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): Response<SearchResponse>


    // =================================================================================
    // 3️⃣ APRENDIZAJE (LETRAS Y ANÁLISIS)
    // =================================================================================

    // Obtener letra simple
    @GET("/api/v1/lyrics/")
    suspend fun getLyrics(
        @Query("title") title: String,
        @Query("artist") artist: String
    ): Response<LyricsResponse>

    // Obtener letra analizada con colores (IA)
    @GET("/api/v1/lyrics/analyze")
    suspend fun analyzeLyrics(
        @Query("title") title: String,
        @Query("artist") artist: String,
        @Query("user_level") userLevel: String
    ): Response<AnalysisResponse>

    // Obtener letra con tiempos para Karaoke (Fragmentador)
    @GET("/api/v1/lyrics/with-fragments")
    suspend fun getLyricsWithFragments(
        @Query("title") title: String,
        @Query("artist") artist: String
    ): Response<FragmentsResponse>


    // =================================================================================
    // 4️⃣ DICCIONARIO PERSONAL
    // =================================================================================

    @POST("/api/v1/user/words")
    suspend fun addWord(@Body request: AddWordRequest): Response<UserWord>

    @GET("/api/v1/user/words")
    suspend fun getDictionary(): Response<DictionaryResponse>

    @DELETE("/api/v1/user/words/{word_id}")
    suspend fun deleteWord(@Path("word_id") wordId: String): Response<Map<String, String>>


    // =================================================================================
    // 5️⃣ FLASHCARDS (SISTEMA DE REPASO)
    // =================================================================================

    @POST("/api/v1/user/flashcards")
    suspend fun createFlashcard(@Body request: CreateFlashcardRequest): Response<FlashcardItem>

    @GET("/api/v1/user/flashcards/review")
    suspend fun getFlashcardsToReview(): Response<FlashcardReviewResponse>

    @POST("/api/v1/user/flashcards/{card_id}/review")
    suspend fun submitReview(
        @Path("card_id") cardId: String,
        @Body request: ReviewRequest
    ): Response<FlashcardItem>


    // =================================================================================
    // 6️⃣ SESIONES DE ESTUDIO
    // =================================================================================

    @POST("/api/v1/user/sessions")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<SessionResponse>

    @GET("/api/v1/user/sessions")
    suspend fun getSessions(): Response<SessionListResponse>


    // =================================================================================
    // 7️⃣ PERFIL Y UTILIDADES
    // =================================================================================

    @GET("/api/v1/user/progress")
    suspend fun getUserProgress(): Response<ProgressResponse>

    @GET("/api/v1/health")
    suspend fun healthCheck(): Response<HealthResponse>
}