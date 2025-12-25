package com.example.diccionario_hiphop

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
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
    // 👇 NUEVO: Overlay de carga y mensaje
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingMessage: TextView
    // 👇 NUEVO: Spinner discreto IA
    private lateinit var layoutAiStatus: LinearLayout
    

    private var originalLyricsText: String = ""
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var wordWasAdded = false // 🔥 Flag para saber si se añadió alguna palabra

    // Datos recibidos
    private var songTitle: String = ""
    private var songArtist: String = ""
    private var coverUrl: String? = null
    private var previewUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_learning)

        // 1. Recoger datos del Intent (Sincronizado con SelectionActivity)
        songTitle = intent.getStringExtra("song_title") ?: "Desconocido"
        songArtist = intent.getStringExtra("song_artist") ?: "Desconocido"
        coverUrl = intent.getStringExtra("song_image")      
        previewUrl = intent.getStringExtra("song_audio")   

        initViews()
        setupUI()     // Pone textos y CARGA LA IMAGEN
        setupPlayer() // Prepara el audio

        // 👇 Inicializar overlay y mensaje
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

        // 👇 1. CONTROL DE LA PANTALLA DE CARGA Y FAB/LOGO
        viewModel.loadingState.observe(this) { isLoading ->
            if (isLoading) {
                loadingOverlay.visibility = View.VISIBLE
               
            } else {
                loadingOverlay.visibility = View.GONE
                
            }
        }

        // 👇 2. SPINNER DISCRETO IA
        viewModel.isAiAnalyzing.observe(this) { isAnalyzing ->
            layoutAiStatus.visibility = if (isAnalyzing) View.VISIBLE else View.GONE
        }

        // 👇 3. MENSAJES INFORMATIVOS (TOASTS) - CORREGIDO
        viewModel.statusMessage.observe(this) { message ->
            // Solo mostramos el Toast si el mensaje NO es nulo Y NO está vacío
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        // 👇 4. AUDIO STREAM (YouTube)
        viewModel.audioStreamState.observe(this) { streamUrl ->
            if (!streamUrl.isNullOrEmpty()) {
                prepareMediaPlayer(streamUrl)
            }
        }

        viewModel.errorState.observe(this) { errorMsg ->
            // Protegemos también los errores para que no salgan vacíos
            if (!errorMsg.isNullOrBlank()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        }

        // 3. Cargar datos si no existen (Llama a la IA)
        val userLevel = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("userLevel", "B1") ?: "B1"
        viewModel.loadContent(songTitle, songArtist, userLevel)
    }

    private fun initViews() {
        tvLyrics = findViewById(R.id.tvLyrics)
        btnPlay = findViewById(R.id.btnPlayPause)
        progressBar = findViewById(R.id.progressBar)
        tvTitle = findViewById(R.id.tvHeaderTitle)
        tvArtist = findViewById(R.id.tvHeaderArtist)
        btnBack = findViewById(R.id.btnBack)
        ivCover = findViewById(R.id.ivAlbumCover) 

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
        // Configuración inicial (si venía preview del Intent)
        if (!previewUrl.isNullOrEmpty()) {
            prepareMediaPlayer(previewUrl!!)
        } else {
            // Si no hay preview, deshabilitamos hasta que llegue el de YouTube
            btnPlay.isEnabled = false
            btnPlay.alpha = 0.5f
        }

        btnPlay.setOnClickListener {
            togglePlay()
        }
    }

    // 🔥 NUEVO: Función reutilizable para cargar cualquier URL
    private fun prepareMediaPlayer(url: String) {
        try {
            // Si ya existía, lo reseteamos para cargar la nueva URL
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                }
            } else {
                mediaPlayer?.reset()
            }

            mediaPlayer?.apply {
                setDataSource(url)
                prepareAsync() // Carga en segundo plano
                
                setOnPreparedListener {
                    btnPlay.isEnabled = true
                    btnPlay.alpha = 1.0f
                    // Opcional: Auto-play si quieres que arranque solo
                    // start() 
                    // isPlaying = true
                    // btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                }
                
                setOnCompletionListener {
                    this@SongLearningActivity.isPlaying = false
                    btnPlay.setImageResource(android.R.drawable.ic_media_play)
                }
                
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(this@SongLearningActivity, "Error audio: $what", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error cargando audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlay() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlaying = false
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player.start()
                isPlaying = true
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

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
            
            // 🔥 Obtener color del campo 'color' que viene del backend
            val colorString = when {
                itemData is WordDefinition -> itemData.color
                itemData is ExpressionDefinition -> itemData.color
                else -> "purple"
            }
            
            // Mapear string a color Android
            val color = when (colorString.lowercase()) {
                "orange" -> android.graphics.Color.parseColor("#FF9800")  // Material Orange
                "red" -> android.graphics.Color.parseColor("#F44336")     // Material Red
                "green" -> android.graphics.Color.parseColor("#4CAF50")   // Material Green
                "gray" -> android.graphics.Color.parseColor("#9E9E9E")    // Material Gray (palabras guardadas)
                else -> ContextCompat.getColor(this, R.color.purple_200)  // Fallback morado
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
        var isExpr = false // ✅ Variable para saber qué es

        if (itemData is WordDefinition) {
            builder.setTitle("📖 ${itemData.word}")
            builder.setMessage("Significado: ${itemData.definition}\n\nEjemplo: ${itemData.example}")
            
            term = itemData.word
            def = itemData.definition  // Traducción literal
            val explanation = itemData.explanation ?: "Sin explicación"
            exampleOrTranslation = itemData.example
            isExpr = false // Es palabra
            
            // 🔥 SI YA ESTÁ GUARDADA, CAMBIAR BOTÓN
            if (itemData.alreadySaved) {
                builder.setNeutralButton("✓ Ya guardado") { _, _ ->
                    Toast.makeText(this, "Esta palabra ya está en tu diccionario", Toast.LENGTH_SHORT).show()
                }
            } else {
                builder.setNeutralButton("Guardar en mi Vocabulario") { _, _ ->
                    viewModel.addToDictionary(term, def, explanation, exampleOrTranslation, isExpression = isExpr)
                    wordWasAdded = true
                    Toast.makeText(this, "Guardado: $term", Toast.LENGTH_SHORT).show()
                }
            }

        } else if (itemData is ExpressionDefinition) {
            builder.setTitle("🗣️ ${itemData.expression}")
            builder.setMessage("Significado: ${itemData.meaning}\n\nTraducción: ${itemData.translation ?: "Sin traducción"}")
            
            term = itemData.expression
            def = itemData.meaning  // Significado/traducción
            val explanation = itemData.translation ?: ""  // Contexto adicional
            exampleOrTranslation = itemData.example
            isExpr = true // Es expresión
            
            // 🔥 SI YA ESTÁ GUARDADA, CAMBIAR BOTÓN
            if (itemData.alreadySaved) {
                builder.setNeutralButton("✓ Ya guardado") { _, _ ->
                    Toast.makeText(this, "Esta expresión ya está en tu diccionario", Toast.LENGTH_SHORT).show()
                }
            } else {
                builder.setNeutralButton("Guardar en mi Vocabulario") { _, _ ->
                    viewModel.addToDictionary(term, def, explanation, exampleOrTranslation, isExpression = isExpr)
                    wordWasAdded = true
                    Toast.makeText(this, "Guardado: $term", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }

        builder.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 🔥 Si se añadió alguna palabra, devolver RESULT_OK para que se recargue el perfil
        if (wordWasAdded) {
            setResult(RESULT_OK)
        }
        
        mediaPlayer?.release()
        mediaPlayer = null
    }
}