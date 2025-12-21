package com.example.diccionario_hiphop

import androidx.lifecycle.viewModelScope
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SongLearningViewModel(application: Application) : AndroidViewModel(application) {
    
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

    // Variables para guardar datos y no volver a pedir
    private var currentSongTitle: String? = null
    
    fun loadContent(title: String, artist: String, userLevel: String) {
        // SI YA TENEMOS DATOS, NO HACEMOS NADA (Así evitamos recargas al girar/modo oscuro)
        if (_lyricsState.value != null) return

        currentSongTitle = title
        _loadingState.value = true
        _lyricsState.value = "Buscando letra..."

        viewModelScope.launch {
            try {
                // 1. Obtener Letra
                val lyricsRes = repository.getLyrics(title, artist)
                
                if (lyricsRes.isSuccessful && lyricsRes.body() != null) {
                    val lyrics = lyricsRes.body()!!.lyrics
                    _lyricsState.value = lyrics // Actualizamos UI con letra plana

                    // 2. Analizar con IA
                    val aiRes = repository.analyzeLyrics(title, artist, userLevel, lyrics)
                    if (aiRes.isSuccessful && aiRes.body() != null) {
                        _analysisState.value = aiRes.body() // Actualizamos UI con colores
                    } else {
                        _errorState.value = "Error IA: ${aiRes.code()}"
                    }
                } else {
                    _errorState.value = "Letra no encontrada"
                }
            } catch (e: Exception) {
                _errorState.value = "Error: ${e.message}"
            } finally {
                _loadingState.value = false
            }
        }
    }

    fun addToDictionary(term: String, definition: String, example: String, isExpression: Boolean) {
        viewModelScope.launch {
            try {
                // Usamos TU clase existente (AddWordRequest)
                val request = AddWordRequest(
                    word = term,
                    
                    // Como tu modelo pide "translation" pero la UI nos da "definition",
                    // se lo pasamos aquí. Es lo mismo a efectos prácticos.
                    translation = definition, 
                    
                    // Podemos poner una nota automática o dejarlo null
                    notes = "Guardado desde: $currentSongTitle", 
                    
                    type = if (isExpression) "expression" else "word",
                    example = example,
                    isRecommended = false,
                    songId = null // Si no tenemos el ID de la canción a mano, mandamos null
                )

                // Llamamos al repositorio (que ya está conectado a Retrofit)
                val response = repository.addWord(request)

                if (response.isSuccessful) {
                    // Éxito - La API de Python se encarga del resto
                } else {
                    _errorState.value = "Error al guardar: ${response.code()}"
                }

            } catch (e: Exception) {
                _errorState.value = "Error de conexión: ${e.message}"
            }
        }
    }
}