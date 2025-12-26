package com.example.diccionario_hiphop

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

    private var currentSongTitle: String? = null

    /**
     * 🔥 ESTRATEGIA FINAL: NewPipe Local (como Grayjay) + Fallback iTunes
     */
    fun loadContent(title: String, artist: String, userLevel: String, fallbackPreviewUrl: String?) {
        if (_lyricsState.value != null && currentSongTitle == title) return
        currentSongTitle = title

        viewModelScope.launch {
            // 1. TELÓN ABAJO
            _loadingState.value = true
            _statusMessage.value = "Cargando letra..."
            _errorState.value = ""
            _isAiAnalyzing.value = false
            _isPreviewOnly.value = false
            _audioStreamState.value = null

            try {
                // PASO 1: OBTENER LETRAS (Backend solo para esto)
                val response = repository.getLyrics(title, artist)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Setear letra inmediatamente
                    _lyricsState.value = data.lyrics
                    _loadingState.value = false // ✅ Mostrar letra YA

                    // PASO 2: BUSCAR AUDIO (EN PARALELO, sin bloquear UI)
                    searchAudioInBackground(title, artist, data.previewUrl, fallbackPreviewUrl)

                    // PASO 3: IA EN LA SOMBRA
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

    /**
     * 🎧 BÚSQUEDA DE AUDIO EN SEGUNDO PLANO (No bloquea la letra)
     */
    private fun searchAudioInBackground(
        title: String, 
        artist: String, 
        backendPreview: String?,
        intentPreview: String?
    ) {
        viewModelScope.launch {
            _statusMessage.value = "Buscando audio..."
            
            // NIVEL 1: NEWPIPE LOCAL (Como Grayjay)
            val youtubeUrl = withTimeoutOrNull(12000L) {
                try {
                    Log.d("ViewModel", "🔍 Intentando NewPipe local...")
                    YoutubeStreamExtractor.getStreamUrl(
                        context = getApplication(),
                        query = "$artist - $title audio"
                    )
                } catch (e: Exception) {
                    Log.e("ViewModel", "❌ NewPipe falló: ${e.message}")
                    null
                }
            }

            if (youtubeUrl != null) {
                _audioStreamState.value = youtubeUrl
                _isPreviewOnly.value = false
                _statusMessage.value = "Reproduciendo audio completo (YouTube)"
                Log.d("ViewModel", "✅ Audio Source: NewPipe Local")
                return@launch
            }

            // NIVEL 2: PREVIEW BACKEND (iTunes 30s)
            if (!backendPreview.isNullOrEmpty()) {
                _audioStreamState.value = backendPreview
                _isPreviewOnly.value = true
                _statusMessage.value = "Reproduciendo preview (30s)"
                Log.d("ViewModel", "⚠️ Audio Source: Backend Preview")
                return@launch
            }

            // NIVEL 3: PREVIEW INTENT (Fallback final)
            if (!intentPreview.isNullOrEmpty()) {
                _audioStreamState.value = intentPreview
                _isPreviewOnly.value = true
                _statusMessage.value = "Reproduciendo preview (30s)"
                Log.d("ViewModel", "⚠️ Audio Source: Intent Preview")
                return@launch
            }

            // SIN AUDIO
            _statusMessage.value = "No se encontró audio para esta canción"
            Log.e("ViewModel", "❌ Audio Source: None")
        }
    }

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