package com.example.diccionario_hiphop

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST

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

    // 🔥 CAMBIO 1: Usar 'User' en lugar de 'UserProfile' para que funcionen los contadores
    @GET("/api/v1/users/profile")
    suspend fun getProfile(): Response<User>

    // 🔥 CAMBIO 2: Usar 'User' aquí también
    @PUT("/api/v1/users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @POST("/api/v1/users/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Void>

    @POST("/api/v1/users/change-email")
    suspend fun changeEmail(@Body request: ChangeEmailRequest): Response<Void>

    @GET("/api/v1/user/progress")
    suspend fun getUserProgress(): Response<ProgressResponse>
    
    // 🔥 CAMBIO 3: Usar 'AvatarUpdateResponse' porque el backend NO devuelve un User completo aquí
    @Multipart
    @POST("/api/v1/users/upload-avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<AvatarUpdateResponse>

    // =================================================================================
    // 3️⃣ CANCIONES Y BÚSQUEDA
    // =================================================================================

    @GET("/api/v1/songs/top-grammy")
    suspend fun getTopGrammy(@Query("lang") lang: String = "en"): Response<List<SongItem>>

    @GET("/api/v1/songs/search")
    suspend fun searchSongs(@Query("query") query: String): Response<List<SongItem>>
    
    // 🔥 NUEVO: Endpoint para obtener el audio real
    @GET("/api/v1/audio/stream")
    suspend fun getAudioStream(
        @Query("q") query: String
    ): Response<AudioStreamResponse>

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