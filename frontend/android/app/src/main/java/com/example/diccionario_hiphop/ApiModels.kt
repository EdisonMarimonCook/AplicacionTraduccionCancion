package com.example.diccionario_hiphop

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// ===========================================================
// 1️⃣ AUTENTICACIÓN
// ===========================================================
data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val user: UserDTO
)

data class UserDTO(
    @SerializedName("user_id") val userId: String,
    val email: String,
    val username: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val language: String = "es"
)

data class RegisterResponse(
    @SerializedName("user_id") val userId: String,
    val email: String,
    val username: String,
    val status: String
)

data class VerifyTokenResponse(
    val valid: Boolean,
    @SerializedName("user_id") val userId: String,
    val email: String
)

// ===========================================================
// 2️⃣ MODELO DE CANCIÓN (Unificado para Search y Top)
// ===========================================================
data class SongItem(
    val id: String,

    // Mapea tanto "title" (búsqueda) como "name" (top songs) a esta variable
    @SerializedName("title", alternate = ["name"])
    val title: String,

    val artist: String,

    @SerializedName("image_url")
    val imageUrl: String?,

    val language: String? = "en",

    // Campos específicos
    @SerializedName("genius_id") val geniusId: String? = null,
    val url: String? = null,
    @SerializedName("preview_url") val previewUrl: String? = null,
    val position: Int? = null
) : Serializable

// ===========================================================
// 3️⃣ BÚSQUEDA Y LISTAS
// ===========================================================
data class SearchResponse(
    val query: String,
    @SerializedName("total_results") val totalResults: Int,
    val results: List<SongItem>,
    val status: String
)

data class TopSongsResponse(
    val language: String,
    val total: Int,
    val songs: List<SongItem>,
    val status: String
)

// ===========================================================
// 4️⃣ LETRAS, ANÁLISIS Y FRAGMENTOS
// ===========================================================

// GET /api/v1/lyrics/ (Texto plano)
data class LyricsResponse(
    val title: String,
    val artist: String,
    val lyrics: String,
    @SerializedName("line_count") val lineCount: Int,
    @SerializedName("image_url") val imageUrl: String?,
    val status: String
)

// GET /api/v1/lyrics/analyze (Interactivo)
data class AnalysisResponse(
    val title: String,
    val artist: String,
    @SerializedName("user_level") val userLevel: String,
    @SerializedName("estimated_song_level") val estimatedSongLevel: String,
    val language: String,
    @SerializedName("image_url") val imageUrl: String?,

    @SerializedName("analyzed_lyrics")
    val analyzedLyrics: List<AnalyzedLine>,

    @SerializedName("word_stats")
    val wordStats: WordStats,

    val status: String
)

data class AnalyzedLine(
    val id: Int,
    val original: String,
    @SerializedName("highlighted_words") val highlightedWords: List<HighlightedWord>,
    @SerializedName("line_number") val lineNumber: Int
)

data class HighlightedWord(
    val word: String,
    val level: String,
    val translation: String,
    val definition: String,
    val color: String?
)

data class WordStats(
    @SerializedName("total_words") val totalWords: Int,
    @SerializedName("a1_words") val a1Words: Int,
    @SerializedName("a2_words") val a2Words: Int,
    @SerializedName("b1_words") val b1Words: Int,
    @SerializedName("b2_words") val b2Words: Int,
    @SerializedName("c1_words") val c1Words: Int,
    @SerializedName("c2_words") val c2Words: Int
)

// GET /api/v1/lyrics/with-fragments (Audio segmentado)
data class FragmentsResponse(
    val title: String,
    val artist: String,
    val language: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("preview_url") val previewUrl: String?,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("total_fragments") val totalFragments: Int,
    val fragments: List<LyricFragment>,
    val status: String
)

data class LyricFragment(
    val id: Int,
    val text: String,
    @SerializedName("start_ms") val startMs: Long,
    @SerializedName("end_ms") val endMs: Long,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("line_number") val lineNumber: Int
)

// ===========================================================
// 5️⃣ DICCIONARIO PERSONAL
// ===========================================================
data class AddWordRequest(
    val word: String,
    val translation: String,
    val context: String,
    @SerializedName("source_song_id") val sourceSongId: String,
    @SerializedName("source_song_title") val sourceSongTitle: String,
    @SerializedName("source_artist") val sourceArtist: String,
    val language: String
)

data class UserWord(
    @SerializedName("word_id") val wordId: String,
    val word: String,
    val translation: String,
    val context: String,
    @SerializedName("source_song_title") val sourceSongTitle: String,
    @SerializedName("difficulty_rating") val difficultyRating: Int? = 0,
    @SerializedName("review_count") val reviewCount: Int? = 0
)

data class DictionaryResponse(
    val total: Int,
    val words: List<UserWord>,
    val status: String
)

// ===========================================================
// 6️⃣ FLASHCARDS
// ===========================================================
data class CreateFlashcardRequest(
    val word: String,
    val translation: String,
    val context: String,
    @SerializedName("source_song_id") val sourceSongId: String,
    val language: String
)

data class FlashcardItem(
    @SerializedName("card_id") val cardId: String,
    val word: String,
    val translation: String,
    val context: String,
    @SerializedName("next_review") val nextReview: String?,
    @SerializedName("correct_count") val correctCount: Int? = 0,
    @SerializedName("incorrect_count") val incorrectCount: Int? = 0
)

data class FlashcardReviewResponse(
    @SerializedName("total_to_review") val totalToReview: Int,
    val flashcards: List<FlashcardItem>,
    val status: String
)

data class ReviewRequest(val result: String) // "correct" o "incorrect"

// ===========================================================
// 7️⃣ SESIONES DE ESTUDIO
// ===========================================================
data class CreateSessionRequest(
    @SerializedName("song_id") val songId: String,
    @SerializedName("song_name") val songName: String,
    val artist: String,
    val language: String
)

data class SessionResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("song_id") val songId: String,
    @SerializedName("started_at") val startedAt: String,
    val status: String
)

data class SessionItem(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("song_name") val songName: String,
    val artist: String,
    @SerializedName("duration_minutes") val durationMinutes: Double?,
    @SerializedName("words_added") val wordsAdded: Int?
)

data class SessionListResponse(
    val total: Int,
    val sessions: List<SessionItem>,
    val status: String
)

// ===========================================================
// 8️⃣ PROGRESO Y SALUD
// ===========================================================
data class ProgressResponse(
    @SerializedName("total_words_learned") val totalWordsLearned: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("stats_by_language") val statsByLanguage: Map<String, LanguageStats>,
    val status: String
)

data class LanguageStats(
    @SerializedName("words_learned") val wordsLearned: Int,
    @SerializedName("estimated_level") val estimatedLevel: String
)

data class HealthResponse(val status: String)