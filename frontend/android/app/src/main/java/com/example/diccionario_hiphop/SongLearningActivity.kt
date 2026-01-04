package com.example.diccionario_hiphop

import com.example.diccionario_hiphop.WordDefinition
import com.example.diccionario_hiphop.ExpressionDefinition
import com.example.diccionario_hiphop.HighlightWordsResponse
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
import coil.load
import coil.transform.RoundedCornersTransformation
import java.util.concurrent.TimeUnit
import com.example.diccionario_hiphop.utils.WindowInsetsHelper


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
    
    // 👇 Overlay de carga y mensaje
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingMessage: TextView
    // 👇 Spinner discreto IA
    private lateinit var layoutAiStatus: LinearLayout

    // 🔥 NUEVOS ELEMENTOS PARA LA SEEKBAR
    private lateinit var sbProgress: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var layoutPlayerControls: LinearLayout

    private var originalLyricsText: String = ""
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var wordWasAdded = false // Flag para saber si se añadió alguna palabra

    // 🔥 VARIABLES PARA CONTROL DE TIEMPO
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false // Para saber si el usuario está arrastrando la barra

    // Datos recibidos
    private var songTitle: String = ""
    private var songArtist: String = ""
    private var coverUrl: String? = null
    private var previewUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_learning)

        // � Aplicar WindowInsets para controles de reproducción
        val rootView = findViewById<View>(android.R.id.content)
        WindowInsetsHelper.applySystemBarInsets(rootView)

        // �🔥 INICIALIZAR EL MOTOR NUCLEAR AQUÍ
        try {
            com.yausername.youtubedl_android.YoutubeDL.getInstance().init(applicationContext)
            // Opcional: inicializar FFmpeg si fuera necesario, pero para sacar URL suele sobrar
            // com.yausername.youtubedl_android.FFmpeg.getInstance().init(applicationContext)
            android.util.Log.d("YoutubeDL", "Motor iniciado correctamente 🚀")
        } catch (e: Exception) {
            android.util.Log.e("YoutubeDL", "Error fatal al iniciar motor", e)
        }

        // 1. Recoger datos del Intent
        songTitle = intent.getStringExtra("song_title") ?: "Desconocido"
        songArtist = intent.getStringExtra("song_artist") ?: "Desconocido"
        coverUrl = intent.getStringExtra("song_image")      
        previewUrl = intent.getStringExtra("song_audio")    

        initViews()
        setupUI()     // Pone textos y CARGA LA IMAGEN
        setupPlayer() // Prepara el audio inicial
        setupSeekBar() // 🔥 CONFIGURA LA BARRA DE PROGRESO

        // Inicializar overlay (ya buscados en initViews, pero aseguramos)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage)
        layoutAiStatus = findViewById(R.id.layoutAiStatus)
        

        // 2. Observar ViewModel
        viewModel.lyricsState.observe(this) { lyrics ->
            originalLyricsText = lyrics
            tvLyrics.text = lyrics
        }

        viewModel.analysisState.observe(this) { analysis ->
            if (analysis != null) applyHighlights(analysis)
        }

        // CONTROL DE CARGA
        viewModel.loadingState.observe(this) { isLoading ->
            if (isLoading) {
                loadingOverlay.visibility = View.VISIBLE
                // El mensaje se actualizará por el observer de statusMessage
            } else {
                loadingOverlay.visibility = View.GONE
            }
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
            // Actualizar mensaje de carga dinámicamente
            if (loadingOverlay.visibility == View.VISIBLE && !message.isNullOrEmpty()) {
                tvLoadingMessage.text = message
            }
        }

        // AUDIO STREAM (YouTube / Preview)
        viewModel.audioStreamState.observe(this) { url ->
            android.util.Log.d("SongLearningActivity", "[audioStreamState] url: $url")
            android.util.Log.d("SongLearningActivity", "layoutPlayerControls.visibility BEFORE: ${layoutPlayerControls.visibility}")
            layoutPlayerControls.visibility = View.VISIBLE
            if (!url.isNullOrEmpty()) {
                btnPlay.visibility = View.VISIBLE
                sbProgress.visibility = View.VISIBLE
                btnPlay.isEnabled = false
                btnPlay.alpha = 0.5f
                sbProgress.isEnabled = false
                sbProgress.alpha = 0.5f
                android.util.Log.d("SongLearningActivity", "Intentando reproducir: $url")
                android.util.Log.d("SongLearningActivity", "layoutPlayerControls.visibility SET TO VISIBLE")
                prepareMediaPlayer(url)
            } else {
                btnPlay.isEnabled = false
                btnPlay.alpha = 0.5f
                sbProgress.isEnabled = false
                sbProgress.alpha = 0.5f
            }
            android.util.Log.d("SongLearningActivity", "layoutPlayerControls.visibility AFTER: ${layoutPlayerControls.visibility}")
        }
        
        // Llamar a loadContent fuera del observer
        val userLevel = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userLevel", "B1") ?: "B1"
        viewModel.loadContent(songTitle, songArtist, userLevel, previewUrl)
        
    } // 🔥 ESTA ERA LA LLAVE QUE FALTABA (Fin del onCreate)

    private fun initViews() {
        tvLyrics = findViewById(R.id.tvLyrics)
        btnPlay = findViewById(R.id.btnPlayPause) 
        progressBar = findViewById(R.id.progressBar) // Nota: ¿Usas progressBar o loadingOverlay?
        tvTitle = findViewById(R.id.tvHeaderTitle)
        tvArtist = findViewById(R.id.tvHeaderArtist)
        btnBack = findViewById(R.id.btnBack)
        ivCover = findViewById(R.id.ivAlbumCover) 
        layoutPlayerControls = findViewById(R.id.layoutPlayerControls)

        // 🔥 Inicializar vistas de la SeekBar
        sbProgress = findViewById(R.id.sbProgress)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)

        tvLyrics.movementMethod = LinkMovementMethod.getInstance()
        
        btnBack.setOnClickListener { finish() }
    }

    private fun setupUI() {
        tvTitle.text = songTitle
        tvArtist.text = songArtist
        
        // CARGA DE IMAGEN CON COIL
        if (!coverUrl.isNullOrEmpty()) {
            ivCover.load(coverUrl) {
                crossfade(true)
                transformations(RoundedCornersTransformation(16f))
                error(R.drawable.ic_launcher_background) 
            }
        }
    }

    private fun setupPlayer() {
        // Ya no mostramos controles aquí, solo configuramos el click
        btnPlay.setOnClickListener {
            togglePlay()
        }
    }

    /**
     * 🔥 CONFIGURACIÓN DE LA BARRA DE PROGRESO (LISTENER)
     */
    private fun setupSeekBar() {
        sbProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Si el usuario mueve el dedo, actualizamos el texto en tiempo real
                    tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true // Pausamos el reloj automático
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false // Reanudamos el reloj automático
                mediaPlayer?.let { player ->
                    val targetProgress = seekBar?.progress ?: 0
                    // Asegurarse de no buscar más allá del final
                    if (targetProgress >= player.duration) {
                        player.seekTo(player.duration)
                        player.pause()
                        isPlaying = false
                        btnPlay.setImageResource(android.R.drawable.ic_media_play)
                    } else {
                        player.seekTo(targetProgress)
                        // Si estaba reproduciendo antes de arrastrar, seguir reproduciendo
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
            android.util.Log.d("SongLearningActivity", "prepareMediaPlayer: $url")
            // Liberar anterior si existe
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
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("SongLearningActivity", "Error al reproducir audio: $url (code: $what)")
                    if (what == -38) {
                        tvLoadingMessage.text = "Cargando audio... (esperando buffer)"
                        // No ocultar loadingOverlay ni mostrar Toast
                        return@setOnErrorListener true
                    } else {
                        loadingOverlay.visibility = View.GONE
                        return@setOnErrorListener false
                    }
                }
                setDataSource(url)
                prepareAsync() // Async para no bloquear UI

                setOnPreparedListener { mp ->
                // 🔥 ACTUALIZAR BARRA AL CARGAR
                val duration = mp.duration
                if (duration > 0) {
                    sbProgress.max = duration
                    tvTotalTime.text = formatTime(duration.toLong())
                }
                // Solo ocultar loadingOverlay y habilitar controles cuando el buffer esté listo y el cronómetro (tvCurrentTime) muestra tiempo real
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
                // Si ya estábamos reproduciendo (cambio de preview a full), reanudar
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
            android.util.Log.e("SongLearningActivity", "Excepción en prepareMediaPlayer: $url", e)
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

    /**
     * 🔥 RELOJ QUE MUEVE LA BARRA CADA SEGUNDO
     */
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying && !isUserSeeking) {
                    val currentPosition = player.currentPosition
                    val totalDuration = player.duration

                    // Actualizar UI
                    sbProgress.progress = currentPosition
                    tvCurrentTime.text = formatTime(currentPosition.toLong())
                    
                    // Asegurar que el total está bien puesto
                    if (totalDuration > 0 && sbProgress.max != totalDuration) {
                        sbProgress.max = totalDuration
                        tvTotalTime.text = formatTime(totalDuration.toLong())
                    }
                    
                    // Repetir en 1 segundo (o menos para más fluidez)
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun startSeekBarUpdater() {
        handler.removeCallbacks(updateSeekBarRunnable) // Evitar duplicados
        handler.post(updateSeekBarRunnable)
    }

    private fun stopSeekBarUpdater() {
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    // Helper para formato 00:00
    private fun formatTime(millis: Long): String {
        if (millis < 0) return "00:00"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // -------------------------------------------------------------
    // LÓGICA DE LETRAS Y DIÁLOGOS (Highlight Words)
    // -------------------------------------------------------------

    // Usa HighlightWordsResponse (que ya tienes)
    private fun applyHighlights(data: HighlightWordsResponse) {
        val spannable = SpannableString(originalLyricsText)
        val lowerLyrics = originalLyricsText.lowercase()

        // Usamos 'data.words' que es List<WordDefinition>
        data.words.forEach { w -> highlightTerm(spannable, lowerLyrics, w.word, w, false) }
        // Usamos 'data.expressions' que es List<ExpressionDefinition>
        data.expressions.forEach { e -> highlightTerm(spannable, lowerLyrics, e.expression, e, true) }

        tvLyrics.text = spannable
    }

    private fun highlightTerm(spannable: SpannableString, fullTextLower: String, term: String, itemData: Any, isExpression: Boolean) {
        val termLower = term.lowercase()
        var startIndex = fullTextLower.indexOf(termLower)
        while (startIndex >= 0) {
            val endIndex = startIndex + termLower.length
            
            // 🔥 CORRECCIÓN: Usamos tus clases originales
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

        // 🔥 CORRECCIÓN: Usamos WordDefinition
        if (itemData is WordDefinition) {
            // En tu ApiModels: 'definition' viene del JSON "translation"
            builder.setTitle("📖 ${itemData.word}")
            builder.setMessage("Significado: ${itemData.definition}\n\nExplicación: ${itemData.explanation ?: ""}\n\nEjemplo: ${itemData.example}")
            
            term = itemData.word
            def = itemData.definition
            val explanation = itemData.explanation ?: ""
            exampleOrTranslation = itemData.example
            isExpr = false 
            
            if (itemData.alreadySaved) {
                builder.setNeutralButton("✓ Ya guardado") { _, _ ->
                    Toast.makeText(this, "Esta palabra ya está guardada", Toast.LENGTH_SHORT).show()
                }
            } else {
                builder.setNeutralButton("Guardar") { _, _ ->
                    viewModel.addToDictionary(term, def, explanation, exampleOrTranslation, isExpression = isExpr)
                    wordWasAdded = true
                    Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
                }
            }

        // 🔥 CORRECCIÓN: Usamos ExpressionDefinition
        } else if (itemData is ExpressionDefinition) {
            // En tu ApiModels: 'meaning' es la traducción principal
            builder.setTitle("🗣️ ${itemData.expression}")
            builder.setMessage("Significado: ${itemData.meaning}\n\nExplicación: ${itemData.translation ?: ""}") 
            
            term = itemData.expression
            def = itemData.meaning
            val explanation = itemData.translation ?: "" // Ojo a tu mapeo en ApiModels
            exampleOrTranslation = itemData.example
            isExpr = true 
            
            if (itemData.alreadySaved) {
                builder.setNeutralButton("✓ Ya guardado") { _, _ ->
                    Toast.makeText(this, "Esta expresión ya está guardada", Toast.LENGTH_SHORT).show()
                }
            } else {
                builder.setNeutralButton("Guardar") { _, _ ->
                    viewModel.addToDictionary(term, def, explanation, exampleOrTranslation, isExpression = isExpr)
                    wordWasAdded = true
                    Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSeekBarUpdater()
        if (wordWasAdded) {
            setResult(RESULT_OK)
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }
}