package com.example.diccionario_hiphop

<<<<<<< HEAD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import retrofit2.Response

class SongRepository(private val authToken: String? = null) {
    private val TAG = "SongRepository"
    private val apiService = RetrofitService.apiService

    suspend fun getTopSongs(language: String = "es"): List<Song> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Obteniendo canciones top...")

                // Por ahora, devolvemos datos mock
                // Cuando la API esté lista, descomenta esto:
                /*
                val response = apiService.getTopSongs(
                    token = "Bearer $authToken",
                    language = language
                )

                if (response.isSuccessful) {
                    response.body()?.songs?.map { topSong ->
                        Song(
                            id = topSong.id,
                            title = topSong.name,
                            artist = topSong.artist,
                            spotifyRank = topSong.position,
                            lyrics = "",
                            terms = emptyList(),
                            albumCover = topSong.image_url
                        )
                    } ?: emptyList()
                } else {
                    Log.e(TAG, "❌ Error HTTP: ${response.code()}")
                    emptyList()
                }
                */

                // Datos mock temporales
                getMockSongs()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo canciones: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun getAnalyzedLyrics(title: String, artist: String, userLevel: String): AnalysisResponse? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Analizando letra: $title - $artist")

                // Por ahora, devolvemos análisis mock
                // Cuando la API esté lista, descomenta esto:
                /*
                val response = apiService.analyzeLyrics(
                    token = "Bearer $authToken",
                    title = title,
                    artist = artist,
                    userLevel = userLevel
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Análisis obtenido")
                    response.body()
                } else {
                    Log.e(TAG, "❌ Error en análisis: ${response.code()}")
                    null
                }
                */

                // Análisis mock temporal
                createMockAnalysis(title, artist, userLevel)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción en análisis: ${e.message}")
                null
            }
        }
    }

    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.healthCheck()
                response.isSuccessful && response.body()?.status == "healthy"
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en test conexión: ${e.message}")
                false
            }
        }
    }

    // Datos mock para desarrollo
    private fun getMockSongs(): List<Song> {
        return listOf(
            Song(
                id = "1",
                title = "Endless Possibility",
                artist = "Sonic",
                spotifyRank = 1,
                lyrics = "This is my escape\nI'm running through this world\nAnd I'm not looking back",
                terms = emptyList(),
                albumCover = ""
            ),
            Song(
                id = "2",
                title = "Lose Yourself",
                artist = "Eminem",
                spotifyRank = 2,
                lyrics = "His palms are sweaty, knees weak, arms are heavy",
                terms = emptyList(),
                albumCover = ""
            ),
            Song(
                id = "3",
                title = "God's Plan",
                artist = "Drake",
                spotifyRank = 3,
                lyrics = "I been movin' calm, don't start no trouble with me",
                terms = emptyList(),
                albumCover = ""
            )
        )
    }

    private fun createMockAnalysis(title: String, artist: String, userLevel: String): AnalysisResponse {
        return AnalysisResponse(
            title = title,
            artist = artist,
            user_level = userLevel,
            estimated_song_level = "B1",
            language = "en",
            image_url = "",
            analyzed_lyrics = listOf(
                AnalyzedLine(
                    id = 0,
                    original = "This is my escape",
                    highlighted_words = listOf(
                        HighlightedWord(
                            word = "escape",
                            level = "A2",
                            translation = "escapada",
                            definition = "Way out or refuge from something",
                            color = "yellow"
                        )
                    ),
                    line_number = 1
                ),
                AnalyzedLine(
                    id = 1,
                    original = "I'm running through this world",
                    highlighted_words = listOf(
                        HighlightedWord(
                            word = "running",
                            level = "A1",
                            translation = "corriendo",
                            definition = "Moving fast on foot",
                            color = "green"
                        )
                    ),
                    line_number = 2
                ),
                AnalyzedLine(
                    id = 2,
                    original = "And I'm not looking back",
                    highlighted_words = listOf(
                        HighlightedWord(
                            word = "looking",
                            level = "A2",
                            translation = "mirando",
                            definition = "Directing one's gaze toward something",
                            color = "yellow"
                        )
                    ),
                    line_number = 3
                )
            ),
            word_stats = WordStats(
                total_words = 150,
                a1_words = 45,
                a2_words = 35,
                b1_words = 40,
                b2_words = 20,
                c1_words = 10,
                c2_words = 0
            ),
            status = "success"
        )
=======
import android.content.Context
import retrofit2.Response

class SongRepository(context: Context) {

    private val apiService = RetrofitService.getInstance(context)

    suspend fun searchSongs(query: String): Response<List<SongItem>> {
        return apiService.searchSongs(query)
    }

    suspend fun getTopGrammy(language: String = "en"): Response<List<SongItem>> {
        return apiService.getTopGrammy(language)
    }

    // 🔥 CORRECCIÓN CLAVE: Añadimos el parámetro 'lyrics'
    suspend fun analyzeLyrics(title: String, artist: String, userLevel: String, lyrics: String): Response<HighlightWordsResponse> {
        val request = AnalyzeLyricsRequest(
            title = title,
            artist = artist,
            lyrics = lyrics, // ✅ Ahora enviamos la letra real, no vacía
            userLevel = userLevel
        )
        return apiService.analyzeLyrics(request)
    }

    suspend fun getLyrics(title: String, artist: String): Response<LyricsResponse> {
        return apiService.getLyrics(title, artist)
    }

    suspend fun addWord(request: AddWordRequest): Response<UserWord> {
        return apiService.addWord(request)
>>>>>>> feature/lyrics-translation
    }
}