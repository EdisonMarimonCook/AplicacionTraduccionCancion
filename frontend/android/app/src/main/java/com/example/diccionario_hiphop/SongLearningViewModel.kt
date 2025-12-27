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

    private val _audioStreamState = MutableLiveData<String?>()
    val audioStreamState: LiveData<String?> get() = _audioStreamState

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> get() = _statusMessage

    private val _isPreviewOnly = MutableLiveData<Boolean>()
    val isPreviewOnly: LiveData<Boolean> get() = _isPreviewOnly

    /**
     * 🎯 ESTRATEGIA NUEVA: Esperar Audio Completo (Loading...) -> Fallback Preview
     */
    fun loadContent(title: String, artist: String, userLevel: String, fallbackPreviewUrl: String?) {
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
                _lyricsState.value = data.lyrics

                if (_audioStreamState.value == null && !data.previewUrl.isNullOrEmpty()) {
                    _audioStreamState.value = data.previewUrl
                    _isPreviewOnly.value = true
                }

                analyzeLyricsInBackground(title, artist, userLevel, data.lyrics)
            } else {
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
     */
    fun addToDictionary(term: String, definition: String, explanation: String, example: String, isExpression: Boolean) {
       viewModelScope.launch {
            try {
                 val request = AddWordRequest(
                    word = term,
                    translation = definition,
                    notes = explanation,
                    type = if (isExpression) "expression" else "word",
                    example = example,
                    isRecommended = false,
                    songId = null
                )
                val response = repository.addWord(request)
                
                if (response.isSuccessful) {
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
}