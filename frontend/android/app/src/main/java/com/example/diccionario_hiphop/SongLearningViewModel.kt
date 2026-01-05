package com.example.diccionario_hiphop

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongLearningViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _isAiAnalyzing = MutableLiveData<Boolean>()
    val isAiAnalyzing: LiveData<Boolean> get() = _isAiAnalyzing
   
    private val repository = SongRepository(application)

    // Estados UI
    private val _lyricsState = MutableLiveData<String>()
    val lyricsState: LiveData<String> get() = _lyricsState

    private val _analysisState = MutableLiveData<HighlightWordsResponse?>()
    val analysisState: LiveData<HighlightWordsResponse?> get() = _analysisState

    private val _loadingState = MutableLiveData<Boolean>()
    val loadingState: LiveData<Boolean> get() = _loadingState

    private val _errorState = MutableLiveData<String>()
    val errorState: LiveData<String> get() = _errorState

    // 🔥 FASE 4: Validaciones anti-burnout
    private val _burnoutError = MutableLiveData<String?>()
    val burnoutError: LiveData<String?> get() = _burnoutError
    
    private val _dailyGoalWarning = MutableLiveData<String?>()
    val dailyGoalWarning: LiveData<String?> get() = _dailyGoalWarning

    private val _audioStreamState = MutableLiveData<String?>()
    val audioStreamState: LiveData<String?> get() = _audioStreamState

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> get() = _statusMessage

    private val _isPreviewOnly = MutableLiveData<Boolean>()
    val isPreviewOnly: LiveData<Boolean> get() = _isPreviewOnly

    // 🎵 Letras sincronizadas (LRC) para fragmentos de audio
    private val _syncedLyrics = MutableLiveData<String?>()
    val syncedLyrics: LiveData<String?> get() = _syncedLyrics

    // 🎬 Metadata de la canción actual
    private var currentSongTitle: String = ""
    private var currentSongArtist: String = ""

    /**
     * 🎯 ESTRATEGIA NUEVA: Esperar Audio Completo (Loading...) -> Fallback Preview
     */
    fun loadContent(title: String, artist: String, userLevel: String, fallbackPreviewUrl: String?) {
        // Guardar metadatos de la canción
        currentSongTitle = title
        currentSongArtist = artist
        
        // 1. Iniciamos carga
        _loadingState.value = true
        _audioStreamState.value = null
        startLoadingCycle()

        viewModelScope.launch {
            // A. Lanzamos la búsqueda de letras en paralelo
            val lyricsDeferred = async {
                try {
                    repository.getLyrics(title, artist)
                } catch (e: Exception) {
                    null
                }
            }

            // B. Buscar audio completo (bloqueamos aquí la UI intencionalmente)
            val fullAudioResult = getFullAudioSuspended(title, artist)

            if (fullAudioResult != null) {
                _audioStreamState.value = fullAudioResult.url
                _isPreviewOnly.value = false
                _statusMessage.value = "¡Listo! Reproduciendo versión completa"
            } else {
                if (!fallbackPreviewUrl.isNullOrEmpty()) {
                    _audioStreamState.value = fallbackPreviewUrl
                    _isPreviewOnly.value = true
                    _statusMessage.value = "Audio completo no disponible. Usando preview."
                } else {
                    _statusMessage.value = "No se encontró audio reproducible"
                }
            }

            // D. Procesar Letras
            val lyricsResponse = lyricsDeferred.await()
            if (lyricsResponse != null && lyricsResponse.isSuccessful && lyricsResponse.body() != null) {
                val data = lyricsResponse.body()!!
                val lyricsText = data.lyrics
                
                // Guardar synced_lyrics para uso posterior
                _syncedLyrics.value = data.syncedLyrics
                
                // Verificar si hay letra o está vacía
                if (lyricsText.isNullOrEmpty()) {
                    // ⚠️ NO HAY LETRA DISPONIBLE
                    _lyricsState.value = "😔 Letra no disponible\n\nLo sentimos, no pudimos encontrar la letra de esta canción en nuestras fuentes (LRCLib, Genius).\n\n¡Pero puedes disfrutar del audio! 🎵"
                    _statusMessage.value = "Letra no encontrada, pero el audio está listo"
                    // NO ejecutar análisis IA si no hay letra
                } else {
                    // ✅ HAY LETRA - Proceso normal
                    _lyricsState.value = lyricsText
                    analyzeLyricsInBackground(title, artist, userLevel, lyricsText)
                }

                // Fallback de audio: Si no tenemos audio completo ni yt-dlp, usar preview del backend
                if (_audioStreamState.value == null && !data.previewUrl.isNullOrEmpty()) {
                    _audioStreamState.value = data.previewUrl
                    _isPreviewOnly.value = true
                }
            } else {
                // Error crítico al obtener respuesta del backend
                _lyricsState.value = "❌ Error de conexión\n\nNo se pudo conectar con el servidor para obtener la letra.\n\nIntenta de nuevo más tarde."
                _errorState.value = "No se pudieron cargar las letras"
            }

            // 2. ¡FIN! Ocultamos carga
            _loadingState.value = false
        }
    }

    // Mensajes de carga animados
    private fun startLoadingCycle() {
        viewModelScope.launch {
            val messages = listOf(
                "🎧 Afinando instrumentos...",
                "📡 Buscando audio de alta calidad...",
                "🎤 Calentando la voz...",
                "🕵️‍♀️ Escaneando letras ocultas...",
                "🧠 La IA está pensando...",
                "🎵 Conectando con el estudio...",
                "👨‍🏫 Contactando con el tutor..."
            )
            var index = 0
            while (_loadingState.value == true) {
                _statusMessage.postValue(messages[index])
                kotlinx.coroutines.delay(2500)
                index = (index + 1) % messages.size
            }
        }
    }

    // 🔥 CARGA DEL AUDIO COMPLETO (Suspensión)
    // Devuelve el resultado o null, sin efectos secundarios en la UI directa
    private suspend fun getFullAudioSuspended(title: String, artist: String): AudioResult? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Intentar extracción nuclear local (yt-dlp)
                val result = GrayjayAudioExtractor.getAudioWithMetadata("$artist - $title")
                if (result != null) {
                    return@withContext result
                }
                // 2. Si falla, podrías intentar aquí otro método de preview si lo implementas
                // Por ahora, solo retorna null si yt-dlp falla
                null
            } catch (e: Exception) {
                Log.e("ViewModel", "Error buscando audio completo", e)
                null
            }
        }
    }

    /**
     * 🤖 ANÁLISIS IA EN BACKGROUND
     */
    private fun analyzeLyricsInBackground(title: String, artist: String, level: String, lyrics: String) {
        _isAiAnalyzing.value = true 
        viewModelScope.launch {
            try {
                val aiRes = repository.analyzeLyrics(title, artist, level, lyrics)
                if (aiRes.isSuccessful && aiRes.body() != null) {
                    _analysisState.value = aiRes.body()
                    _statusMessage.value = null // Limpiamos mensaje si todo salió bien
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error en análisis IA: ${e.message}")
            } finally {
                _isAiAnalyzing.value = false
            }
        }
    }

    /**
     * 💾 GUARDAR PALABRA EN DICCIONARIO
     * 🔥 FASE 4: Con validación anti-burnout
     * 🎵 FASE 5: Con metadatos de audio para fragmentos originales
     */
    fun addToDictionary(term: String, definition: String, explanation: String, example: String, isExpression: Boolean) {
       viewModelScope.launch {
            try {
                 // 🔥 Obtener el idioma detectado del análisis actual
                 val detectedLanguage = _analysisState.value?.detectedLanguage ?: "en"
                 
                 // 🎵 Intentar extraer timestamp del synced_lyrics
                 val timestamps = parseLrcTimestamp(_syncedLyrics.value, term)
                 val youtubeUrl = getYoutubeSearchUrl()
                 
                 // Log para debugging
                 Log.d("SongLearningViewModel", "💾 Guardando palabra: $term")
                 Log.d("SongLearningViewModel", "🎵 YouTube URL: $youtubeUrl")
                 Log.d("SongLearningViewModel", "⏱️ Timestamps: $timestamps")
                 
                 val request = AddWordRequest(
                    word = term,
                    translation = definition,
                    language = detectedLanguage,  // 🌍 IDIOMA DETECTADO
                    notes = explanation,
                    type = if (isExpression) "expression" else "word",
                    example = example,
                    isRecommended = false,
                    songId = null,
                    // 🎵 Metadatos de audio para fragmentos
                    songYoutubeUrl = youtubeUrl,
                    timestampStart = timestamps?.first,
                    timestampEnd = timestamps?.second
                )
                val response = repository.addWord(request)
                
                // 🔥 BURNOUT CAP: HTTP 400
                if (response.code() == 400) {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    // Parsear el detail del backend
                    val errorMsg = try {
                        // Backend devuelve: {"detail": "Tienes X flashcards pendientes..."}
                        val regex = "\"detail\":\"([^\"]+)\"".toRegex()
                        regex.find(errorBody)?.groupValues?.get(1) ?: errorBody
                    } catch (e: Exception) {
                        "Tienes demasiadas flashcards pendientes. Repasa antes de añadir más."
                    }
                    _burnoutError.value = errorMsg
                    // Auto-reset después de mostrar
                    _burnoutError.value = null
                    return@launch
                }
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    
                    // 🔥 DAILY GOAL WARNING: Campo warning en respuesta
                    if (responseBody != null) {
                        val warning = responseBody.warning
                        if (!warning.isNullOrEmpty()) {
                            _dailyGoalWarning.value = warning
                            // Auto-reset después de mostrar
                            _dailyGoalWarning.value = null
                        }
                    }
                    
                    // Marcar como guardado en la UI
                    val current = _analysisState.value
                    if (current != null) {
                         if (isExpression) {
                            val updatedExpressions = current.expressions.map {
                                if (it.expression.equals(term, ignoreCase = true))
                                    it.copy(alreadySaved = true, color = "gray")
                                else it
                            }
                            _analysisState.value = current.copy(expressions = updatedExpressions)
                        } else {
                            val updatedWords = current.words.map {
                                if (it.word.equals(term, ignoreCase = true))
                                    it.copy(alreadySaved = true, color = "gray")
                                else it
                            }
                            _analysisState.value = current.copy(words = updatedWords)
                        }
                    }
                } else {
                    _errorState.value = "Error al guardar: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorState.value = "Error de red: ${e.message}"
            }
       }
    }

    /**
     * 🎵 PARSEAR TIMESTAMPS DEL FORMATO LRC
     * Busca una palabra/expresión en synced_lyrics y extrae el timestamp
     * Formato LRC: "[MM:SS.mm]texto de la canción"
     * 
     * @param syncedLyrics Letras sincronizadas en formato LRC
     * @param searchTerm Palabra o expresión a buscar
     * @return Par de (timestamp_start, timestamp_end) en segundos, o null si no se encuentra
     */
    private fun parseLrcTimestamp(syncedLyrics: String?, searchTerm: String): Pair<Float, Float>? {
        if (syncedLyrics.isNullOrEmpty()) {
            Log.d("SongLearningViewModel", "No hay synced_lyrics disponibles")
            return null
        }
        
        // Regex para parsear líneas LRC: [MM:SS.mm]texto o [MM:SS]texto
        val lrcRegex = """\[(\d{2}):(\d{2})(?:\.(\d{2}))?\](.+)""".toRegex()
        val lines = syncedLyrics.lines()
        
        Log.d("SongLearningViewModel", "Buscando '$searchTerm' en ${lines.size} líneas de synced_lyrics")
        
        for (i in lines.indices) {
            val match = lrcRegex.find(lines[i]) ?: continue
            val groups = match.groupValues
            val minutes = groups[1].toInt()
            val seconds = groups[2].toInt()
            val centiseconds = if (groups[3].isNotEmpty()) groups[3].toInt() else 0
            val text = groups[4]
            
            // Buscar la palabra/expresión en el texto (case-insensitive)
            if (text.contains(searchTerm, ignoreCase = true)) {
                // Convertir a segundos
                val startTime = minutes * 60 + seconds + centiseconds / 100f
                
                Log.d("SongLearningViewModel", "✅ Encontrado '$searchTerm' en: $text (timestamp: $startTime)")
                
                // Para timestamp_end, usar la siguiente línea o añadir 3 segundos
                val endTime = if (i + 1 < lines.size) {
                    val nextMatch = lrcRegex.find(lines[i + 1])
                    if (nextMatch != null) {
                        val nextGroups = nextMatch.groupValues
                        val nextMin = nextGroups[1].toInt()
                        val nextSec = nextGroups[2].toInt()
                        val nextCenti = if (nextGroups[3].isNotEmpty()) nextGroups[3].toInt() else 0
                        nextMin * 60 + nextSec + nextCenti / 100f
                    } else {
                        startTime + 3f // Buffer de 3 segundos si no hay siguiente línea válida
                    }
                } else {
                    startTime + 3f
                }
                
                Log.d("SongLearningViewModel", "⏱️ Timestamps: start=$startTime, end=$endTime")
                return Pair(startTime, endTime)
            }
        }
        
        Log.d("SongLearningViewModel", "❌ No se encontró '$searchTerm' en las letras sincronizadas")
        return null // No se encontró la palabra en las letras sincronizadas
    }

    /**
     * 🎬 OBTENER URL DE YOUTUBE DE LA CANCIÓN ACTUAL
     * Construye una URL de búsqueda de YouTube con el título y artista
     */
    private fun getYoutubeSearchUrl(): String? {
        if (currentSongTitle.isEmpty() || currentSongArtist.isEmpty()) {
            return null
        }
        
        // Construir query de búsqueda
        val searchQuery = "$currentSongArtist $currentSongTitle"
        val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
        
        // Retornar URL de búsqueda que yt-dlp puede procesar
        return "ytsearch:$searchQuery"
    }
}
