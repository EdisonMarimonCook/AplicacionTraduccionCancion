package com.example.diccionario_hiphop

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GrayjayAudioExtractor {
    
    // Timeouts cortos para saltar rápido si un server no responde
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // 🛡️ LISTA DE INSTANCIAS (Resiliencia)
    private val INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.privacy.com.de",
        "https://pipedapi.drgns.space",
        "https://piped-api.garudalinux.org",
        "https://api.piped.yt"
    )

    fun getAudioStreamUrl(query: String): String? {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

        for (baseUrl in INSTANCES) {
            try {
                Log.d("GrayjayExtractor", "🔄 Probando servidor: $baseUrl")
                
                // 1. Buscar ID del video
                val searchUrl = "$baseUrl/search?q=$encodedQuery&filter=music_songs"
                val searchReq = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:68.0) Gecko/68.0 Firefox/68.0")
                    .build()
                
                var videoId: String? = null
                
                client.newCall(searchReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val json = JSONObject(body)
                            val items = json.optJSONArray("items")
                            if (items != null && items.length() > 0) {
                                // Buscamos en los primeros resultados
                                val urlPath = items.getJSONObject(0).optString("url") // /watch?v=ID
                                if (urlPath.contains("v=")) {
                                    videoId = urlPath.split("v=")[1]
                                }
                            }
                        }
                    }
                }

                // 2. Si tenemos ID, sacamos el audio en ESTE mismo servidor
                if (videoId != null) {
                    val streamUrl = "$baseUrl/streams/$videoId"
                    val streamReq = Request.Builder().url(streamUrl).build()
                    
                    client.newCall(streamReq).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (!body.isNullOrEmpty()) {
                                val json = JSONObject(body)
                                val audioStreams = json.optJSONArray("audioStreams")
                                
                                if (audioStreams != null && audioStreams.length() > 0) {
                                    // Prioridad: M4A (Mejor para Android)
                                    for (i in 0 until audioStreams.length()) {
                                        val stream = audioStreams.getJSONObject(i)
                                        if (stream.optString("mimeType").contains("mp4")) {
                                            Log.d("GrayjayExtractor", "✅ BINGO en $baseUrl")
                                            return stream.optString("url")
                                        }
                                    }
                                    // Fallback
                                    return audioStreams.getJSONObject(0).optString("url")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GrayjayExtractor", "❌ Falló $baseUrl, probando siguiente...")
            }
        }
        return null // Fallaron todos
    }
}