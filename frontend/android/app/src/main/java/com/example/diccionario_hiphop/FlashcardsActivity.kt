package com.example.diccionario_hiphop

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.example.diccionario_hiphop.utils.WindowInsetsHelper

class FlashcardsActivity : AppCompatActivity() {

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
    
    // Barras de Progreso
    private lateinit var progressBarLinear: ProgressBar // Barra superior
    private lateinit var progressBarLoading: ProgressBar // Spinner de carga

    // Variables de control de Swipe
    private var dX = 0f
    private var dY = 0f
    private var initialRawX = 0f
    private var isAnswerRevealed = false
    
    // 🔥 SEMÁFORO: Evita que se salte cartas si deslizas rápido o doble
    private var isProcessingSwipe = false 
    
    private val SWIPE_THRESHOLD = 300f 

    // Data
    private var flashcards: MutableList<FlashcardData> = mutableListOf()
    private var currentIndex = 0
    
    // 🔥 Parámetros de filtrado
    private var selectedLanguage: String? = null
    private var selectedType: String? = null  // "word", "expression" o null (global)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcards)

        // 🔧 Aplicar WindowInsets para controles inferiores
        val rootView = findViewById<View>(android.R.id.content)
        WindowInsetsHelper.applySystemBarInsets(rootView)

        // 🔥 Obtener idioma del Intent
        selectedLanguage = intent.getStringExtra("language")

        initViews()
        setupCardPhysics()
        loadFlashcards()

        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }
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
        
        // Enlazamos las dos barras (importante para que se muevan)
        progressBarLinear = findViewById(R.id.progressBarLinear)
        progressBarLoading = findViewById(R.id.progressBar)

        touchOverlay.setOnClickListener { revealAnswer() }
        
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
            cardView.foreground = ColorDrawable(Color.parseColor("#4DF44336")) 
            tvIntervalIndicator.visibility = View.VISIBLE
            tvIntervalIndicator.text = "DIFÍCIL ✗"
            tvIntervalIndicator.setBackgroundColor(Color.parseColor("#F44336"))
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
        cardView.animate()
            .translationX(1500f) 
            .rotation(20f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    submitReview(4) // 4 = Fácil
                }
            }).start()
    }

    private fun swipeLeft() {
        if (isProcessingSwipe) return // Doble seguridad
        isProcessingSwipe = true // 🔴 BLOQUEAMOS INTERACCIÓN

        showIntervalPreview(false)
        cardView.animate()
            .translationX(-1500f) 
            .rotation(-20f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    submitReview(1) // 1 = Difícil
                }
            }).start()
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
        tvIntervalIndicator.setBackgroundColor(if (isEasy) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        tvIntervalIndicator.visibility = View.VISIBLE
    }

    private fun loadFlashcards() {
        showLoading(true)
        lifecycl// 🔥 Pasar idioma y tipo al API
                val response = api.getDueFlashcards(selectedLanguage, selectedType
            try {
                val api = RetrofitService.getInstance(this@FlashcardsActivity)
                val response = api.getDueFlashcards()

                if (response.isSuccessful && response.body() != null) {
                    flashcards = response.body()!!.toMutableList()

                    if (flashcards.isEmpty()) {
                        showEmptyState("¡No tienes repasos pendientes! 🎉")
                        setProgressSmoothly(100) 
                    } else {
                        showGameUI()
                        currentIndex = 0
                        showCard()
                    }
                } else {
                    showEmptyState("Error al cargar")
                }
            } catch (e: Exception) {
                showEmptyState("Error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showCard() {
        // 🟢 DESBLOQUEAMOS INTERACCIÓN (Ya cargó la nueva carta)
        isProcessingSwipe = false
        
        if (currentIndex >= flashcards.size) {
            showEmptyState("¡Repaso completado! 🎉")
            setProgressSmoothly(100) 
            return
        }

        // 🔥 ANIMACIÓN DE BARRA DE PROGRESO
        val targetProgress = if (flashcards.size > 0) {
            (currentIndex * 100) / flashcards.size
        } else 0
        setProgressSmoothly(targetProgress)

        val card = flashcards[currentIndex]
        isAnswerRevealed = false

        // Reseteo visual completo de la carta
        cardView.translationX = 0f
        cardView.rotation = 0f
        cardView.alpha = 1f
        cardView.foreground = null
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
    }

    private fun submitReview(quality: Int) {
        val card = flashcards[currentIndex]

        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@FlashcardsActivity)
                val request = FlashcardReviewRequest(quality)
                val response = api.reviewFlashcard(card.wordId, request)

                // Independientemente de si el server responde OK o falla (offline/error),
                // avanzamos a la siguiente carta para que el usuario no se quede atascado.
                // Si quieres ser estricto, mete el avance dentro del 'if (isSuccessful)'
                
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    val realDays = result.intervalDays
                    val message = if (quality >= 3) {
                        "✓ La verás en $realDays ${if (realDays == 1) "día" else "días"}"
                    } else {
                        "✗ La verás mañana"
                    }
                    tvIntervalIndicator.text = message
                } else {
                    // Si falla el server, al menos avisamos pero dejamos continuar
                    Toast.makeText(this@FlashcardsActivity, "Guardado local (Sync pendiente)", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                // Error de red
                Toast.makeText(this@FlashcardsActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                // Avanzamos SIEMPRE tras una pequeña pausa para ver el feedback
                cardView.postDelayed({
                    currentIndex++
                    showCard()
                }, 350)
            }
        }
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
}