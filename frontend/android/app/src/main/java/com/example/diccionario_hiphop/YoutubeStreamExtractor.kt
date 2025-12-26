package com.example.diccionario_hiphop

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchEngine
import java.util.concurrent.TimeUnit

object YoutubeStreamExtractor {

    private var isInitialized = false

    private fun init(context: Context) {
        if (isInitialized) return
        NewPipe.init(MyDownloader(), Localization.DEFAULT, null)
        isInitialized = true
    }

    suspend fun getStreamUrl(context: Context, query: String): String? = withContext(Dispatchers.IO) {
        try {
            init(context)
            // 1. Buscar video
            val searchUrl = ServiceList.YouTube.searchEngine.getQueryUrl(
                query, listOf(SearchEngine.Filter.STREAM), null
            )
            val searchExtractor = ServiceList.YouTube.getSearchExtractor(searchUrl)
            searchExtractor.fetchPage()
            
            val items = searchExtractor.initialPage.items
            if (items.isEmpty()) return@withContext null

            // 2. Sacar detalles del primer resultado
            val videoUrl = items[0].url
            val streamExtractor = ServiceList.YouTube.getStreamExtractor(videoUrl)
            streamExtractor.fetchPage()

            // 3. Buscar audio m4a (mejor compatibilidad) o el que haya
            val audioStreams = streamExtractor.audioStreams
            val bestStream = audioStreams.find { it.format.toString().contains("m4a") } 
                             ?: audioStreams.lastOrNull()

            return@withContext bestStream?.url
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    // Cliente HTTP camuflado de navegador
    private class MyDownloader : Downloader() {
        private val client = okhttp3.OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
            val okRequestBuilder = okhttp3.Request.Builder()
                .url(request.url())
                .method(request.httpMethod(), null)
            
            request.headers().forEach { (k, v) -> okRequestBuilder.addHeader(k, v.first()) }
            okRequestBuilder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            
            val response = client.newCall(okRequestBuilder.build()).execute()
            return Response(response.code, response.message, response.headers.toMultimap(), response.body?.string() ?: "", null)
        }
    }
}