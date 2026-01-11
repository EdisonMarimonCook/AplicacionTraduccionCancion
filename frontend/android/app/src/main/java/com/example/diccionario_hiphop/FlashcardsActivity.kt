package com.example.diccionario_hiphop

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.diccionario_hiphop.utils.NetworkMonitor
import com.example.diccionario_hiphop.utils.OfflineManager
import com.example.diccionario_hiphop.utils.WindowInsetsHelper
import kotlinx.coroutines.launch
import kotlin.math.abs
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class FlashcardsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FlashcardsActivity"
    }

    // Views
    private lateinit var tvCounter: TextView
    private lateinit var tvWord: TextView
    private lateinit var tvContextQuestion: TextView
    private lateinit var tvTranslation: TextView
    private lateinit var tvExplanation: TextView
    private lateinit var divider: View
    private lateinit var tvTapHint: TextView
    private lateinit var touchOverlay: View
    private lateinit var layoutSwipeArrows: View
    private lateinit var tvMessage: TextView
    private lateinit var cardView: CardView
    private lateinit var tvIntervalIndicator: TextView
    private lateinit var btnAudio: ImageButton
    private lateinit var konfettiView: KonfettiView  // 🎉 Vista de confeti
    
    // Barras de Progreso
    private lateinit var progressBarLinear: ProgressBar
    private lateinit var progressBarLoading: ProgressBar

    // Offline Support
    private lateinit var offlineManager: OfflineManager
    private lateinit var networkMonitor: NetworkMonitor

    // Variables de control de Swipe
    private var dX = 0f
    private var dY = 0f
    private var initialRawX = 0f
    private var isAnswerRevealed = false
    private var hasPlayedAutoAudio = false
    
    private var isProcessingSwipe = false 
    
    private val SWIPE_THRESHOLD = 300f 

    // Data
    private var flashcards: MutableList<FlashcardData> = mutableListOf()
    private var currentIndex = 0
    
    private var selectedLanguage: String? = null
    private var selectedType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcards)

        val rootView = findViewById<View>(android.R.id.content)
        WindowInsetsHelper.applySystemBarInsets(rootView)

        selectedLanguage = intent.getStringExtra("language")
        
        offlineManager = OfflineManager(this)
        networkMonitor = NetworkMonitor.getInstance(this)

        initViews()
        setupCardPhysics()
        setupOfflineSync()

        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }
    }
    
    private fun setupOfflineSync() {
        // Cargar una sola vez según el estado actual de conectividad
        lifecycleScope.launch {
            val isConnected = networkMonitor.isConnected.value
            if (isConnected) {
                // Sincronizar reseñas pendientes primero
                syncPendingReviews()
                loadFlashcards()
            } else {
                loadOfflineFlashcards()
            }
        }
    }
    
    private suspend fun syncPendingReviews() {
        val pendingReviews = offlineManager.getPendingReviews()
        if (pendingReviews.isEmpty()) return
        
        val api = RetrofitService.getInstance(this)
        var syncedCount = 0
        
        pendingReviews.forEach { (wordId, quality) ->
            try {
                val request = FlashcardReviewRequest(quality)
                val response = api.reviewFlashcard(wordId, request)
                if (response.isSuccessful) {
                    offlineManager.markReviewSynced(wordId)
                    syncedCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sincronizando reseña de $wordId: ${e.message}")
            }
        }
        
        if (syncedCount > 0) {
            Log.d(TAG, "✅ Sincronizadas $syncedCount reseñas pendientes")
        }
    }

    private fun initViews() {
        tvCounter = findViewById(R.id.tvCounter)
        tvWord = findViewById(R.id.tvWord)
        tvContextQuestion = findViewById(R.id.tvContextQuestion)
        tvTranslation = findViewById(R.id.tvTranslation)
        tvExplanation = findViewById(R.id.tvExplanation)
        divider = findViewById(R.id.divider)
        tvTapHint = findViewById(R.id.tvTapHint)
        touchOverlay = findViewById(R.id.touchOverlay)
        layoutSwipeArrows = findViewById(R.id.layoutSwipeArrows)
        tvMessage = findViewById(R.id.tvMessage)
        cardView = findViewById(R.id.cardView)
        tvIntervalIndicator = findViewById(R.id.tvIntervalIndicator)
        btnAudio = findViewById(R.id.btnAudio)  // 🔊 Botón de audio
        konfettiView = findViewById(R.id.konfettiView)  // 🎉 Vista de confeti
        
        // Enlazamos las dos barras (importante para que se muevan)
        progressBarLinear = findViewById(R.id.progressBarLinear)
        progressBarLoading = findViewById(R.id.progressBar)

        touchOverlay.setOnClickListener { revealAnswer() }
        
        // 🔊 Configurar botón de audio (reproducción manual)
        btnAudio.setOnClickListener { playCurrentCardAudio() }
        
        // 🔥 Configurar chips de modo
        setupModeChips()
    }
    
    private fun setupModeChips() {
        val chipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupMode)
        val chipGlobal = findViewById<com.google.android.material.chip.Chip>(R.id.chipGlobal)
        val chipWords = findViewById<com.google.android.material.chip.Chip>(R.id.chipWords)
        val chipExpressions = findViewById<com.google.android.material.chip.Chip>(R.id.chipExpressions)
        
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            selectedType = when (checkedIds[0]) {
                R.id.chipWords -> "word"
                R.id.chipExpressions -> "expression"
                else -> null  // Global
            }
            
            // Recargar flashcards con el nuevo filtro
            loadFlashcards()
        }
    }

    private fun setupCardPhysics() {
        cardView.setOnTouchListener { view, event ->
            // Si la respuesta no está visible O ya estamos procesando un swipe, ignoramos el toque
            if (!isAnswerRevealed || isProcessingSwipe) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    initialRawX = event.rawX
                    true 
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val deltaX = event.rawX - initialRawX 

                    view.animate()
                        .x(newX)
                        .setDuration(0) 
                        .start()

                    // Rotación suave
                    val rotation = (deltaX / 40f).coerceIn(-15f, 15f)
                    view.rotation = rotation

                    updateColorFeedback(deltaX)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - initialRawX
                    
                    if (abs(deltaX) > SWIPE_THRESHOLD) {
                        if (deltaX > 0) {
                            swipeRight() // FÁCIL
                        } else {
                            swipeLeft() // DIFÍCIL
                        }
                    } else {
                        resetCardPosition()
                    }
                    // Limpieza visual al soltar
                    if (abs(deltaX) <= SWIPE_THRESHOLD) {
                        tvIntervalIndicator.visibility = View.GONE
                        cardView.foreground = null
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun updateColorFeedback(deltaX: Float) {
        if (deltaX > 100) {
            cardView.foreground = ColorDrawable(Color.parseColor("#4D4CAF50")) 
            tvIntervalIndicator.visibility = View.VISIBLE
            tvIntervalIndicator.text = "FÁCIL ✓"
            tvIntervalIndicator.setBackgroundColor(Color.parseColor("#4CAF50"))
        } else if (deltaX < -100) {
            cardView.foreground = ColorDrawable(Color.parseColor("#4DFF9800"))  // 🟠 Naranja suave en vez de rojo
            tvIntervalIndicator.visibility = View.VISIBLE
            tvIntervalIndicator.text = "DIFÍCIL ✗"
            tvIntervalIndicator.setBackgroundColor(Color.parseColor("#FF9800"))  // 🟠 Naranja en vez de rojo
        } else {
            cardView.foreground = null
            tvIntervalIndicator.visibility = View.GONE
        }
    }

    private fun resetCardPosition() {
        cardView.animate()
            .x(0f) 
            .translationX(0f) 
            .rotation(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.5f)) 
            .start()
    }

    private fun swipeRight() {
        if (isProcessingSwipe) return // Doble seguridad
        isProcessingSwipe = true // 🔴 BLOQUEAMOS INTERACCIÓN
        
        showIntervalPreview(true)
        
        // ✅ Fondo verde degradado para "FÁCIL"
        cardView.setBackgroundResource(R.drawable.bg_swipe_easy)
        
        cardView.animate()
            .translationX(1500f) 
            .rotation(20f)
            .setDuration(300)
            .setListener(null) // 🔥 Limpiar listener anterior
            .withEndAction {
                submitReview(4) // 4 = Fácil
            }.start()
    }

    private fun swipeLeft() {
        if (isProcessingSwipe) return // Doble seguridad
        isProcessingSwipe = true // 🔴 BLOQUEAMOS INTERACCIÓN

        showIntervalPreview(false)
        
        // ❌ Fondo naranja degradado para "DIFÍCIL"
        cardView.setBackgroundResource(R.drawable.bg_swipe_hard)
        
        cardView.animate()
            .translationX(-1500f) 
            .rotation(-20f)
            .setDuration(300)
            .setListener(null) // 🔥 Limpiar listener anterior
            .withEndAction {
                submitReview(1) // 1 = Difícil
            }.start()
    }

    private fun showIntervalPreview(isEasy: Boolean) {
        val card = flashcards[currentIndex]
        val currentReps = card.repetitions
        val currentInterval = card.interval

        val estimatedDays = if (isEasy) {
            if (currentReps == 0) 1
            else if (currentReps == 1) 6
            else (currentInterval * 2.5).toInt().coerceAtLeast(7)
        } else {
            1
        }

        val message = if (isEasy) {
            "Fácil ✓\nLa verás en $estimatedDays ${if (estimatedDays == 1) "día" else "días"}"
        } else {
            "Difícil ✗\nLa verás mañana"
        }

        tvIntervalIndicator.text = message
        tvIntervalIndicator.setBackgroundColor(if (isEasy) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800"))  // 🟠 Naranja para difícil
        tvIntervalIndicator.visibility = View.VISIBLE
    }

    private fun loadFlashcards() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@FlashcardsActivity)
                val response = api.getDueFlashcards(selectedLanguage, selectedType)

                if (response.isSuccessful && response.body() != null) {
                    flashcards = response.body()!!.toMutableList()
                    
                    Log.d(TAG, "✅ Flashcards cargadas: ${flashcards.size} tarjetas")
                    flashcards.forEachIndexed { index, card ->
                        Log.d(TAG, "  [$index] ${card.word} (${card.type}) - ${card.language}")
                    }
                    
                    // Sincronizar con offline
                    offlineManager.syncFlashcards(flashcards)

                    if (flashcards.isEmpty()) {
                        showEmptyState("¡No tienes repasos pendientes! 🎉")
                        setProgressSmoothly(100) 
                    } else {
                        showGameUI()
                        currentIndex = 0
                        Log.d(TAG, "🎮 Iniciando juego - currentIndex = $currentIndex, total = ${flashcards.size}")
                        showCard()
                    }
                } else {
                    // Fallar silenciosamente en modo offline - el sistema intentará cargar offline
                    Log.d(TAG, "📴 Error respuesta - intentando modo offline")
                    loadOfflineFlashcards()
                }
            } catch (e: Exception) {
                // Fallar silenciosamente en modo offline - el sistema intentará cargar offline  
                Log.d(TAG, "📴 Error excepción - intentando modo offline: ${e.message}")
                loadOfflineFlashcards()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun loadOfflineFlashcards() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // 🔥 Filtrar por idioma y tipo también en modo offline
                offlineManager.getDueFlashcards(selectedLanguage, selectedType).collect { cachedCards ->
                    flashcards = cachedCards.map { entity ->
                        FlashcardData(
                            id = entity.id,
                            wordId = entity.wordId,
                            word = entity.word,
                            translation = entity.translation,
                            example = "",
                            explanation = null,
                            type = entity.type ?: "word",  // 🔥 Usar el tipo real de la entity
                            language = entity.language,
                            songYoutubeUrl = entity.songContext,
                            timestampStart = null,
                            timestampEnd = null,
                            easinessFactor = entity.easeFactor,
                            interval = entity.interval,
                            repetitions = entity.repetitions,
                            nextReviewDate = entity.nextReviewDate
                        )
                    }.toMutableList()

                    if (flashcards.isEmpty()) {
                        showEmptyState("📴 No hay repasos offline")
                        setProgressSmoothly(100)
                    } else {
                        showGameUI()
                        currentIndex = 0
                        showCard()
                    }
                    showLoading(false)
                }
            } catch (e: Exception) {
                showEmptyState("Error cargando datos offline")
                showLoading(false)
            }
        }
    }

    private fun showCard() {
        Log.d(TAG, "📇 showCard() - Mostrando índice: $currentIndex de ${flashcards.size}")
        
        // 🟢 DESBLOQUEAMOS INTERACCIÓN (Ya cargó la nueva carta)
        isProcessingSwipe = false
        hasPlayedAutoAudio = false  // 🔊 Resetear flag para permitir audio automático en la siguiente carta
        
        // 🔥 ANIMACIÓN DE BARRA DE PROGRESO
        val targetProgress = if (flashcards.size > 0) {
            (currentIndex * 100) / flashcards.size
        } else 0
        setProgressSmoothly(targetProgress)

        val card = flashcards[currentIndex]
        Log.d(TAG, "📇 Flashcard: ${card.word} (${card.type})")
        isAnswerRevealed = false

        // Reseteo visual completo de la carta
        cardView.translationX = 0f
        cardView.rotation = 0f
        cardView.alpha = 1f
        cardView.foreground = null
        cardView.setCardBackgroundColor(getColor(R.color.bg_card))  // 🎨 Restablecer fondo original
        tvIntervalIndicator.visibility = View.GONE

        // Datos
        tvWord.text = card.word.replaceFirstChar { it.uppercase() }
        tvContextQuestion.text = "\"${card.example}\""
        tvTranslation.text = "📖 ${card.translation}"
        tvExplanation.text = card.explanation ?: "Sin explicación disponible"

        // Ocultar respuesta
        tvTranslation.visibility = View.INVISIBLE
        tvExplanation.visibility = View.INVISIBLE
        divider.visibility = View.INVISIBLE
        layoutSwipeArrows.visibility = View.INVISIBLE
        btnAudio.visibility = View.INVISIBLE  // 🔊 Ocultar botón de audio inicialmente
        touchOverlay.visibility = View.VISIBLE
        tvTapHint.visibility = View.VISIBLE

        tvCounter.text = "${currentIndex + 1} / ${flashcards.size}"
    }

    // Animación suave de la barra (500ms)
    private fun setProgressSmoothly(targetProgress: Int) {
        val animation = ObjectAnimator.ofInt(progressBarLinear, "progress", progressBarLinear.progress, targetProgress)
        animation.duration = 500
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    private fun revealAnswer() {
        isAnswerRevealed = true
        tvTranslation.visibility = View.VISIBLE
        tvExplanation.visibility = View.VISIBLE
        divider.visibility = View.VISIBLE
        layoutSwipeArrows.visibility = View.VISIBLE
        touchOverlay.visibility = View.GONE
        tvTapHint.visibility = View.GONE
        btnAudio.visibility = View.VISIBLE  // 🔊 Mostrar botón de audio
        
        // 🔊 REPRODUCIR AUDIO AUTOMÁTICAMENTE LA PRIMERA VEZ
        if (!hasPlayedAutoAudio) {
            hasPlayedAutoAudio = true
            playCurrentCardAudio()
        }
    }

    private fun submitReview(quality: Int) {
        val card = flashcards[currentIndex]
        Log.d(TAG, "📝 submitReview() - Evaluando índice: $currentIndex, palabra: ${card.word}, calidad: $quality")

        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@FlashcardsActivity)
                val request = FlashcardReviewRequest(quality)
                val response = api.reviewFlashcard(card.wordId, request)
                
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    val realDays = result.intervalDays
                    Log.d(TAG, "✅ Review exitoso - próximo repaso en $realDays días")
                    val message = if (quality >= 3) {
                        "✓ La verás en $realDays ${if (realDays == 1) "día" else "días"}"
                    } else {
                        "✗ La verás mañana"
                    }
                    tvIntervalIndicator.text = message
                    
                    // ✅ Avanzar después del delay
                    cardView.postDelayed({
                        advanceToNextCard()
                    }, 350)
                } else {
                    // Si falla el server, guardar offline y avanzar igual
                    offlineManager.savePendingReview(card.wordId, quality)
                    Log.d(TAG, "💾 Reseña guardada offline (se sincronizará)")
                    
                    cardView.postDelayed({
                        advanceToNextCard()
                    }, 350)
                }

            } catch (e: Exception) {
                // Error de red - guardar offline y avanzar
                offlineManager.savePendingReview(card.wordId, quality)
                Log.d(TAG, "📴 Reseña guardada offline (sin conexión)")
                
                cardView.postDelayed({
                    advanceToNextCard()
                }, 350)
            }
        }
    }
    
    /**
     * Avanza a la siguiente flashcard o muestra pantalla de completado
     */
    private fun advanceToNextCard() {
        Log.d(TAG, "⏭️ advanceToNextCard() - currentIndex antes: $currentIndex, total: ${flashcards.size}")
        currentIndex++
        Log.d(TAG, "⏭️ advanceToNextCard() - currentIndex después: $currentIndex")
        
        // Verificar si quedan más flashcards
        if (currentIndex >= flashcards.size) {
            Log.d(TAG, "🎉 Repaso completado - currentIndex: $currentIndex >= ${flashcards.size}")
            showCompletionCelebration()  // 🎉 Mostrar confeti
            showEmptyState("¡Repaso completado! 🎉")
            setProgressSmoothly(100)
        } else {
            Log.d(TAG, "➡️ Mostrando siguiente flashcard - índice: $currentIndex")
            showCard()
        }
    }
    
    /**
     * 🎉 Muestra animación de confeti al completar la sesión
     */
    private fun showCompletionCelebration() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.3)
        )
        
        konfettiView.start(party)
    }

    private fun showLoading(isLoading: Boolean) {
        progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(msg: String) {
        tvMessage.text = msg
        tvMessage.visibility = View.VISIBLE
        cardView.visibility = View.GONE
        layoutSwipeArrows.visibility = View.GONE
    }

    private fun showGameUI() {
        tvMessage.visibility = View.GONE
        cardView.visibility = View.VISIBLE
    }
    
    // ========================================================================
    // 🔊 FUNCIONES DE AUDIO
    // ========================================================================
    
    /**
     * Reproduce audio de la tarjeta actual
     * Prioridad: 1) Fragmento de YouTube, 2) TTS
     */
    private fun playCurrentCardAudio() {
        // Verificar si hay conexión a Internet
        if (!networkMonitor.isConnected.value) {
            Toast.makeText(this, "📴 Audio no disponible sin conexión", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (currentIndex >= flashcards.size) return
        
        val card = flashcards[currentIndex]
        
        lifecycleScope.launch {
            try {
                AudioPlayerHelper.playAudio(
                    context = this@FlashcardsActivity,
                    text = card.word,
                    language = card.language,
                    songYoutubeUrl = card.songYoutubeUrl,
                    timestampStart = card.timestampStart,
                    timestampEnd = card.timestampEnd
                )
            } catch (e: Exception) {
                android.util.Log.e("FlashcardsActivity", "Error reproduciendo audio: ${e.message}")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 🔊 Liberar recursos de audio
        AudioPlayerHelper.releasePlayer()
    }
}