package com.example.diccionario_hiphop

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// ===========================================================
// 1️⃣ AUTENTICACIÓN
// ===========================================================
data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("user_id") val userId: String,
    val email: String,
    val username: String?
)

data class RegisterRequest(
    val email: String,
    val username: String,
    @SerializedName("full_name") val fullName: String,
    val password: String,
    @SerializedName("native_language") val nativeLanguage: String = "es",
    @SerializedName("learning_languages") val learningLanguages: List<LearningLanguageRequest>
)

data class LearningLanguageRequest(
    val language: String,
    val level: String,
    @SerializedName("started_at") val startedAt: String? = null
)

data class RegisterResponse(val id: String, val email: String, val username: String)
data class VerifyTokenResponse(val valid: Boolean)

// ===========================================================
// 2️⃣ USUARIO (Unificado: Perfil + Datos)
// ===========================================================

// 🔥 ESTA ES LA CLASE IMPORTANTE. ÚSALA EN RETROFIT Y PROFILEACTIVITY
data class User(
    val id: String,
    val email: String,
    val username: String,
    
    @SerializedName("native_language") 
    val nativeLanguage: String,
    
    @SerializedName("avatar_url") 
    val avatarUrl: String?,
    
    @SerializedName("learning_languages") 
    val learningLanguages: List<LearningLanguage>?,
    
    // ✨ FASE 2.5: Gamificación y multi-idioma
    @SerializedName("primary_language")
    val primaryLanguage: String = "en",
    
    @SerializedName("total_xp")
    val totalXp: Int = 0,
    
    @SerializedName("current_streak")
    val currentStreak: Int = 0,
    
    @SerializedName("longest_streak")
    val longestStreak: Int = 0,
    
    @SerializedName("burnout_limit")
    val burnoutLimit: Int = 50,
    
    // Estadísticas globales (compatibilidad)
    @SerializedName("reviews_count") 
    val reviewsCount: Int = 0,
    
    @SerializedName("words_count")
    val wordsCount: Int = 0,
    
    // ✅ Para onboarding por cuenta (no por dispositivo)
    @SerializedName("onboarding_completed")
    val onboardingCompleted: Boolean = false
)

data class LearningLanguage(
    val language: String,
    val level: String,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("last_tested") val lastTested: String? = null,
    
    // ✨ FASE 2.5: Progreso y gestión
    @SerializedName("daily_goal") val dailyGoal: Int = 10,
    @SerializedName("reviews_pending") val reviewsPending: Int = 0,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("words_learned") val wordsLearned: Int = 0
)

// Solicitudes de cambio de perfil
data class UpdateProfileRequest(
    val username: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("native_language") val nativeLanguage: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String
)

data class ChangeEmailRequest(
    @SerializedName("new_email") val newEmail: String,
    val password: String
)

// ===========================================================
// 3️⃣ MODELO DE CANCIÓN
// ===========================================================
data class SongItem(
    val id: String,
    @SerializedName("title", alternate = ["name"]) val title: String,
    val artist: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("preview_url") val previewUrl: String?,
    @SerializedName("spotify_url") val spotifyUrl: String?
)

// ===========================================================
// 🔥 NUEVO: STREAMING DE AUDIO
// ===========================================================
data class AudioStreamResponse(
    val title: String,
    @SerializedName("stream_url") val streamUrl: String, // La URL mágica de GoogleVideo
    val duration: Int,
    val thumbnail: String,
    val source: String
)
// ===========================================================
// 4️⃣ LETRAS Y ANÁLISIS IA
// ===========================================================
data class LyricsResponse(
    val title: String,
    val artist: String,
    val lyrics: String,
    
    @SerializedName("image_url")
    val imageUrl: String?,
    
    @SerializedName("preview_url")
    val previewUrl: String?,      // El de iTunes (30 seg)
    
    // 👇 ¡ESTE ES EL NUEVO!
    @SerializedName("full_audio_url")
    val fullAudioUrl: String?,     // El de Cobalt (YouTube completo)
    
    @SerializedName("synced_lyrics")
    val syncedLyrics: String?      // Formato LRC con timestamps
)

data class AnalyzeLyricsRequest(
    val title: String,
    val artist: String,
    val lyrics: String,
    @SerializedName("user_level") val userLevel: String = "B1"
)

data class HighlightWordsResponse(
    @SerializedName("detected_language") val detectedLanguage: String,
    val words: List<WordDefinition>,
    val expressions: List<ExpressionDefinition>,
    val suggestions: List<String>
)

data class WordDefinition(
    val word: String,
    val type: String,
    @SerializedName("translation") val definition: String,
    val explanation: String?,
    val example: String,
    val difficulty: String,
    val color: String,
    val recommended: Boolean,
    @SerializedName("already_saved") val alreadySaved: Boolean = false
)

data class ExpressionDefinition(
    val expression: String,
    val type: String,
    @SerializedName("translation") val meaning: String,
    @SerializedName("explanation") val translation: String?,
    val example: String,
    val difficulty: String,
    val color: String,
    val recommended: Boolean,
    @SerializedName("already_saved") val alreadySaved: Boolean = false
)

// ===========================================================
// 5️⃣ DICCIONARIO
// ===========================================================
data class UserWord(
    val id: String,
    val word: String,
    val translation: String?,
    @SerializedName("notes") val context: String?,
    val type: String = "word",
    val example: String? = null,
    val language: String = "en",  // 🌍 Código del idioma
    
    // 🔊 AUDIO (NUEVO)
    @SerializedName("song_youtube_url") val songYoutubeUrl: String? = null,
    @SerializedName("timestamp_start") val timestampStart: Float? = null,
    @SerializedName("timestamp_end") val timestampEnd: Float? = null,
    
    @SerializedName("is_recommended") val isRecommended: Boolean = false,
    val warning: String? = null  // 🔥 FASE 4: Warning si superó daily_goal
) : Serializable

data class AddWordRequest(
    val word: String,
    val translation: String?,
    val language: String,  // 🌍 OBLIGATORIO: código del idioma
    val notes: String?,
    val type: String,
    val example: String?,
    
    // 🔊 AUDIO (NUEVO)
    @SerializedName("song_youtube_url") val songYoutubeUrl: String? = null,
    @SerializedName("timestamp_start") val timestampStart: Float? = null,
    @SerializedName("timestamp_end") val timestampEnd: Float? = null,
    
    @SerializedName("is_recommended") val isRecommended: Boolean,
    @SerializedName("song_id") val songId: String?
)

// ===========================================================
// 6️⃣ PROGRESO Y OTROS
// ===========================================================
data class ProgressResponse(
    @SerializedName("total_words_learned") val totalWordsLearned: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("learning_languages") val learningLanguages: List<LearningLanguage> = emptyList()
)

data class RefreshTokenRequest(@SerializedName("refresh_token") val refreshToken: String)
data class HealthResponse(val status: String)

// ===========================================================
// 7️⃣ FLASHCARDS
// ===========================================================
data class FlashcardReviewRequest(val quality: Int)

data class FlashcardData(
    val id: String,
    @SerializedName("word_id") val wordId: String,
    val word: String,
    val translation: String,
    val example: String,
    val explanation: String?,
    val type: String,
    val language: String,  // 🌍 Código del idioma (ja, ko, zh, es, etc.)
    
    // 🔊 AUDIO (NUEVO)
    @SerializedName("song_youtube_url") val songYoutubeUrl: String? = null,
    @SerializedName("timestamp_start") val timestampStart: Float? = null,
    @SerializedName("timestamp_end") val timestampEnd: Float? = null,
    
    @SerializedName("easiness_factor") val easinessFactor: Float,
    val interval: Int,
    val repetitions: Int,
    @SerializedName("next_review_date") val nextReviewDate: String
)

data class FlashcardReviewResponse(
    val success: Boolean,
    @SerializedName("next_review_date") val nextReviewDate: String,
    @SerializedName("interval_days") val intervalDays: Int,
    val message: String
)

// ===========================================================
// 8️⃣ RECUPERACIÓN DE CUENTA
// ===========================================================
data class VerifyAccountRequest(val email: String, val code: String)
data class ForgotPasswordRequest(val email: String)

data class ResetPasswordRequest(
    val email: String, 
    val code: String,
    @SerializedName("new_password") val newPassword: String
)

data class MessageResponse(val message: String)
data class AvatarUpdateResponse(
    @SerializedName("avatar_url") val avatarUrl: String,
    val message: String
)

// ===========================================================
// 9️⃣ GESTIÓN DE IDIOMAS (Fase 2.5)
// ===========================================================
data class AddLanguageRequest(
    val language: String,
    val level: String
)

data class ReorderLanguagesRequest(
    @SerializedName("language_order") val languageOrder: List<String>
)

data class UpdateDailyGoalRequest(
    @SerializedName("daily_goal") val dailyGoal: Int
)

data class UpdateLevelRequest(
    @SerializedName("level") val level: String
)

// ===========================================================
// 🔟 BORRADO DE IDIOMAS & GRAMMY (BLOQUE 2)
// ===========================================================
data class DeleteLanguageResponse(
    val message: String,
    @SerializedName("deleted_words") val deletedWords: Int,
    @SerializedName("deleted_flashcards") val deletedFlashcards: Int,
    @SerializedName("remaining_languages") val remainingLanguages: Int
)

data class GrammyCheckResponse(
    @SerializedName("is_grammy") val isGrammy: Boolean,
    val badge: String?
)