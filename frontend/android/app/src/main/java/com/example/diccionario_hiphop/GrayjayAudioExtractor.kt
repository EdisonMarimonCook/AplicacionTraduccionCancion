package com.example.diccionario_hiphop

import android.util.Log
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class AudioResult(
    val url: String,
    val title: String,
    val author: String? = null,
    val durationSeconds: Long? = null,
    val thumbnailUrl: String? = null
)

object GrayjayAudioExtractor {

    private val cache = mutableMapOf<String, Pair<AudioResult, Long>>()
    private const val CACHE_VALIDITY_MS = 6 * 60 * 60 * 1000L // 6 horas

    // 🔥 LISTA PIPED (Actualizada y Verificada)
    private val PIPED_SERVERS = listOf(
        "https://pipedapi.kavin.rocks", // A veces revive, dejémoslo primero
        "https://pipedapi.adminforge.de", // Alemania, muy estable
        "https://api.piped.mha.fi",       // Finlandia, rápido
        "https://pipedapi.system41.neocities.org", // Backup
        "https://api.piped.yt"            // El oficial (por si acaso)
    )

    // 🔥 LISTA INVIDIOUS (Backup blindado)
    private val INVIDIOUS_SERVERS = listOf(
        "https://inv.nadeko.net",         // Muy robusto hoy
        "https://invidious.nerdvpn.de",   // Alemania, buena velocidad
        "https://invidious.drgns.space",  // A veces va, a veces no
        "https://yewtu.be"                // El clásico, lento pero seguro
    )

    // Cliente "Hacker" (Anti-Censura DNS + Anti-SSL + Camuflaje)
    private val client: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        OkHttpClient.Builder()
            .dns(GoogleHttpDns()) // Bypass DNS del Router
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS) // Menos timeout para saltar rápido
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // User Agent de un Chrome real para que no nos bloqueen
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    fun getAudioWithMetadata(originalQuery: String): AudioResult? {
        // 1. Revisar Caché
        val cached = cache[originalQuery]
        if (cached != null) {
            if (System.currentTimeMillis() - cached.second < CACHE_VALIDITY_MS) {
                Log.d("GrayjayEngine", "💾 [Caché] $originalQuery")
                return cached.first
            } else cache.remove(originalQuery)
        }

        val optimizedQuery = originalQuery.replace("official audio", "lyrics")
        Log.d("GrayjayEngine", "🚀 Buscando audio para: '$optimizedQuery'")

        // 2. INTENTO PIPED (Plan A)
        val pipedResult = tryPiped(optimizedQuery)
        if (pipedResult != null) {
            cache[originalQuery] = pipedResult to System.currentTimeMillis()
            return pipedResult
        }

        // 3. INTENTO INVIDIOUS (Plan B)
        Log.d("GrayjayEngine", "⚠️ Piped falló, activando Plan B (Invidious)...")
        val invidiousResult = tryInvidious(optimizedQuery)
        if (invidiousResult != null) {
            cache[originalQuery] = invidiousResult to System.currentTimeMillis()
            return invidiousResult
        }

        Log.e("GrayjayEngine", "💀 Fallaron Piped y Invidious. Rendición.")
        return null
    }

    // --- LÓGICA PIPED ---
    private fun tryPiped(query: String): AudioResult? {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        
        for (baseUrl in PIPED_SERVERS) {
            try {
                // Search
                val searchUrl = "$baseUrl/search?q=$encodedQuery&filter=music_songs"
                val searchReq = Request.Builder().url(searchUrl).header("User-Agent", USER_AGENT).build()
                val searchResp = client.newCall(searchReq).execute()
                
                if (!searchResp.isSuccessful) {
                    Log.w("GrayjayEngine", "🔸 Piped Search Error $baseUrl: ${searchResp.code}")
                    searchResp.close(); continue
                }
                
                val body = searchResp.body?.string() ?: continue
                if (!body.trim().startsWith("{")) continue

                val json = JSONObject(body)
                val items = json.optJSONArray("items")
                if (items == null || items.length() == 0) continue

                val videoId = extractVideoId(items.getJSONObject(0).optString("url")) ?: continue
                val title = items.getJSONObject(0).optString("title")

                // Stream
                val streamUrl = "$baseUrl/streams/$videoId"
                val streamReq = Request.Builder().url(streamUrl).header("User-Agent", USER_AGENT).build()
                val streamResp = client.newCall(streamReq).execute()
                
                if (!streamResp.isSuccessful) {
                    streamResp.close(); continue
                }

                val streamBody = streamResp.body?.string() ?: continue
                val streamJson = JSONObject(streamBody)
                val audioStreams = streamJson.optJSONArray("audioStreams") ?: continue
                
                // Buscar m4a/mp4
                for (i in 0 until audioStreams.length()) {
                    val s = audioStreams.getJSONObject(i)
                    if (s.optString("mimeType").contains("mp4") || s.optString("mimeType").contains("m4a")) {
                        Log.d("GrayjayEngine", "🎉 Piped Éxito en $baseUrl")
                        return AudioResult(s.getString("url"), title)
                    }
                }
            } catch (e: Exception) {
                Log.w("GrayjayEngine", "🔸 Falló Piped $baseUrl: ${e.message}")
            }
        }
        return null
    }

    // --- LÓGICA INVIDIOUS (NUEVA) ---
    private fun tryInvidious(query: String): AudioResult? {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

        for (baseUrl in INVIDIOUS_SERVERS) {
            try {
                // Search API: /api/v1/search?q=...&type=video
                val searchUrl = "$baseUrl/api/v1/search?q=$encodedQuery&type=video"
                val request = Request.Builder().url(searchUrl).header("User-Agent", USER_AGENT).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.w("GrayjayEngine", "🔹 Invidious Search Error $baseUrl: ${response.code}")
                    response.close(); continue
                }

                val body = response.body?.string() ?: continue
                if (!body.trim().startsWith("[")) continue // Invidious devuelve Array

                val items = JSONArray(body)
                if (items.length() == 0) continue
                
                val firstItem = items.getJSONObject(0)
                val videoId = firstItem.optString("videoId")
                val title = firstItem.optString("title")

                // Video Info API: /api/v1/videos/{videoId}
                val videoUrl = "$baseUrl/api/v1/videos/$videoId"
                val videoReq = Request.Builder().url(videoUrl).header("User-Agent", USER_AGENT).build()
                val videoResp = client.newCall(videoReq).execute()

                if (!videoResp.isSuccessful) {
                    videoResp.close(); continue
                }

                val videoBody = videoResp.body?.string() ?: continue
                val videoJson = JSONObject(videoBody)
                
                // Invidious usa 'adaptiveFormats' o 'formatStreams'
                val formats = videoJson.optJSONArray("adaptiveFormats") 
                    ?: videoJson.optJSONArray("formatStreams") ?: continue

                for (i in 0 until formats.length()) {
                    val f = formats.getJSONObject(i)
                    // Buscamos audio/mp4 o audio/webm
                    val type = f.optString("type")
                    val container = f.optString("container")
                    
                    // Prioridad mp4/m4a
                    if (type.contains("audio") && (container.contains("m4a") || container.contains("mp4"))) {
                        Log.d("GrayjayEngine", "🎉 Invidious Éxito en $baseUrl")
                        return AudioResult(f.getString("url"), title)
                    }
                }
            } catch (e: Exception) {
                Log.w("GrayjayEngine", "🔹 Falló Invidious $baseUrl: ${e.message}")
            }
        }
        return null
    }

    fun getAudioStreamUrl(query: String): String? = getAudioWithMetadata(query)?.url

    private fun extractVideoId(url: String?): String? {
        if (url == null) return null
        return if (url.contains("v=")) url.substringAfter("v=") else url.substringAfterLast("/")
    }

    // --- GOOGLE DNS (Tu salvador) ---
    class GoogleHttpDns : Dns {
        private val dnsClient = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).build()
        override fun lookup(hostname: String): List<InetAddress> {
            try {
                // Intentamos Google DNS via HTTPS
                val url = "https://dns.google/resolve?name=$hostname&type=A".toHttpUrl()
                val req = Request.Builder().url(url).header("Accept", "application/json").build()
                val resp = dnsClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    if (json.has("Answer")) {
                        val answers = json.getJSONArray("Answer")
                        val list = mutableListOf<InetAddress>()
                        for (i in 0 until answers.length()) {
                            val item = answers.getJSONObject(i)
                            if (item.optInt("type") == 1) { // A record
                                list.add(InetAddress.getByName(item.optString("data")))
                            }
                        }
                        if (list.isNotEmpty()) {
                            Log.d("GrayjayDNS", "✅ DNS OK: $hostname -> ${list[0]}")
                            return list
                        }
                    }
                }
            } catch (e: Exception) { /* Ignorar y usar fallback */ }
            return Dns.SYSTEM.lookup(hostname)
        }
    }
}