package com.example.diccionario_hiphop

<<<<<<< HEAD
// ✅ MODELOS COMPLETOS PARA LA APP

// 1️⃣ AUTENTICACIÓN
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val language: String
)

data class RegisterResponse(
    val user_id: String,
    val email: String,
    val username: String,
    val language: String,
    val created_at: String,
    val status: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val user: UserInfo
)

data class UserInfo(
    val user_id: String,
=======
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
>>>>>>> feature/lyrics-translation
    val email: String,
    val username: String
)

<<<<<<< HEAD
data class VerifyTokenResponse(
    val valid: Boolean,
    val user_id: String,
    val email: String
)

// 2️⃣ BÚSQUEDA
data class SearchResponse(
    val query: String,
    val total_results: Int,
    val results: List<SearchResult>,
    val status: String
)

data class SearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val language: String,
    val url: String,
    val image_url: String,
    val genius_id: String
)

// 3️⃣ LETRAS
data class LyricsResponse(
    val title: String,
    val artist: String,
    val genius_id: String,
    val language: String,
    val url: String,
    val image_url: String,
    val lyrics: String,
    val line_count: Int,
    val status: String
)

// 4️⃣ TOP CANCIONES
data class TopSongsResponse(
    val language: String,
    val total: Int,
    val songs: List<TopSong>,
    val cached_at: String,
    val status: String
)

data class TopSong(
    val id: String,
    val position: Int,
    val name: String,
    val artist: String,
    val preview_url: String,
    val image_url: String,
    val language: String
)

// 5️⃣ ANÁLISIS IA (TU FUNCIONALIDAD PRINCIPAL)
data class AnalysisResponse(
    val title: String,
    val artist: String,
    val user_level: String,
    val estimated_song_level: String,
    val language: String,
    val image_url: String,
    val analyzed_lyrics: List<AnalyzedLine>,
    val word_stats: WordStats,
    val status: String
)

data class AnalyzedLine(
    val id: Int,
    val original: String,
    val highlighted_words: List<HighlightedWord>,
    val line_number: Int
)

data class HighlightedWord(
    val word: String,
    val level: String,
    val translation: String,
    val definition: String,
    val color: String
)

data class WordStats(
    val total_words: Int,
    val a1_words: Int,
    val a2_words: Int,
    val b1_words: Int,
    val b2_words: Int,
    val c1_words: Int,
    val c2_words: Int
)

// HEALTH CHECK
data class HealthResponse(
    val status: String,
    val message: String
)
=======
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
>>>>>>> feature/lyrics-translation
