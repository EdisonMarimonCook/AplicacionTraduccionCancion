package com.example.diccionario_hiphop

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SongLearningViewModel(application: Application) : AndroidViewModel(application) {
    
    // 👇 Estado IA
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

    // Nuevo: Estado para indicar si solo hay preview
    private val _isPreviewOnly = MutableLiveData<Boolean>()
    val isPreviewOnly: LiveData<Boolean> get() = _isPreviewOnly

    private var currentSongTitle: String? = null

    /**
     * 🔥 NUEVA LÓGICA SIMPLIFICADA (Vía Cobalt Backend)
     */
    fun loadContent(title: String, artist: String, userLevel: String, fallbackPreviewUrl: String?) {
        if (_lyricsState.value != null && currentSongTitle == title) return
        currentSongTitle = title

        viewModelScope.launch {
            // 1. TELÓN ABAJO
            _loadingState.value = true
            _statusMessage.value = "Cargando letra y audio..."
            _errorState.value = ""
            _isAiAnalyzing.value = false
            _isPreviewOnly.value = false
            _audioStreamState.value = null // Reseteamos audio previo

            try {
                // --- LLAMADA ÚNICA AL BACKEND ---
                // Ahora getLyrics trae la letra Y el audio completo (fullAudioUrl)
                val response = repository.getLyrics(title, artist)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // 1. SETEAR LETRA
                    _lyricsState.value = data.lyrics

                    // 2. LOGICA DE SELECCIÓN DE AUDIO (Jerarquía de calidad)
                    val fullAudio = data.fullAudioUrl
                    val previewAudio = data.previewUrl
                    
                    var finalUrl: String? = null

                    when {
                        // A. Prioridad Máxima: Audio Completo de Cobalt
                        !fullAudio.isNullOrEmpty() -> {
                            finalUrl = fullAudio
                            _isPreviewOnly.value = false
                            _statusMessage.value = "Reproduciendo versión completa (YouTube)"
                            Log.d("ViewModel", "✅ Audio Source: Cobalt Full Audio")
                            _isPreviewOnly.value = true
                            _statusMessage.value = "Solo preview disponible (30s)"
                            Log.d("ViewModel", "⚠️ Audio Source: Backend Preview")
                        }
                        // C. Último recurso: Preview que venía de la pantalla anterior (Spotify)
                        !fallbackPreviewUrl.isNullOrEmpty() -> {
                            finalUrl = fallbackPreviewUrl
                            _isPreviewOnly.value = true
                            _statusMessage.value = "Solo preview disponible (30s)"
                            Log.d("ViewModel", "⚠️ Audio Source: Fallback Intent")
                        }
                        else -> {
                            _statusMessage.value = "No se encontró audio para esta canción."
                            Log.e("ViewModel", "❌ Audio Source: None")
                        }
                    }

                    _audioStreamState.value = finalUrl

                    // 🚀 ¡ABRIR TELÓN! (Letra y Audio listos)
                    _loadingState.value = false

                    // --- FASE 3: LA IA TRABAJA EN LA SOMBRA ---
                    // Iniciamos el análisis después de mostrar la letra para no bloquear la UI
                    analyzeLyricsInBackground(title, artist, userLevel, data.lyrics)

                } else {
                    _errorState.value = "No se encontró la letra (Error ${response.code()})"
                    _loadingState.value = false
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _errorState.value = "Error de conexión: ${e.message}"
                _loadingState.value = false
            }
        }
    }

    // Separé la IA en una función privada para limpiar loadContent
    private fun analyzeLyricsInBackground(title: String, artist: String, level: String, lyrics: String) {
        _isAiAnalyzing.value = true 
        viewModelScope.launch {
            try {
                val aiRes = repository.analyzeLyrics(title, artist, level, lyrics)
                if (aiRes.isSuccessful && aiRes.body() != null) {
                    _analysisState.value = aiRes.body()
                    _statusMessage.value = "¡Análisis inteligente completado!"
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Fallo IA background: ${e.message}")
            } finally {
                _isAiAnalyzing.value = false
            }
        }
    }

    // Funcionalidad de Diccionario (Manteniendo tu lógica original)
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
                    // Actualizar UI (Poner palabra en gris)
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