package com.example.diccionario_hiphop

import com.example.diccionario_hiphop.WordDefinition
import com.example.diccionario_hiphop.ExpressionDefinition
import com.example.diccionario_hiphop.HighlightWordsResponse
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView // IMPORTANTE: Importar CardView
import coil.load
import coil.transform.RoundedCornersTransformation
import java.util.concurrent.TimeUnit
import com.example.diccionario_hiphop.utils.WindowInsetsHelper
import android.graphics.Color
import android.graphics.PorterDuff

class SongLearningActivity : AppCompatActivity() {

    private val viewModel: SongLearningViewModel by viewModels()

    // UI Elements
    private lateinit var tvLyrics: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var ivCover: ImageView

    // 🔥 NUEVOS ELEMENTOS PARA EDICIÓN GRAMMY
    private lateinit var cvAlbumContainer: CardView
    private lateinit var tvGrammyBadge: TextView

    // 👇 Overlay de carga y mensaje
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingMessage: TextView
    // 👇 Spinner discreto IA
    private lateinit var layoutAiStatus: LinearLayout

    // 🔥 ELEMENTOS PARA LA SEEKBAR
    private lateinit var sbProgress: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var layoutPlayerControls: LinearLayout

    private var originalLyricsText: String = ""
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var wordWasAdded = false

    // 🔥 VARIABLES PARA CONTROL DE TIEMPO
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

    // Datos recibidos
    private var songTitle: String = ""
    private var songArtist: String = ""
    private var coverUrl: String? = null
    private var previewUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_learning)

        // 1. Recoger datos del Intent primero
        songTitle = intent.getStringExtra("song_title") ?: "Desconocido"
        songArtist = intent.getStringExtra("song_artist") ?: "Desconocido"
        coverUrl = intent.getStringExtra("song_image")
        previewUrl = intent.getStringExtra("song_audio")

        // 2. Inicializar Vistas
        initViews()
        setupUI()
        setupPlayer()
        setupSeekBar()

        //  Aplicar WindowInsets
        val mainLayout = findViewById<View>(R.id.mainLayout)
        WindowInsetsHelper.applySystemBarInsets(mainLayout)

        // 🏆 DETECCIÓN DE TEMA GRAMMY (LÓGICA ACTUALIZADA)
        val isGrammyTheme = intent.getBooleanExtra("is_grammy_theme", false)

        if (isGrammyTheme) {
            // 1. Fondo Dorado Radiante
            mainLayout?.background = ContextCompat.getDrawable(this, R.drawable.gradient_grammy_gold)

            // 2. Caja de Cristal para la letra
            tvLyrics.background = ContextCompat.getDrawable(this, R.drawable.bg_lyrics_panel)
            tvLyrics.setTextColor(Color.WHITE)

            // 3. ✨ VISIBILIDAD DE ETIQUETA NOMINADO ✨
            tvGrammyBadge.visibility = View.VISIBLE

            // 4. ✨ BORDE DORADO A LA CARÁTULA ✨
            // Usamos el CardView como borde poniendo el fondo dorado y añadiendo padding
            cvAlbumContainer.setCardBackgroundColor(Color.parseColor("#FFD700"))
            cvAlbumContainer.setContentPadding(6, 6, 6, 6) // Grosor del borde

            // 5. Header de Lujo
            tvTitle.setTextColor(Color.WHITE)
            // Sombra fuerte para leer sobre dorado
            tvTitle.setShadowLayer(12f, 0f, 4f, Color.parseColor("#80000000"))

            // 6. Artista con Estrella y Dorado
            tvArtist.setTextColor(Color.parseColor("#FFD700")) // Texto Dorado
            tvArtist.setShadowLayer(4f, 0f, 2f, Color.BLACK)
            tvArtist.text = "⭐ $songArtist" // Añadimos la estrella

            // Barra de estado oscura
            window.statusBarColor = Color.parseColor("#3A2C00")
        }

        // 🔥 INICIALIZAR EL MOTOR NUCLEAR
        try {
            com.yausername.youtubedl_android.YoutubeDL.getInstance().init(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("YoutubeDL", "Error fatal al iniciar motor", e)
        }

        // Inicializar vistas extra (Overlays)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage)
        layoutAiStatus = findViewById(R.id.layoutAiStatus)

        // 3. Observar ViewModel
        viewModel.lyricsState.observe(this) { lyrics ->
            originalLyricsText = lyrics
            tvLyrics.text = lyrics
        }

        viewModel.analysisState.observe(this) { analysis ->
            if (analysis != null) applyHighlights(analysis)
        }

        // CONTROL DE CARGA
        viewModel.loadingState.observe(this) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // SPINNER DISCRETO IA
        viewModel.isAiAnalyzing.observe(this) { isAnalyzing ->
            layoutAiStatus.visibility = if (isAnalyzing) View.VISIBLE else View.GONE
        }

        // MENSAJES (TOASTS)
        viewModel.statusMessage.observe(this) { message ->
            if (message == "¡Análisis inteligente completado!") {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
            if (loadingOverlay.visibility == View.VISIBLE && !message.isNullOrEmpty()) {
                tvLoadingMessage.text = message
            }
        }

        // AUDIO STREAM
        viewModel.audioStreamState.observe(this) { url ->
            layoutPlayerControls.visibility = View.VISIBLE
            if (!url.isNullOrEmpty()) {
                btnPlay.visibility = View.VISIBLE
                sbProgress.visibility = View.VISIBLE
                btnPlay.isEnabled = false
                btnPlay.alpha = 0.5f
                sbProgress.isEnabled = false
                sbProgress.alpha = 0.5f
                prepareMediaPlayer(url)
            } else {
                btnPlay.isEnabled = false
                btnPlay.alpha = 0.5f
                sbProgress.isEnabled = false
                sbProgress.alpha = 0.5f
            }
        }

        // WARNINGS
        viewModel.burnoutError.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("⚠️ Demasiadas flashcards pendientes")
                    .setMessage(errorMsg)
                    .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
                    .setCancelable(false)
                    .show()
            }
        }

        viewModel.dailyGoalWarning.observe(this) { warningMsg ->
            if (!warningMsg.isNullOrEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("📊 Meta diaria alcanzada")
                    .setMessage(warningMsg)
                    .setPositiveButton("Continuar") { dialog, _ -> dialog.dismiss() }
                    .setNegativeButton("Detener") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }

        // Cargar contenido
        val userLevel = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userLevel", "B1") ?: "B1"
        viewModel.loadContent(songTitle, songArtist, userLevel, previewUrl)
    }

    private fun initViews() {
        tvLyrics = findViewById(R.id.tvLyrics)
        tvTitle = findViewById(R.id.tvHeaderTitle)
        tvArtist = findViewById(R.id.tvHeaderArtist)

        // 🔥 NUEVOS ELEMENTOS
        cvAlbumContainer = findViewById(R.id.cvAlbumContainer)
        tvGrammyBadge = findViewById(R.id.tvGrammyBadge)

        btnPlay = findViewById(R.id.btnPlayPause)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        ivCover = findViewById(R.id.ivAlbumCover)
        layoutPlayerControls = findViewById(R.id.layoutPlayerControls)

        sbProgress = findViewById(R.id.sbProgress)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)

        tvLyrics.movementMethod = LinkMovementMethod.getInstance()
        btnBack.setOnClickListener { finish() }
    }

    private fun setupUI() {
        tvTitle.text = songTitle
        tvArtist.text = songArtist // Se sobrescribe en onCreate si es Grammy theme

        if (!coverUrl.isNullOrEmpty()) {
            ivCover.load(coverUrl) {
                crossfade(true)
                // Quitamos RoundedCornersTransformation aquí porque el CardView ya recorta las esquinas
                // y queda mejor si la imagen llena todo el CardView cuadrado
                error(R.drawable.ic_launcher_background)
            }
        }
    }

    private fun setupPlayer() {
        btnPlay.setOnClickListener { togglePlay() }
    }

    private fun setupSeekBar() {
        sbProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) tvCurrentTime.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isUserSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                mediaPlayer?.let { player ->
                    val targetProgress = seekBar?.progress ?: 0
                    if (targetProgress >= player.duration) {
                        player.seekTo(player.duration)
                        player.pause()
                        isPlaying = false
                        btnPlay.setImageResource(android.R.drawable.ic_media_play)
                    } else {
                        player.seekTo(targetProgress)
                        if (isPlaying) {
                            player.start()
                            startSeekBarUpdater()
                        }
                    }
                }
            }
        })
    }

    private fun prepareMediaPlayer(url: String) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnErrorListener { _, what, _ ->
                    if (what == -38) {
                        tvLoadingMessage.text = "Buffering audio..."
                        return@setOnErrorListener true
                    }
                    loadingOverlay.visibility = View.GONE
                    return@setOnErrorListener false
                }
                setDataSource(url)
                prepareAsync()

                setOnPreparedListener { mp ->
                    val duration = mp.duration
                    if (duration > 0) {
                        sbProgress.max = duration
                        tvTotalTime.text = formatTime(duration.toLong())
                    }
                    fun checkAndHideLoading() {
                        val formatted = tvTotalTime.text?.toString() ?: "00:00"
                        if (mp.duration > 0 && formatted != "00:00" && formatted != "-:--") {
                            loadingOverlay.visibility = View.GONE
                            btnPlay.isEnabled = true
                            btnPlay.alpha = 1.0f
                            sbProgress.isEnabled = true
                            sbProgress.alpha = 1.0f
                        } else {
                            handler.postDelayed({ checkAndHideLoading() }, 300)
                        }
                    }
                    handler.postDelayed({ checkAndHideLoading() }, 300)

                    if (isPlaying) {
                        mp.start()
                        startSeekBarUpdater()
                    }
                }
                setOnCompletionListener {
                    this@SongLearningActivity.isPlaying = false
                    btnPlay.setImageResource(android.R.drawable.ic_media_play)
                    sbProgress.progress = 0
                    tvCurrentTime.text = "00:00"
                    stopSeekBarUpdater()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlay() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlaying = false
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
                stopSeekBarUpdater()
            } else {
                player.start()
                isPlaying = true
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                startSeekBarUpdater()
            }
        }
    }

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying && !isUserSeeking) {
                    val current = player.currentPosition
                    val total = player.duration
                    sbProgress.progress = current
                    tvCurrentTime.text = formatTime(current.toLong())
                    if (total > 0 && sbProgress.max != total) {
                        sbProgress.max = total
                        tvTotalTime.text = formatTime(total.toLong())
                    }
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun startSeekBarUpdater() {
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.post(updateSeekBarRunnable)
    }
    private fun stopSeekBarUpdater() { handler.removeCallbacks(updateSeekBarRunnable) }

    private fun formatTime(millis: Long): String {
        if (millis < 0) return "00:00"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // --- HIGHLIGHTS Y DICCIONARIO ---
    private fun applyHighlights(data: HighlightWordsResponse) {
        val spannable = SpannableString(originalLyricsText)
        val lowerLyrics = originalLyricsText.lowercase()
        data.words.forEach { w -> highlightTerm(spannable, lowerLyrics, w.word, w, false) }
        data.expressions.forEach { e -> highlightTerm(spannable, lowerLyrics, e.expression, e, true) }
        tvLyrics.text = spannable
    }

    private fun highlightTerm(spannable: SpannableString, fullTextLower: String, term: String, itemData: Any, isExpression: Boolean) {
        val termLower = term.lowercase()
        var startIndex = fullTextLower.indexOf(termLower)
        while (startIndex >= 0) {
            val endIndex = startIndex + termLower.length
            val colorString = when (itemData) {
                is WordDefinition -> itemData.color
                is ExpressionDefinition -> itemData.color
                else -> "purple"
            }
            val color = when (colorString.lowercase()) {
                "orange" -> android.graphics.Color.parseColor("#FF9800")
                "red" -> android.graphics.Color.parseColor("#F44336")
                "green" -> android.graphics.Color.parseColor("#4CAF50")
                "gray" -> android.graphics.Color.parseColor("#9E9E9E")
                else -> ContextCompat.getColor(this, R.color.purple_200)
            }
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) { showDefinitionDialog(itemData) }
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

    private fun showDefinitionDialog(itemData: Any) {
        val builder = AlertDialog.Builder(this)
        var term = ""
        var def = ""
        var exampleOrTranslation = ""
        var isExpr = false

        if (itemData is WordDefinition) {
            builder.setTitle("📖 ${itemData.word}")
            builder.setMessage("Significado: ${itemData.definition}\n\nExplicación: ${itemData.explanation ?: ""}\n\nEjemplo: ${itemData.example}")
            term = itemData.word
            def = itemData.definition
            exampleOrTranslation = itemData.example
            isExpr = false
        } else if (itemData is ExpressionDefinition) {
            builder.setTitle("🗣️ ${itemData.expression}")
            builder.setMessage("Significado: ${itemData.meaning}\n\nExplicación: ${itemData.translation ?: ""}")
            term = itemData.expression
            def = itemData.meaning
            exampleOrTranslation = itemData.example
            isExpr = true
        }

        val itemSaved = if (itemData is WordDefinition) itemData.alreadySaved else (itemData as ExpressionDefinition).alreadySaved
        if (itemSaved) {
            builder.setNeutralButton("✓ Ya guardado") { _, _ ->
                Toast.makeText(this, "Ya está en tu diccionario", Toast.LENGTH_SHORT).show()
            }
        } else {
            builder.setNeutralButton("Guardar") { _, _ ->
                viewModel.addToDictionary(term, def, "", exampleOrTranslation, isExpression = isExpr)
                wordWasAdded = true
                Toast.makeText(this, "✓ Guardado", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSeekBarUpdater()
        if (wordWasAdded) setResult(RESULT_OK)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}