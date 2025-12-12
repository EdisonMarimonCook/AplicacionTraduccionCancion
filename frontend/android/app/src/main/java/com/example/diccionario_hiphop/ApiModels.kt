package com.example.diccionario_hiphop

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// ===========================================================
// 1️⃣ AUTENTICACIÓN (Sincronizado con Backend v4.0)
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

data class RegisterResponse(
    val id: String,
    val email: String,
    val username: String
)

data class VerifyTokenResponse(val valid: Boolean)

// ===========================================================
// 2️⃣ PERFIL DE USUARIO
// ===========================================================

// Ver datos del perfil
data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("native_language") val nativeLanguage: String?
)

// Para cambiar Nombre / Datos básicos
data class UpdateProfileRequest(
    val username: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("native_language") val nativeLanguage: String? = null
)

// Para cambiar Contraseña
data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String
)

// Para cambiar Email
data class ChangeEmailRequest(
    @SerializedName("new_email") val newEmail: String,
    val password: String // Contraseña actual para confirmar
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
    val words: List<WordHighlight>,
    val expressions: List<ExpressionHighlight>,
    val suggestions: List<String>
)

data class WordHighlight(
    val word: String,
    val type: String,
    val translation: String,
    val explanation: String,
    val example: String,
    val difficulty: String,
    val color: String,
    val recommended: Boolean
)

data class ExpressionHighlight(
    val expression: String,
    val type: String,
    val translation: String,
    val explanation: String,
    val example: String,
    val difficulty: String,
    val color: String,
    val recommended: Boolean
)

// ===========================================================
// 5️⃣ DICCIONARIO
// ===========================================================
data class UserWord(
    val id: String,
    val word: String,
    val translation: String?,

    @SerializedName("notes")
    val context: String?,

    val type: String = "word",
    val example: String? = null,

    @SerializedName("is_recommended")
    val isRecommended: Boolean = false
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
    val status: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class HealthResponse(val status: String)

// ===========================================================
// 7️⃣ TARJETAS DE REPASO (NEW)
// ===========================================================
data class FlashcardReviewRequest(
    val quality: Int // 0-5
)

data class FlashcardData(
    val id: String,
    @SerializedName("word_id") val wordId: String,
    val word: String,
    val translation: String,
    val example: String,
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