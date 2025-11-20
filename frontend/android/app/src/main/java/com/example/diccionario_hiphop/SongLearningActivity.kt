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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var loadingProgress: ProgressBar

    // Data
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
        loadingProgress = findViewById(R.id.loadingProgress)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener { finish() }
        favoriteButton.setOnClickListener { toggleFavorite() }
        addToDictButton.setOnClickListener { showWordSelectionDialog() }
        createCardButton.setOnClickListener { showCreateFlashcardDialog() }
        statsButton.setOnClickListener { showSongStatistics() }
    }

    private fun loadSongData() {
        val titleText = intent.getStringExtra("song_title") ?: "Unknown Title"
        val artistText = intent.getStringExtra("song_artist") ?: "Unknown Artist"

        lifecycleScope.launch {
            showLoading(true)

            // ⚠️ IMPORTANTE: Aquí usamos datos MOCK para probar la UI.
            // Cuando tu backend esté listo, cambia esta línea por la llamada real a Retrofit:
            // val response = RetrofitService.getInstance(this@SongLearningActivity).analyzeLyrics(titleText, artistText, "B1")

            val mockAnalysis = createMockAnalysis(titleText, artistText)
            displaySongData(mockAnalysis)

            showLoading(false)
        }
    }

    private fun displaySongData(analysis: AnalysisResponse) {
        currentAnalysis = analysis

        // ✅ CORRECCIÓN: Accedemos directamente a las propiedades en la raíz del objeto AnalysisResponse
        songTitle.text = analysis.title
        songArtist.text = analysis.artist
        songLanguage.text = "🇬🇧 ${analysis.language.uppercase()}"
        songLevel.text = "Nivel: ${analysis.estimatedSongLevel}"

        // Configurar barra de dificultad
        val difficulty = when (analysis.estimatedSongLevel) {
            "A1" -> 20; "A2" -> 40; "B1" -> 60
            "B2" -> 75; "C1" -> 90; "C2" -> 100
            else -> 50
        }
        difficultyBar.progress = difficulty

        // Mostrar letra analizada
        displayAnalyzedLyrics(analysis.analyzedLyrics)
    }

    private fun displayAnalyzedLyrics(analyzedLines: List<AnalyzedLine>) {
        lyricsContainer.removeAllViews()

        analyzedLines.forEach { line ->
            val lineView = TextView(this).apply {
                text = createColoredText(line)
                movementMethod = LinkMovementMethod.getInstance() // Habilita clics en spans si los hubiera
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(0, 16, 0, 16)
                setOnClickListener { onLineClick(line) }
            }
            lyricsContainer.addView(lineView)
        }
    }

    private fun createColoredText(line: AnalyzedLine): SpannableString {
        val spannable = SpannableString(line.original)
        var currentPosition = 0

        // Iteramos sobre las palabras destacadas de la línea
        line.highlightedWords.forEach { word ->
            val startIndex = line.original.indexOf(word.word, currentPosition)
            if (startIndex != -1) {
                val endIndex = startIndex + word.word.length

                val color = getColorForLevel(word.level)

                // 1. Color del texto (letra)
                spannable.setSpan(
                    ForegroundColorSpan(color),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // 2. Color de fondo (transparente para resaltar suavemente)
                spannable.setSpan(
                    BackgroundColorSpan(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))),
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
            "A1" -> Color.parseColor("#4CAF50") // Verde
            "A2" -> Color.parseColor("#8BC34A") // Verde Claro
            "B1" -> Color.parseColor("#FFC107") // Ambar
            "B2" -> Color.parseColor("#FF9800") // Naranja
            "C1" -> Color.parseColor("#F44336") // Rojo
            "C2" -> Color.parseColor("#B71C1C") // Rojo Oscuro
            else -> Color.BLACK
        }
    }

    private fun onLineClick(line: AnalyzedLine) {
        // Si la línea tiene palabras difíciles, mostramos la primera al hacer clic en la línea
        if (line.highlightedWords.isNotEmpty()) {
            showWordDefinitionDialog(line.highlightedWords[0])
        } else {
            // Feedback simple si no hay palabras destacadas
            Toast.makeText(this, line.original, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showWordDefinitionDialog(word: HighlightedWord) {
        AlertDialog.Builder(this)
            .setTitle(word.word)
            .setMessage("Nivel: ${word.level}\nTraducción: ${word.translation}\n\n${word.definition}")
            .setPositiveButton("Guardar en Diccionario") { _, _ -> addWordToDictionary(word) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun addWordToDictionary(word: HighlightedWord) {
        Toast.makeText(this, "${word.word} guardada", Toast.LENGTH_SHORT).show()
        // Aquí llamarías a tu ViewModel o Repository para guardar en la BD o API
    }

    private fun toggleFavorite() {
        Toast.makeText(this, "Favorito clickeado", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
        lyricsContainer.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showWordSelectionDialog() {
        Toast.makeText(this, "Función: Selección Manual", Toast.LENGTH_SHORT).show()
    }

    private fun showCreateFlashcardDialog() {
        Toast.makeText(this, "Función: Crear Flashcard", Toast.LENGTH_SHORT).show()
    }

    private fun showSongStatistics() {
        Toast.makeText(this, "Función: Estadísticas", Toast.LENGTH_SHORT).show()
    }

    // ✅ MOCK DATA: Simula la respuesta del servidor con la estructura correcta
    private fun createMockAnalysis(title: String, artist: String): AnalysisResponse {
        return AnalysisResponse(
            title = title,
            artist = artist,
            userLevel = "B1",
            estimatedSongLevel = "B1",
            language = "en",
            imageUrl = null,
            analyzedLyrics = listOf(
                AnalyzedLine(0, "This is my escape", listOf(HighlightedWord("escape", "A2", "escapada", "Way out", null)), 1),
                AnalyzedLine(1, "I'm running through this world", listOf(HighlightedWord("running", "A1", "corriendo", "Moving fast", null)), 2)
            ),
            wordStats = WordStats(150, 45, 35, 40, 20, 10, 0),
            status = "success"
        )
    }
}