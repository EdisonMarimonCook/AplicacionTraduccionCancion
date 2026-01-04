package com.example.diccionario_hiphop

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Mantenemos tu clase de datos para no romper el ViewModel
data class AudioResult(
    val url: String,
    val title: String,
    val author: String? = null,
    val durationSeconds: Long? = null
)

object GrayjayAudioExtractor {

    // 🔥 Función helper para normalizar búsqueda
    private fun normalizeQuery(query: String): String {
        val normalized = query
            .replace(Regex("""\s*-\s*Anime Size""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*TV Size""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*Remake Ver\.?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\(Official.*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\(Lyric.*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\(Audio\)""", RegexOption.IGNORE_CASE), "")
            .trim()
            .replace(Regex("""\s+"""), " ") // Espacios múltiples
        
        Log.d("LocalExtractor", "🔄 Query normalizado: '$query' → '$normalized'")
        return normalized
    }

    // Esta función se debe llamar desde una Corrutina (Dispatchers.IO)
    suspend fun getAudioWithMetadata(originalQuery: String): AudioResult? {
        return withContext(Dispatchers.IO) {
            // 🔥 Normalizar query antes de buscar
            val cleanQuery = normalizeQuery(originalQuery)
            val query = "$cleanQuery official audio"  // Cambiado: "official audio" más específico
            Log.d("LocalExtractor", "☢️ Iniciando yt-dlp local para: $query")

            try {
                // 1. Configuramos la petición al binario interno
                // "ytsearch1:" busca en YouTube y coge el primer resultado
                val request = YoutubeDLRequest("ytsearch1:$query")
                
                // "-f bestaudio[ext=m4a]" = Mejor audio en M4A (nativo Android)
                request.addOption("-f", "bestaudio[ext=m4a]/bestaudio")
                
                // "-g" = Get URL (No descargar el archivo, solo dame el link)
                request.addOption("-g")
                
                // "--get-title" = Danos también el título para confirmar
                request.addOption("--get-title")

                // 2. EJECUTAR (Esto tarda unos 2-4 segundos, usa CPU del móvil)
                val response = YoutubeDL.getInstance().execute(request)
                
                // 3. Procesar respuesta
                // La salida suele ser: Título \n URL
                val lines = response.out.trim().lines()
                
                // Buscamos cuál línea parece una URL
                val url = lines.find { it.startsWith("http") }
                val title = lines.find { !it.startsWith("http") } ?: "Audio Track"

                if (!url.isNullOrEmpty()) {
                    Log.d("LocalExtractor", "🎉 URL encontrada: $url")
                    return@withContext AudioResult(url, title)
                } else {
                    Log.e("LocalExtractor", "⚠️ Salida vacía o irreconocible: ${response.out}")
                    return@withContext null
                }

            } catch (e: Exception) {
                // Si falla (ej. timeout, o sin internet), capturamos aquí
                Log.e("LocalExtractor", "❌ Error en motor local: ${e.message}")
                return@withContext null
            }
        }
    }

    // Método helper por si lo llamas directamente
    suspend fun getAudioStreamUrl(query: String): String? = getAudioWithMetadata(query)?.url
}