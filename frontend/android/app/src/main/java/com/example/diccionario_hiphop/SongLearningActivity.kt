package com.example.diccionario_hiphop

import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
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
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.IOException

class SongLearningActivity : AppCompatActivity() {

    // --- VISTAS (UI) ---
    private lateinit var backButton: ImageButton
    private lateinit var favoriteButton: ImageButton
    private lateinit var headerTitle: TextView
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var songLanguage: TextView
    private lateinit var songLevel: TextView
    private lateinit var difficultyBar: ProgressBar
    private lateinit var albumCover: ImageView

    // Reproductor
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevFragment: Button
    private lateinit var btnNextFragment: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvDuration: TextView

    // Contenido y Carga
    private lateinit var lyricsContainer: LinearLayout
    private lateinit var loadingProgress: ProgressBar

    // Botones de Acción (Footer)
    private lateinit var btnAddToDict: Button
    private lateinit var btnCreateCard: Button
    private lateinit var btnFavAction: Button
    private lateinit var btnStats: Button

    // --- DATOS Y ESTADO ---
    private var currentAnalysis: AnalysisResponse? = null
    private var mediaPlayer: MediaPlayer? = null

    // Variable para controlar si la música suena (var, no val)
    private var isMusicPlaying = false

    // Datos de la canción recibidos
    private var songId: String = ""
    private var previewUrl: String? = null
    private var spotifyUrl: String? = null
    private var hasPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_learning)

        // 1. Recoger datos del Intent (desde la lista)
        songId = intent.getStringExtra("song_id") ?: ""
        val titleText = intent.getStringExtra("song_title") ?: "Unknown"
        val artistText = intent.getStringExtra("song_artist") ?: "Unknown"
        val imageUrl = intent.getStringExtra("song_image")

        // Datos de audio
        previewUrl = intent.getStringExtra("song_preview")
        spotifyUrl = intent.getStringExtra("song_spotify_url")
        hasPreview = intent.getBooleanExtra("song_has_preview", false)

        initializeViews()

        // 2. Pintar info básica
        headerTitle.text = titleText
        songTitle.text = titleText
        songArtist.text = artistText

        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(albumCover)
        }

        setupClickListeners()
        setupAudioLogic() // Decide si usar MediaPlayer o abrir Spotify

        // 3. Cargar la letra analizada desde el Backend
        loadSongAnalysis(titleText, artistText)
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        headerTitle = findViewById(R.id.headerTitle)
        songTitle = findViewById(R.id.songTitle)
        songArtist = findViewById(R.id.songArtist)
        songLanguage = findViewById(R.id.songLanguage)
        songLevel = findViewById(R.id.songLevel)
        difficultyBar = findViewById(R.id.difficultyBar)
        albumCover = findViewById(R.id.albumCover)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrevFragment = findViewById(R.id.btnPrevFragment)
        btnNextFragment = findViewById(R.id.btnNextFragment)
        seekBar = findViewById(R.id.seekBar)
        tvDuration = findViewById(R.id.tvDuration)
        lyricsContainer = findViewById(R.id.lyricsContainer)
        loadingProgress = findViewById(R.id.loadingProgress)
        btnAddToDict = findViewById(R.id.btnAddToDict)
        btnCreateCard = findViewById(R.id.btnCreateCard)
        btnFavAction = findViewById(R.id.btnFavAction)
        btnStats = findViewById(R.id.btnStats)
    }

    // --- LÓGICA DE AUDIO INTELIGENTE ---
    private fun setupAudioLogic() {
        if (hasPreview && !previewUrl.isNullOrEmpty()) {
            // Opción A: Reproductor Nativo (Loop de 30s)
            setupMediaPlayer()
            tvDuration.text = "Preview (Loop)"
        } else if (!spotifyUrl.isNullOrEmpty()) {
            // Opción B: Abrir en Spotify (App Externa)
            btnPlayPause.setImageResource(android.R.drawable.ic_menu_search)
            tvDuration.text = "Abrir Spotify"
            btnPlayPause.setOnClickListener { openSpotify(spotifyUrl!!) }
        } else {
            // Opción C: Sin audio
            btnPlayPause.isEnabled = false
            btnPlayPause.alpha = 0.5f
            tvDuration.text = "No Audio"
        }
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(previewUrl)
                isLooping = true // Bucle para estudiar
                prepareAsync()
                setOnPreparedListener { btnPlayPause.isEnabled = true }
            } catch (e: Exception) { e.printStackTrace() }
        }

        btnPlayPause.setOnClickListener { toggleAudio() }
    }

    private fun toggleAudio() {
        mediaPlayer?.let { player ->
            if (isMusicPlaying) player.pause() else player.start()
            isMusicPlaying = !isMusicPlaying

            val iconRes = if (isMusicPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            btnPlayPause.setImageResource(iconRes)
        }
    }

    private fun openSpotify(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.setPackage("com.spotify.music") // Intentar abrir App oficial
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Navegador web
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }
    }

    // --- CARGA DE DATOS Y UI ---

    private fun setupClickListeners() {
        backButton.setOnClickListener { finish() }
        favoriteButton.setOnClickListener { toggleFav() }
        btnFavAction.setOnClickListener { toggleFav() }

        btnAddToDict.setOnClickListener { Toast.makeText(this, "Toca una frase para guardar", Toast.LENGTH_SHORT).show() }
        // Botones WIP (Work In Progress)
        btnPrevFragment.setOnClickListener { Toast.makeText(this, "Anterior (WIP)", Toast.LENGTH_SHORT).show() }
        btnNextFragment.setOnClickListener { Toast.makeText(this, "Siguiente (WIP)", Toast.LENGTH_SHORT).show() }
    }

    private fun toggleFav() {
        Toast.makeText(this, "♥ Favorito guardado", Toast.LENGTH_SHORT).show()
    }

    private fun loadSongAnalysis(title: String, artist: String) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val api = RetrofitService.getInstance(this@SongLearningActivity)
                // Llamada al endpoint /analyze
                val response = api.analyzeLyrics(title, artist, "B1")

                if (response.isSuccessful && response.body() != null) {
                    displaySongData(response.body()!!)
                } else {
                    Toast.makeText(this@SongLearningActivity, "Error analizando letra", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SongLearningActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally { showLoading(false) }
        }
    }

    private fun displaySongData(analysis: AnalysisResponse) {
        currentAnalysis = analysis
        songLanguage.text = "🇬🇧 ${analysis.language.uppercase()}"
        songLevel.text = "Nivel: ${analysis.estimatedSongLevel}"

        val diff = when(analysis.estimatedSongLevel) {
            "A1" -> 20; "A2" -> 40; "B1" -> 60; "B2" -> 80; else -> 100
        }
        difficultyBar.progress = diff

        // Pintar la letra línea por línea
        lyricsContainer.removeAllViews()
        analysis.analyzedLyrics.forEach { line ->
            val lineView = TextView(this).apply {
                text = createColoredText(line)
                movementMethod = LinkMovementMethod.getInstance()
                textSize = 18f
                setTextColor(Color.DKGRAY)
                setPadding(0, 12, 0, 12)
                setOnClickListener { onLineClick(line) }
            }
            lyricsContainer.addView(lineView)
        }
    }

    // Pinta las palabras difíciles con colores
    private fun createColoredText(line: AnalyzedLine): SpannableString {
        val spannable = SpannableString(line.original)
        var currentPosition = 0
        line.highlightedWords.forEach { word ->
            val startIndex = line.original.indexOf(word.word, currentPosition)
            if (startIndex != -1) {
                val endIndex = startIndex + word.word.length
                val color = getColorForLevel(word.level)
                spannable.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(BackgroundColorSpan(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                currentPosition = endIndex
            }
        }
        return spannable
    }

    private fun onLineClick(line: AnalyzedLine) {
        if (line.highlightedWords.isNotEmpty()) {
            showWordDialog(line.highlightedWords[0], line.original)
        } else {
            Toast.makeText(this, "Frase sin palabras complejas", Toast.LENGTH_SHORT).show()
        }
    }

    // --- DIÁLOGO DE GUARDADO ---
    private fun showWordDialog(word: HighlightedWord, context: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_word, null)
        val etTranslation = dialogView.findViewById<TextInputEditText>(R.id.etTranslation)
        val etContext = dialogView.findViewById<TextInputEditText>(R.id.etContext)

        etTranslation.setText(word.translation)
        etContext.setText(context)

        AlertDialog.Builder(this)
            .setTitle("Guardar: ${word.word}")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val finalTranslation = etTranslation.text.toString()
                val finalContext = etContext.text.toString()
                saveWordToApi(word.word, finalTranslation, finalContext)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveWordToApi(word: String, translation: String, context: String) {
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@SongLearningActivity)
                val request = AddWordRequest(
                    word = word,
                    translation = translation,
                    context = context,
                    sourceSongId = songId,
                    sourceSongTitle = songTitle.text.toString(),
                    sourceArtist = songArtist.text.toString(),
                    language = "en"
                )
                val response = api.addWord(request)
                if (response.isSuccessful) Toast.makeText(this@SongLearningActivity, "✅ Guardado", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this@SongLearningActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SongLearningActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getColorForLevel(level: String): Int {
        return when (level) {
            "A1" -> Color.parseColor("#4CAF50") // Verde
            "A2" -> Color.parseColor("#8BC34A")
            "B1" -> Color.parseColor("#FFC107") // Amarillo
            "B2" -> Color.parseColor("#FF9800") // Naranja
            "C1" -> Color.parseColor("#F44336") // Rojo
            else -> Color.BLACK
        }
    }

    private fun showLoading(show: Boolean) {
        loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
        lyricsContainer.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}