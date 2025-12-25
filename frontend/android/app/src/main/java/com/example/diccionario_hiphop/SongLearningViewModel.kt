package com.example.diccionario_hiphop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SongLearningViewModel(application: Application) : AndroidViewModel(application) {
        // 👇 NUEVO: Estado específico para saber si la IA está trabajando en 2º plano
        private val _isAiAnalyzing = MutableLiveData<Boolean>()
        val isAiAnalyzing: LiveData<Boolean> get() = _isAiAnalyzing
    
    private val repository = SongRepository(application)

    // Estados para que la Vista sepa qué pintar
    private val _lyricsState = MutableLiveData<String>()
    val lyricsState: LiveData<String> get() = _lyricsState

    private val _analysisState = MutableLiveData<HighlightWordsResponse?>()
    val analysisState: LiveData<HighlightWordsResponse?> get() = _analysisState

    private val _loadingState = MutableLiveData<Boolean>()
    val loadingState: LiveData<Boolean> get() = _loadingState

    private val _errorState = MutableLiveData<String>()
    val errorState: LiveData<String> get() = _errorState

    // Estado para la URL del audio
    private val _audioStreamState = MutableLiveData<String?>()
    val audioStreamState: LiveData<String?> get() = _audioStreamState

    // 🔥 NUEVO: Estado para mensajes informativos (Toasts) sin bloquear la app
    // Ejemplo: "Audio listo, pero la IA falló"
    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> get() = _statusMessage

    // Variables para guardar datos y no volver a pedir
    private var currentSongTitle: String? = null
    
    // Cache placeholders (Fase 3)
    private var cachedAnalysis: HighlightWordsResponse? = null
    private var cachedSongId: String? = null

    fun loadContent(title: String, artist: String, userLevel: String) {
        if (_lyricsState.value != null && currentSongTitle == title) return
        currentSongTitle = title

        viewModelScope.launch {
            // 1. TELÓN ABAJO: Pantalla de carga completa
            _loadingState.value = true
            _statusMessage.value = null
            _errorState.value = ""
            _isAiAnalyzing.value = false // La IA aún no empieza

            try {
                // --- FASE 1: PARALELISMO REAL (Audio + Letra) ---
                val audioDeferred = async {
                    val audioQuery = "$title $artist audio"
                    repository.getStreamUrl(audioQuery)
                }
                val lyricsDeferred = async {
                    repository.getLyrics(title, artist)
                }

                // 🛑 PUNTO DE ESPERA: Solo esperamos lo básico para que el usuario empiece
                val audioResult = audioDeferred.await()
                val lyricsResponse = lyricsDeferred.await()

                // --- FASE 2: PROCESAR BÁSICOS Y ABRIR TELÓN ---
                if (audioResult != null) {
                    _audioStreamState.value = audioResult
                } else {
                    _statusMessage.value = "Audio no disponible por el momento."
                }

                var lyricsText = ""
                if (lyricsResponse.isSuccessful && lyricsResponse.body() != null) {
                    lyricsText = lyricsResponse.body()!!.lyrics
                    _lyricsState.value = lyricsText // ¡Pinta la letra ya!
                } else {
                    _errorState.value = "No se encontró la letra."
                    _loadingState.value = false
                    return@launch // Sin letra no hay app, salimos.
                }

                // 🚀 ¡ABRIR TELÓN! El usuario ya puede leer y escuchar.
                _loadingState.value = false

                // --- FASE 3: LA IA TRABAJA EN LA SOMBRA (Background) ---
                _isAiAnalyzing.value = true // Encendemos el spinner pequeñito
                launch {
                    try {
                        val aiRes = repository.analyzeLyrics(title, artist, userLevel, lyricsText)
                        if (aiRes.isSuccessful && aiRes.body() != null) {
                            _analysisState.value = aiRes.body()
                            _statusMessage.value = "¡Análisis inteligente completado!"
                        } else {
                            _statusMessage.value = "La IA se está enfriando, disfruta de la música."
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ViewModel", "Fallo IA background: ${e.message}")
                    } finally {
                        _isAiAnalyzing.value = false // Apagamos el spinner pequeñito siempre
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _errorState.value = "Error de conexión: ${e.message}"
                _loadingState.value = false // Asegurar que quitamos el bloqueo si falla todo
            }
        }
    }

    // Esta función se queda igual, funcionaba bien
    fun addToDictionary(
        term: String,
        translation: String,
        explanation: String,
        example: String,
        isExpression: Boolean
    ) {
        viewModelScope.launch {
            try {
                val request = AddWordRequest(
                    word = term,
                    translation = translation,
                    notes = explanation,
                    type = if (isExpression) "expression" else "word",
                    example = example,
                    isRecommended = false,
                    songId = null
                )

                val response = repository.addWord(request)

                if (response.isSuccessful) {
                    // Feedback instantáneo: actualizar analysisState localmente
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
                _errorState.value = "Error de conexión: ${e.message}"
            }
        }
    }
}