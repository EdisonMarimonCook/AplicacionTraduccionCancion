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
    @SerializedName("user_id") val userId: String,   // Viene directo
    val email: String,                               // Viene directo
    val username: String?                            // Viene directo (nullable por seguridad)
)


// ✅ REGISTRO POTENTE (Soporta lista de idiomas)
data class RegisterRequest(
    val email: String,
    val username: String,
    @SerializedName("full_name") val fullName: String,
    val password: String,
    @SerializedName("native_language") val nativeLanguage: String = "es",
    
    // 🔥 Enviamos una LISTA, aunque por ahora solo lleve uno
    @SerializedName("learning_languages") val learningLanguages: List<LearningLanguageRequest>
)

data class LearningLanguageRequest(
    val language: String,
    val level: String,
    // Enviamos null en la fecha para que el backend ponga la actual, 
    // o enviamos un string si quieres controlarlo tú. 
    // Para simplificar y evitar errores de formato, lo hacemos nullable.
    @SerializedName("started_at") val startedAt: String? = null
)

data class RegisterResponse(
    val id: String,
    val email: String,
    val username: String
)

data class VerifyTokenResponse(val valid: Boolean)

// ===========================================================
// 2️⃣ MODELO DE CANCIÓN (Sincronizado con spotify.py)
// ===========================================================
data class SongItem(
    val id: String,
    @SerializedName("title", alternate = ["name"]) val title: String,
    val artist: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("preview_url") val previewUrl: String?, // Puede venir de iTunes o Spotify
    @SerializedName("spotify_url") val spotifyUrl: String?
)

// ===========================================================
// 3️⃣ LETRAS Y ANÁLISIS IA (Sincronizado con Gemini V3)
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

// ✅ CORRECCIÓN CRÍTICA: Formato de respuesta REAL
data class HighlightWordsResponse(
    @SerializedName("detected_language") val detectedLanguage: String,
    val words: List<WordHighlight>,
    val expressions: List<ExpressionHighlight>,
    val suggestions: List<String>
)

data class WordHighlight(
    val word: String,
    val type: String,        // 'noun', 'verb', 'adj'
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
// 4️⃣ DICCIONARIO (Sincronizado con routers/dictionary.py)
// ===========================================================
data class UserWord(
    val id: String,
    val word: String,
    val translation: String?,
    
    @SerializedName("notes") 
    val context: String?, 
    
    val type: String = "word",       // 🔥 'word' o 'expression'
    val example: String? = null,     // 🔥 Para Flashcards
    
    @SerializedName("is_recommended") 
    val isRecommended: Boolean = false // ⭐ Estrellita
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
// 5️⃣ PROGRESO
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