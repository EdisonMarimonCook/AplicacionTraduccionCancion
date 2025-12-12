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

    @POST("/api/v1/auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<LoginResponse>

    // =================================================================================
    // 2️⃣ GESTIÓN DE USUARIOS (/api/v1/users)
    // =================================================================================

    // Obtener datos del perfil actual
    @GET("/api/v1/users/profile")
    suspend fun getProfile(): Response<UserProfile>

    // Actualizar datos básicos (Nombre, Idioma...)
    // Usa el modelo UpdateProfileRequest definido en Models.kt
    @PUT("/api/v1/users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserProfile>

    // Cambiar Contraseña
    // Usa el modelo ChangePasswordRequest
    @POST("/api/v1/users/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Void>

    // Cambiar Email
    // Usa el modelo ChangeEmailRequest
    @POST("/api/v1/users/change-email")
    suspend fun changeEmail(@Body request: ChangeEmailRequest): Response<Void>

    // Obtener Progreso (Estadísticas)
    @GET("/api/v1/user/progress")
    suspend fun getUserProgress(): Response<ProgressResponse>

    // =================================================================================
    // 3️⃣ CANCIONES Y BÚSQUEDA
    // =================================================================================

    @GET("/api/v1/songs/top-grammy")
    suspend fun getTopGrammy(@Query("lang") lang: String = "en"): Response<List<SongItem>>

    @GET("/api/v1/songs/search")
    suspend fun searchSongs(@Query("query") query: String): Response<List<SongItem>>

    // =================================================================================
    // 4️⃣ LETRAS E IA
    // =================================================================================

    @GET("/api/v1/lyrics/")
    suspend fun getLyrics(
        @Query("title") title: String,
        @Query("artist") artist: String
    ): Response<LyricsResponse>

    @POST("/api/v1/ai/analyze")
    suspend fun analyzeLyrics(@Body request: AnalyzeLyricsRequest): Response<HighlightWordsResponse>

    // =================================================================================
    // 5️⃣ DICCIONARIO
    // =================================================================================

    @GET("/api/v1/dictionary/list")
    suspend fun getDictionary(@Query("type") type: String? = null): Response<List<UserWord>>

    @POST("/api/v1/dictionary/add")
    suspend fun addWord(@Body request: AddWordRequest): Response<UserWord>

    @DELETE("/api/v1/dictionary/delete/{id}")
    suspend fun deleteWord(@Path("id") id: String): Response<Void>

    // =================================================================================
    // FLASHCARDS
    // =================================================================================

    @GET("/api/v1/flashcards/due")
    suspend fun getDueFlashcards(): Response<List<FlashcardData>>

    @POST("/api/v1/flashcards/review/{word_id}")
    suspend fun reviewFlashcard(
        @Path("word_id") wordId: String,
        @Body request: FlashcardReviewRequest
    ): Response<FlashcardReviewResponse>

    @GET("/api/v1/flashcards/stats")
    suspend fun getFlashcardStats(): Response<Map<String, Int>>

    // =================================================================================
    // EXTRAS
    // =================================================================================

    @GET("/api/v1/health")
    suspend fun healthCheck(): Response<HealthResponse>

    // =================================================================================
    // 6️⃣ VERIFICACIÓN Y RECUPERACIÓN DE CONTRASEÑA
    // =================================================================================

    @POST("/api/v1/auth/verify")
    suspend fun verifyAccount(@Body request: VerifyAccountRequest): Response<MessageResponse>

    @POST("/api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("/api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>
}