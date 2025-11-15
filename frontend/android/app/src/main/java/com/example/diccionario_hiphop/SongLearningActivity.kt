package com.example.diccionario_hiphop

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
import kotlinx.coroutines.launch

class SongLearningActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_learning)

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
    }
}