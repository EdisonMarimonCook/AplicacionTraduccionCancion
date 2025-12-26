package com.example.diccionario_hiphop

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SongLearningViewModel(application: Application) : AndroidViewModel(application) {
    
    // 👇 Estado IA
    private val _isAiAnalyzing = MutableLiveData<Boolean>()
    val isAiAnalyzing: LiveData<Boolean> get() = _isAiAnalyzing
   
    // Mantenemos tu repositorio (SongRepository) para la IA y base de datos
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

    private var currentSongTitle: String? = null

    /**
     * 🔥 NUEVA LÓGICA HÍBRIDA:
     * Aceptamos 'fallbackPreviewUrl' (lo que viene de Spotify/iTunes)
     */
    fun loadContent(title: String, artist: String, userLevel: String, fallbackPreviewUrl: String?) {
        if (_lyricsState.value != null && currentSongTitle == title) return
        currentSongTitle = title

        viewModelScope.launch {
            // 1. TELÓN ABAJO
            _loadingState.value = true
            _statusMessage.value = null
            _errorState.value = ""
            _isAiAnalyzing.value = false 

            try {
                // --- FASE 1: PARALELISMO REAL (Audio Local + Letra Backend) ---
                
                // A) Buscar Audio en YouTube (Localmente con NewPipe)
                val audioDeferred = async {
                    val audioQuery = "$artist - $title audio" // "Nas NY State of Mind audio"
                    Log.d("ViewModel", "🔍 Buscando en YouTube: $audioQuery")
                    YoutubeStreamExtractor.getStreamUrl(getApplication(), audioQuery)
                }

                // B) Buscar Letra (Backend Python) - ESTO SE MANTIENE
                val lyricsDeferred = async {
                    repository.getLyrics(title, artist)
                }

                // Esperamos resultados
                val youtubeUrl = audioDeferred.await()
                val lyricsResponse = lyricsDeferred.await()

                // --- FASE 2: GESTIÓN DE AUDIO (Híbrido) ---
                if (youtubeUrl != null) {
                    // ✅ Éxito: Canción completa de YouTube
                    Log.d("ViewModel", "✅ Audio encontrado en YouTube")
                    _audioStreamState.value = youtubeUrl
                    _statusMessage.value = "Reproduciendo versión completa (YouTube)"
                } else {
                    // ❌ Fallo YouTube: Usamos el plan B (Preview de 30s)
                    Log.d("ViewModel", "⚠️ YouTube falló. Usando fallback.")
                    if (!fallbackPreviewUrl.isNullOrEmpty()) {
                        _audioStreamState.value = fallbackPreviewUrl
                        _statusMessage.value = "YouTube no disponible. Usando preview (30s)."
                    } else {
                        _statusMessage.value = "Audio no disponible por el momento."
                    }
                }

                // --- FASE 3: GESTIÓN DE LETRA ---
                var lyricsText = ""
                if (lyricsResponse.isSuccessful && lyricsResponse.body() != null) {
                    lyricsText = lyricsResponse.body()!!.lyrics
                    _lyricsState.value = lyricsText
                } else {
                    _errorState.value = "No se encontró la letra."
                    _loadingState.value = false
                    return@launch 
                }

                // 🚀 ¡ABRIR TELÓN!
                _loadingState.value = false

                // --- FASE 4: LA IA TRABAJA EN LA SOMBRA ---
                _isAiAnalyzing.value = true 
                launch {
                    try {
                        val aiRes = repository.analyzeLyrics(title, artist, userLevel, lyricsText)
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

            } catch (e: Exception) {
                e.printStackTrace()
                _errorState.value = "Error general: ${e.message}"
                _loadingState.value = false
            }
        }
    }

    // (Tu función addToDictionary sigue igual, no la toco)
    fun addToDictionary(term: String, definition: String, explanation: String, example: String, isExpression: Boolean) {
       // ... código original de guardado ...
       viewModelScope.launch {
            try {
                // Simulamos la llamada (necesitaría ver tu AddWordRequest original para copiarlo exacto, 
                // pero asumo que tu SongRepository lo maneja bien como lo tenías)
                // Aquí va tu código original de addToDictionary que me pasaste
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
                // ... resto de tu lógica de actualización de estado ...
                 if (response.isSuccessful) {
                    val current = _analysisState.value
                    if (current != null) {
                        // (Tu lógica de actualizar colores a gris)
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