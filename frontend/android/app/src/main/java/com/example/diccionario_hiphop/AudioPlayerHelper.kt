package com.example.diccionario_hiphop

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🔊 Helper para reproducir audio TTS y fragmentos de canciones
 * Lógica:
 * 1. Si hay song_youtube_url + timestamps → Extraer fragmento con yt-dlp
 * 2. Si no → Usar TTS del backend
 */
object AudioPlayerHelper {
    
    private var mediaPlayer: MediaPlayer? = null
    private const val TAG = "AudioPlayerHelper"
    
    /**
     * Reproduce audio con la siguiente prioridad:
     * 1. Fragmento de YouTube (si tiene URL + timestamps)
     * 2. TTS del backend (fallback)
     * 
     * @param context Contexto de Android
     * @param text Palabra o expresión a pronunciar
     * @param language Código del idioma (ja, ko, zh, es, etc.)
     * @param songYoutubeUrl URL de YouTube (opcional)
     * @param timestampStart Segundo de inicio (opcional)
     * @param timestampEnd Segundo de fin (opcional)
     * @param onComplete Callback cuando termina la reproducción
     */
    suspend fun playAudio(
        context: Context,
        text: String,
        language: String,
        songYoutubeUrl: String? = null,
        timestampStart: Float? = null,
        timestampEnd: Float? = null,
        onComplete: (() -> Unit)? = null
    ) {
        withContext(Dispatchers.Main) {
            try {
                // Liberar MediaPlayer anterior si existe
                releasePlayer()
                
                // 1️⃣ Intentar fragmento de YouTube si hay datos
                if (!songYoutubeUrl.isNullOrEmpty() && timestampStart != null && timestampEnd != null) {
                    Log.d(TAG, "🎯 Intentando extraer fragmento de YouTube...")
                    val fragmentUrl = extractAudioFragment(songYoutubeUrl, timestampStart, timestampEnd)
                    
                    if (fragmentUrl != null) {
                        playAudioFromUrl(fragmentUrl, timestampStart, timestampEnd, onComplete)
                        return@withContext
                    } else {
                        Log.w(TAG, "⚠️ Fragmento no disponible, usando TTS...")
                    }
                }
                
                // 2️⃣ Fallback: TTS del backend
                Log.d(TAG, "🔊 Usando TTS para: '$text' ($language)")
                playTTS(context, text, language, onComplete)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reproduciendo audio: ${e.message}")
                onComplete?.invoke()
            }
        }
    }
    
    /**
     * Extrae fragmento de audio usando yt-dlp
     */
    private suspend fun extractAudioFragment(
        youtubeUrl: String,
        startSeconds: Float,
        endSeconds: Float
    ): String? {
        return try {
            GrayjayAudioExtractor.getAudioFragment(youtubeUrl, startSeconds, endSeconds)
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo fragmento: ${e.message}")
            null
        }
    }
    
    /**
     * Reproduce audio desde URL con timestamps
     */
    private fun playAudioFromUrl(
        url: String,
        startSeconds: Float,
        endSeconds: Float,
        onComplete: (() -> Unit)?
    ) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                
                setOnPreparedListener { mp ->
                    // Saltar al segundo exacto
                    mp.seekTo((startSeconds * 1000).toInt())
                    mp.start()
                    Log.d(TAG, "▶️ Reproduciendo fragmento: ${startSeconds}s - ${endSeconds}s")
                    
                    // Programar detención en el timestamp de fin
                    val duration = ((endSeconds - startSeconds) * 1000).toLong()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        stopPlayer()
                        onComplete?.invoke()
                    }, duration)
                }
                
                setOnCompletionListener {
                    stopPlayer()
                    onComplete?.invoke()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error MediaPlayer: what=$what, extra=$extra")
                    stopPlayer()
                    onComplete?.invoke()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando MediaPlayer: ${e.message}")
            onComplete?.invoke()
        }
    }
    
    /**
     * Reproduce TTS del backend
     */
    private fun playTTS(
        context: Context,
        text: String,
        language: String,
        onComplete: (() -> Unit)?
    ) {
        try {
            val baseUrl = RetrofitService.BASE_URL.removeSuffix("/")
            val ttsUrl = "$baseUrl/api/v1/audio/tts?text=${java.net.URLEncoder.encode(text, "UTF-8")}&language=$language"
            
            Log.d(TAG, "🔊 TTS URL: $ttsUrl")
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(ttsUrl)
                prepareAsync()
                
                setOnPreparedListener { mp ->
                    mp.start()
                    Log.d(TAG, "▶️ Reproduciendo TTS")
                }
                
                setOnCompletionListener {
                    stopPlayer()
                    onComplete?.invoke()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error TTS: what=$what, extra=$extra")
                    stopPlayer()
                    onComplete?.invoke()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reproduciendo TTS: ${e.message}")
            onComplete?.invoke()
        }
    }
    
    /**
     * Detiene la reproducción actual
     */
    fun stopPlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo reproductor: ${e.message}")
        }
    }
    
    /**
     * Libera recursos del MediaPlayer
     */
    fun releasePlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando reproductor: ${e.message}")
        }
    }
}
