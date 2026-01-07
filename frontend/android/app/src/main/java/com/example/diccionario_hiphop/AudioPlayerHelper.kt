package com.example.diccionario_hiphop

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.diccionario_hiphop.utils.AudioFragmentCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/**
 * 🔊 Helper para reproducir audio TTS y fragmentos de canciones
 * Lógica mejorada con cache y timeout:
 * 1. Verificar cache de fragmentos
 * 2. Si no hay cache, intentar extraer con timeout de 5s
 * 3. Fallback automático a TTS si falla o tarda mucho
 */
object AudioPlayerHelper {
    
    private var mediaPlayer: MediaPlayer? = null
    private const val TAG = "AudioPlayerHelper"
    private const val FRAGMENT_TIMEOUT_MS = 10000L // 10 segundos timeout
    
    /**
     * Reproduce audio con estrategia inteligente:
     * 1. Si hay fragmento cacheado → usar fragmento
     * 2. Si no hay cache → intentar extraer (timeout 5s)
     * 3. Si falla o tarda → fallback a TTS
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
                
                // 🔊 USAR SOLO TTS (extracción de fragmentos deshabilitada)
                // Verificar conectividad
                val networkMonitor = com.example.diccionario_hiphop.utils.NetworkMonitor.getInstance(context)
                if (!networkMonitor.isConnected.value) {
                    Log.d(TAG, "📴 Sin conexión, audio TTS no disponible")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "📴 Audio no disponible sin conexión", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    onComplete?.invoke()
                    return@withContext
                }
                
                // Reproducir TTS directamente
                Log.d(TAG, "🔊 Reproduciendo TTS para: '$text' ($language)")
                playTTS(context, text, language) {
                    onComplete?.invoke()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reproduciendo audio: ${e.message}")
                onComplete?.invoke()
            }
        }
    }
    
    /**
     * Reproduce un fragmento desde URL con timestamps específicos
     * @param audioUrl URL del audio completo
     * @param startSeconds Segundo de inicio del fragmento
     * @param endSeconds Segundo de fin del fragmento
     * @return true si se reprodujo correctamente, false si hubo error
     */
    private fun playFragmentUrl(
        audioUrl: String, 
        startSeconds: Float, 
        endSeconds: Float,
        onComplete: (() -> Unit)?
    ): Boolean {
        return try {
            Log.d(TAG, "🎵 Configurando reproducción: ${startSeconds}s - ${endSeconds}s")
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                
                setOnPreparedListener { mp ->
                    // Saltar al timestamp de inicio
                    val startMs = (startSeconds * 1000).toInt()
                    mp.seekTo(startMs)
                    mp.start()
                    
                    Log.d(TAG, "▶️ Reproduciendo fragmento desde ${startSeconds}s")
                    
                    // Programar detención en el timestamp de fin
                    val durationMs = ((endSeconds - startSeconds) * 1000).toLong()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "⏹️ Fragmento completado en ${endSeconds}s")
                        stopPlayer()
                        onComplete?.invoke()
                    }, durationMs)
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "❌ Error MediaPlayer: what=$what, extra=$extra")
                    stopPlayer()
                    false  // Retornar false para intentar TTS
                }
                
                prepareAsync()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reproduciendo fragmento: ${e.message}")
            false
        }
    }
    
    /**
     * Extrae fragmento de audio usando yt-dlp
     * Acepta tanto URLs directas como búsquedas ytsearch:
     */
    private suspend fun extractAudioFragment(
        youtubeUrl: String,
        startSeconds: Float,
        endSeconds: Float
    ): String? {
        return try {
            Log.d(TAG, "🔍 Extrayendo audio: $youtubeUrl")
            Log.d(TAG, "⏱️ Fragmento: ${startSeconds}s - ${endSeconds}s")
            
            // Si es ytsearch:, extraer directamente
            // Si es URL completa, también funciona
            val audioUrl = GrayjayAudioExtractor.getAudioFragment(youtubeUrl, startSeconds, endSeconds)
            
            if (audioUrl != null) {
                Log.d(TAG, "✅ Audio URL obtenida (${audioUrl.length} chars)")
                Log.d(TAG, "🔗 URL: ${audioUrl.take(150)}...")
            } else {
                Log.e(TAG, "❌ No se pudo obtener audio URL")
            }
            
            audioUrl
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extrayendo fragmento: ${e.message}", e)
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
