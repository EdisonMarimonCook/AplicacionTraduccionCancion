package com.example.diccionario_hiphop

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class SongLearningActivity : AppCompatActivity() {

    // UI
    private lateinit var tvLyrics: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var btnBack: ImageButton

    // Lógica
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var repository: SongRepository
    
    private var songId: String? = null
    private var songTitle: String? = null
    private var songArtist: String? = null
    private var previewUrl: String? = null
    private var coverUrl: String? = null
    private var spotifyUrl: String? = null

    // Guardamos la letra para poder repintarla
    private var originalLyricsText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_learning)

        repository = SongRepository(this)
        
        songId = intent.getStringExtra("SONG_ID")
        songTitle = intent.getStringExtra("SONG_TITLE") ?: "Desconocido"
        songArtist = intent.getStringExtra("SONG_ARTIST") ?: "Desconocido"
        previewUrl = intent.getStringExtra("PREVIEW_URL")
        coverUrl = intent.getStringExtra("COVER_URL")
        spotifyUrl = intent.getStringExtra("SPOTIFY_URL")

        initViews()
        setupPlayer()
        loadContent() // 🔥 Aquí empieza la magia corregida
    }

    private fun initViews() {
        tvLyrics = findViewById(R.id.tvLyrics)
        btnPlay = findViewById(R.id.btnPlayPause)
        progressBar = findViewById(R.id.progressBar)
        ivCover = findViewById(R.id.ivAlbumCover)
        tvTitle = findViewById(R.id.tvHeaderTitle)
        tvArtist = findViewById(R.id.tvHeaderArtist)
        btnBack = findViewById(R.id.btnBack)

        tvTitle.text = songTitle
        tvArtist.text = songArtist
        tvLyrics.movementMethod = LinkMovementMethod.getInstance()

        if (!coverUrl.isNullOrEmpty()) {
            Glide.with(this).load(coverUrl).into(ivCover)
        }

        btnPlay.setOnClickListener { togglePlay() }
        btnBack.setOnClickListener { finish() }
    }

    private fun setupPlayer() {
        if (previewUrl.isNullOrEmpty()) {
            if (!spotifyUrl.isNullOrEmpty()) {
                btnPlay.setImageResource(android.R.drawable.ic_menu_search)
                btnPlay.setOnClickListener {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl)))
                    } catch (e: Exception) {
                        Toast.makeText(this, "No se puede abrir Spotify", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                btnPlay.isEnabled = false
                btnPlay.alpha = 0.5f
            }
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(previewUrl)
                isLooping = true
                prepareAsync()
                setOnPreparedListener { btnPlay.isEnabled = true }
                setOnCompletionListener { if (!isLooping) btnPlay.setImageResource(android.R.drawable.ic_media_play) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun togglePlay() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
            } else {
                it.start()
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    // 🔥 LOGICA CORREGIDA: Obtener Letra -> Mostrar -> Analizar
    private fun loadContent() {
        progressBar.visibility = View.VISIBLE
        tvLyrics.text = "Buscando letra..."

        lifecycleScope.launch {
            try {
                // PASO 1: Obtener la letra plana primero (Rápido)
                val lyricsRes = repository.getLyrics(songTitle!!, songArtist!!)
                
                if (lyricsRes.isSuccessful && lyricsRes.body() != null) {
                    originalLyricsText = lyricsRes.body()!!.lyrics
                    
                    // Mostramos la letra inmediatamente (en negro)
                    tvLyrics.text = originalLyricsText
                    
                    // PASO 2: Enviar ESA letra a la IA para analizar (Lento)
                    requestAIAnalysis(originalLyricsText)
                } else {
                    tvLyrics.text = "Letra no encontrada en Genius."
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                tvLyrics.text = "Error de conexión: ${e.message}"
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun requestAIAnalysis(lyricsText: String) {
        // Mostramos un mini aviso de que la IA está pensando, pero mantenemos la letra visible
        Toast.makeText(this, "Analizando con IA...", Toast.LENGTH_SHORT).show()
        
        try {
            val userLevel = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userLevel", "B1") ?: "B1"
            
            // 🔥 Enviamos la letra que acabamos de descargar
            val aiResponse = repository.analyzeLyrics(songTitle!!, songArtist!!, userLevel, lyricsText)

            if (aiResponse.isSuccessful && aiResponse.body() != null) {
                // PASO 3: Pintar los colores sobre la letra existente
                applyHighlights(aiResponse.body()!!)
                Toast.makeText(this, "¡Análisis completado!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "La IA no pudo analizar la letra (Error ${aiResponse.code()})", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error conectando con IA: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            progressBar.visibility = View.GONE
        }
    }

    private fun applyHighlights(data: HighlightWordsResponse) {
        // Usamos la letra que ya tenemos guardada
        val spannable = SpannableString(originalLyricsText)
        val lowerLyrics = originalLyricsText.lowercase()

        // 1. Resaltar Palabras (Naranja)
        data.words.forEach { w -> highlightTerm(spannable, lowerLyrics, w.word, w, false) }
        
        // 2. Resaltar Expresiones (Azul/Cyan)
        data.expressions.forEach { e -> highlightTerm(spannable, lowerLyrics, e.expression, e, true) }

        tvLyrics.text = spannable
    }

    private fun highlightTerm(spannable: SpannableString, fullTextLower: String, term: String, itemData: Any, isExpression: Boolean) {
        val termLower = term.lowercase()
        var startIndex = fullTextLower.indexOf(termLower)
        
        while (startIndex >= 0) {
            val endIndex = startIndex + termLower.length
            val color = if (isExpression) ContextCompat.getColor(this, R.color.teal_200) else ContextCompat.getColor(this, R.color.purple_200)

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) { showDefinitionDialog(itemData, isExpression) }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = color
                    ds.isFakeBoldText = true
                }
            }
            spannable.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            startIndex = fullTextLower.indexOf(termLower, endIndex)
        }
    }

    private fun showDefinitionDialog(item: Any, isExpression: Boolean) {
        val word: String; val translation: String; val explanation: String; val example: String; val recommended: Boolean; val type: String

        if (isExpression) {
            val i = item as ExpressionHighlight
            word = i.expression; translation = i.translation; explanation = i.explanation; example = i.example; recommended = i.recommended; type = i.type
        } else {
            val i = item as WordHighlight
            word = i.word; translation = i.translation; explanation = i.explanation; example = i.example; recommended = i.recommended; type = i.type
        }

        AlertDialog.Builder(this)
            .setTitle(word.replaceFirstChar { it.uppercase() })
            .setMessage("🇪🇸 $translation\n\n💡 $explanation\n\n📝 Ej: \"$example\"")
            .setPositiveButton("Guardar (+)") { _, _ -> saveToDictionary(word, translation, explanation, type, example, recommended) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun saveToDictionary(word: String, translation: String, notes: String, type: String, example: String, isRecommended: Boolean) {
        val request = AddWordRequest(word, translation, notes, type, example, isRecommended, songId)
        lifecycleScope.launch {
            try {
                val response = repository.addWord(request)
                if (response.isSuccessful) Toast.makeText(this@SongLearningActivity, "✅ Guardado", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this@SongLearningActivity, "Error guardando", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this@SongLearningActivity, "Error de red", Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}