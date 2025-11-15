package com.example.diccionario_hiphop

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
    val email: String,
    val username: String
)

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