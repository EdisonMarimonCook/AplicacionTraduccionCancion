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
    
    val streak: Int,
    
    @SerializedName("reviews_count") 
    val reviewsCount: Int,
    
    @SerializedName("words_count")  // 🔥 AÑADIR ESTE CAMPO
    val wordsCount: Int = 0,
    
    @SerializedName("learning_languages") 
    val learningLanguages: List<LearningLanguage>?
)

data class LearningLanguage(
    val language: String,
    val level: String,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("last_tested") val lastTested: String? = null,
    @SerializedName("wordsLearned") val wordsLearned: Int = 0  // 🔥 AÑADIR ESTE CAMPO
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
// 4️⃣ LETRAS Y ANÁLISIS IA
// ===========================================================
data class LyricsResponse(
    val title: String,
    val artist: String,
    val lyrics: String,
    @SerializedName("preview_url") val previewUrl: String?
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
    @SerializedName("is_recommended") val isRecommended: Boolean = false
) : Serializable

data class AddWordRequest(
    val word: String,
    val translation: String?,
    val notes: String?,
    val type: String,
    val example: String?,
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