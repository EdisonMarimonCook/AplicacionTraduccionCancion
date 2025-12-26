package com.example.diccionario_hiphop

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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

    private var currentSongTitle: String? = null

    /**
     * 🎯 ESTRATEGIA HÍBRIDA: Backend + Fallback Grayjay
     */
    fun loadContent(title: String, artist: String, userLevel: String, fallbackPreviewUrl: String?) {
        // 1. Limpieza inicial
        _loadingState.value = true
        _audioStreamState.value = null
        _statusMessage.value = "Cargando..."
        
        // 2. 🚨 CARGA DE EMERGENCIA (Preview):
        // Si viene del Intent (Spotify/iTunes), lo ponemos YA para desbloquear la UI.
        if (!fallbackPreviewUrl.isNullOrEmpty()) {
            _audioStreamState.value = fallbackPreviewUrl
            _isPreviewOnly.value = true
            _statusMessage.value = "🎵 Preview (Buscando completa...)"
        }

        // 3. 🚀 LANZAR "GRAYJAY" EN PARALELO (Hilo IO)
        fetchFullAudioGrayjay(title, artist)

        // 4. PEDIR LETRAS AL BACKEND (Como siempre)
        viewModelScope.launch {
            try {
                val response = repository.getLyrics(title, artist)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    _lyricsState.value = data.lyrics
                    
                    // Si el backend trae una preview mejor y aun no tenemos audio full
                    if (_audioStreamState.value == null && !data.previewUrl.isNullOrEmpty()) {
                         _audioStreamState.value = data.previewUrl
                         _isPreviewOnly.value = true
                    }
                    
                    _loadingState.value = false // UI lista para leer
                    
                    // IA en background
                    analyzeLyricsInBackground(title, artist, userLevel, data.lyrics)
                } else {
                    _errorState.value = "Error letras: ${response.code()}"
                    _loadingState.value = false
                }
            } catch (e: Exception) {
                _errorState.value = "Error red: ${e.message}"
                _loadingState.value = false
            }
        }
    }

    private fun fetchFullAudioGrayjay(title: String, artist: String) {
        viewModelScope.launch(Dispatchers.IO) { // ⚡ HILO SEGUNDO PLANO
            try {
                // Truco: Añadir "official audio" mejora mucho la puntería de Piped/Invidious
                val query = "$artist - $title official audio"
                
                Log.d("ViewModel", "🔍 Buscando audio en GrayjayEngine: '$query'")
                
                // Llamamos a nuestro extractor blindado
                val fullUrl = GrayjayAudioExtractor.getAudioStreamUrl(query)

                withContext(Dispatchers.Main) { // ⚡ VOLVER A UI
                    if (fullUrl != null) {
                        Log.d("ViewModel", "🎉 AUDIO ENCONTRADO: $fullUrl")
                        
                        // Actualizamos el LiveData que observa la Activity
                        _audioStreamState.value = fullUrl 
                        
                        // Quitamos modo preview y avisamos al usuario
                        _isPreviewOnly.value = false
                        _statusMessage.value = "✅ Audio Completo Listo"
                    } else {
                        Log.w("ViewModel", "⚠️ No se encontró audio full. Nos quedamos con lo que haya.")
                        // No tocamos _audioStreamState para no romper la preview si ya estaba sonando
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "💥 Error en motor de audio", e)
            }
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