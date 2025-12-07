package com.example.diccionario_hiphop

<<<<<<< HEAD
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.buildSpannedString
import androidx.lifecycle.lifecycleScope
=======
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
>>>>>>> feature/lyrics-translation
import kotlinx.coroutines.launch

class SongLearningActivity : AppCompatActivity() {

<<<<<<< HEAD
    // Views
    private lateinit var backButton: ImageButton
    private lateinit var favoriteButton: ImageButton
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var songLanguage: TextView
    private lateinit var songLevel: TextView
    private lateinit var difficultyBar: ProgressBar
    private lateinit var albumCover: ImageView
    private lateinit var lyricsContainer: LinearLayout
    private lateinit var addToDictButton: Button
    private lateinit var createCardButton: Button
    private lateinit var statsButton: Button

    // Data
    private lateinit var songRepository: SongRepository
    private var currentAnalysis: AnalysisResponse? = null
=======
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
>>>>>>> feature/lyrics-translation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_learning)

<<<<<<< HEAD
        initializeViews()
        setupClickListeners()
        loadSongData()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        songTitle = findViewById(R.id.songTitle)
        songArtist = findViewById(R.id.songArtist)
        songLanguage = findViewById(R.id.songLanguage)
        songLevel = findViewById(R.id.songLevel)
        difficultyBar = findViewById(R.id.difficultyBar)
        albumCover = findViewById(R.id.albumCover)
        lyricsContainer = findViewById(R.id.lyricsContainer)
        addToDictButton = findViewById(R.id.addToDictButton)
        createCardButton = findViewById(R.id.createCardButton)
        statsButton = findViewById(R.id.statsButton)

        songRepository = SongRepository() // Aquí pasarías el token si lo tienes
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener { finish() }

        favoriteButton.setOnClickListener { toggleFavorite() }

        addToDictButton.setOnClickListener {
            // Implementar añadir al diccionario
            showWordSelectionDialog()
        }

        createCardButton.setOnClickListener {
            // Implementar creación de flashcards
            showCreateFlashcardDialog()
        }

        statsButton.setOnClickListener {
            // Mostrar estadísticas de la canción
            showSongStatistics()
        }
    }

    private fun loadSongData() {
        val songTitleText = intent.getStringExtra("song_title") ?: "Endless Possibility"
        val songArtistText = intent.getStringExtra("song_artist") ?: "Sonic"
        val userLevel = "B1" // Esto debería venir del perfil del usuario

        lifecycleScope.launch {
            showLoading(true)

            // Cargar datos mock para probar (mientras la API está en desarrollo)
            val mockAnalysis = createMockAnalysis(songTitleText, songArtistText)
            displaySongData(mockAnalysis)

            // Cuando la API esté lista, descomenta esto:
            /*
            val analysis = songRepository.getAnalyzedLyrics(songTitleText, songArtistText, userLevel)
            if (analysis != null) {
                displaySongData(analysis)
            } else {
                showError("No se pudo cargar el análisis de la canción")
            }
            */

            showLoading(false)
        }
    }

    private fun displaySongData(analysis: AnalysisResponse) {
        currentAnalysis = analysis

        songTitle.text = analysis.title
        songArtist.text = analysis.artist
        songLanguage.text = "🇬🇧 ${analysis.language.uppercase()}"
        songLevel.text = "Nivel: ${analysis.estimated_song_level}"

        // Configurar barra de dificultad (ejemplo simplificado)
        val difficulty = when (analysis.estimated_song_level) {
            "A1" -> 20
            "A2" -> 40
            "B1" -> 60
            "B2" -> 75
            "C1" -> 90
            "C2" -> 100
            else -> 50
        }
        difficultyBar.progress = difficulty

        // Cargar imagen del álbum (usar Glide/Picasso en producción)
        // Glide.with(this).load(analysis.image_url).into(albumCover)

        // Mostrar letra analizada
        displayAnalyzedLyrics(analysis.analyzed_lyrics)
    }

    private fun displayAnalyzedLyrics(analyzedLines: List<AnalyzedLine>) {
        lyricsContainer.removeAllViews()

        analyzedLines.forEach { line ->
            val lineView = TextView(this).apply {
                text = createColoredText(line)
                movementMethod = LinkMovementMethod.getInstance()
                setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
                setOnClickListener {
                    // Click en línea completa
                    onLineClick(line)
                }
            }
            lyricsContainer.addView(lineView)
        }
    }

    private fun createColoredText(line: AnalyzedLine): SpannableString {
        val spannable = SpannableString(line.original)
        var currentPosition = 0

        line.highlighted_words.forEach { word ->
            val startIndex = line.original.indexOf(word.word, currentPosition)
            if (startIndex != -1) {
                val endIndex = startIndex + word.word.length

                // Aplicar color según el nivel
                val color = getColorForLevel(word.level)
                spannable.setSpan(
                    ForegroundColorSpan(color),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Aplicar fondo ligeramente diferente para destacar
                spannable.setSpan(
                    BackgroundColorSpan(color and 0x80FFFFFF.toInt()), // Color con alpha
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                currentPosition = endIndex
            }
        }

        return spannable
    }

    private fun getColorForLevel(level: String): Int {
        return when (level) {
            "A1" -> Color.GREEN
            "A2" -> Color.YELLOW
            "B1" -> Color.parseColor("#FF9800") // Naranja
            "B2" -> Color.parseColor("#F57C00") // Naranja oscuro
            "C1" -> Color.RED
            "C2" -> Color.parseColor("#B71C1C") // Rojo oscuro
            else -> Color.BLACK
        }
    }

    private fun onLineClick(line: AnalyzedLine) {
        // Mostrar opciones para la línea completa
        showLineOptionsDialog(line)
    }

    private fun onWordClick(word: HighlightedWord) {
        // Mostrar definición de palabra
        showWordDefinitionDialog(word)
    }

    private fun showWordDefinitionDialog(word: HighlightedWord) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(word.word)
            .setMessage("""
                Traducción: ${word.translation}
                Definición: ${word.definition}
                Nivel: ${word.level}
            """.trimIndent())
            .setPositiveButton("Añadir al diccionario") { _, _ ->
                addWordToDictionary(word)
            }
            .setNegativeButton("Cerrar", null)
            .create()
        dialog.show()
    }

    private fun addWordToDictionary(word: HighlightedWord) {
        // Implementar guardado en diccionario personal
        Toast.makeText(this, "Palabra '${word.word}' añadida al diccionario", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFavorite() {
        val isFavorite = favoriteButton.tag as? Boolean ?: false
        favoriteButton.tag = !isFavorite

        // Cambiar icono de favorito
        val iconRes = if (!isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        favoriteButton.setImageResource(iconRes)

        Toast.makeText(this, if (!isFavorite) "Añadido a favoritos" else "Removido de favoritos", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        findViewById<ProgressBar>(R.id.loadingProgress).visibility =
            if (show) View.VISIBLE else View.GONE
        lyricsContainer.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    // Extension para convertir dp a px
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // Datos mock para probar la UI
    private fun createMockAnalysis(title: String, artist: String): AnalysisResponse {
        return AnalysisResponse(
            title = title,
            artist = artist,
            user_level = "B1",
            estimated_song_level = "B1",
            language = "en",
            image_url = "",
            analyzed_lyrics = listOf(
                AnalyzedLine(
                    id = 0,
                    original = "This is my escape",
                    highlighted_words = listOf(
                        HighlightedWord(
                            word = "escape",
                            level = "A2",
                            translation = "escapada",
                            definition = "Way out or refuge from something",
                            color = "yellow"
                        )
                    ),
                    line_number = 1
                ),
                AnalyzedLine(
                    id = 1,
                    original = "I'm running through this world",
                    highlighted_words = listOf(
                        HighlightedWord(
                            word = "running",
                            level = "A1",
                            translation = "corriendo",
                            definition = "Moving fast on foot",
                            color = "green"
                        )
                    ),
                    line_number = 2
                ),
                AnalyzedLine(
                    id = 2,
                    original = "And I'm not looking back",
                    highlighted_words = listOf(
                        HighlightedWord(
                            word = "looking",
                            level = "A2",
                            translation = "mirando",
                            definition = "Directing one's gaze toward something",
                            color = "yellow"
                        )
                    ),
                    line_number = 3
                )
            ),
            word_stats = WordStats(
                total_words = 150,
                a1_words = 45,
                a2_words = 35,
                b1_words = 40,
                b2_words = 20,
                c1_words = 10,
                c2_words = 0
            ),
            status = "success"
        )
    }

    // Métodos pendientes de implementar
    private fun showWordSelectionDialog() {
        Toast.makeText(this, "Selecciona una palabra para añadir al diccionario", Toast.LENGTH_SHORT).show()
    }

    private fun showCreateFlashcardDialog() {
        Toast.makeText(this, "Crear flashcard para palabra seleccionada", Toast.LENGTH_SHORT).show()
    }

    private fun showSongStatistics() {
        Toast.makeText(this, "Mostrar estadísticas de la canción", Toast.LENGTH_SHORT).show()
    }

    private fun showLineOptionsDialog(line: AnalyzedLine) {
        Toast.makeText(this, "Opciones para línea: ${line.original}", Toast.LENGTH_SHORT).show()
=======
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
>>>>>>> feature/lyrics-translation
    }
}