package com.example.diccionario_hiphop

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class FlashcardsActivity : AppCompatActivity() {

    // Views
    private lateinit var tvCounter: TextView
    private lateinit var tvWord: TextView
    private lateinit var tvContextQuestion: TextView
    private lateinit var tvTranslation: TextView
    private lateinit var divider: View
    private lateinit var tvTapHint: TextView
    private lateinit var touchOverlay: View
    private lateinit var layoutSwipeArrows: View
    private lateinit var tvMessage: TextView
    private lateinit var cardView: CardView
    private lateinit var tvIntervalIndicator: TextView

    // Gesture detector para swipe
    private lateinit var gestureDetector: GestureDetector
    private var isAnswerRevealed = false

    // Data
    private var flashcards: MutableList<FlashcardData> = mutableListOf()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcards)

        initViews()
        setupGestureDetector()
        loadFlashcards()

        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }
    }

    private fun initViews() {
        tvCounter = findViewById(R.id.tvCounter)
        tvWord = findViewById(R.id.tvWord)
        tvContextQuestion = findViewById(R.id.tvContextQuestion)
        tvTranslation = findViewById(R.id.tvTranslation)
        divider = findViewById(R.id.divider)
        tvTapHint = findViewById(R.id.tvTapHint)
        touchOverlay = findViewById(R.id.touchOverlay)
        layoutSwipeArrows = findViewById(R.id.layoutSwipeArrows)
        tvMessage = findViewById(R.id.tvMessage)
        cardView = findViewById(R.id.cardView)
        tvIntervalIndicator = findViewById(R.id.tvIntervalIndicator)

        touchOverlay.setOnClickListener { revealAnswer() }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Solo permitir swipe si la respuesta está revelada
                if (!isAnswerRevealed) return false

                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)

                // Verificar que sea principalmente horizontal
                if (abs(diffX) > abs(diffY) && abs(diffX) > 100) {
                    if (diffX > 0) {
                        // Swipe derecha → FÁCIL
                        showIntervalPreview(true)
                        animateSwipeRight()
                        submitReview(4)
                    } else {
                        // Swipe izquierda → DIFÍCIL
                        showIntervalPreview(false)
                        animateSwipeLeft()
                        submitReview(1)
                    }
                    return true
                }
                return false
            }
        })

        // Interceptar toques en la tarjeta para detectar swipes
        cardView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Permitir que otros listeners también reciban el evento
        }
    }

    private fun showIntervalPreview(isEasy: Boolean) {
        // Mostrar predicción de cuándo se verá de nuevo
        val card = flashcards[currentIndex]
        val currentReps = card.repetitions
        val currentInterval = card.interval
        
        // Calcular intervalo aproximado (sin llamar al backend todavía)
        val estimatedDays = if (isEasy) {
            // Fácil: avanza según SM-2
            if (currentReps == 0) 1
            else if (currentReps == 1) 6
            else (currentInterval * 2.5).toInt().coerceAtLeast(7)
        } else {
            // Difícil: resetea
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

        // Ocultar después de 2 segundos
        tvIntervalIndicator.postDelayed({
            tvIntervalIndicator.visibility = View.GONE
        }, 2000)
    }

    private fun animateSwipeRight() {
        // Animación de deslizamiento a la derecha
        val animator = ObjectAnimator.ofFloat(cardView, "translationX", 0f, 1000f)
        animator.duration = 300
        animator.start()
    }

    private fun animateSwipeLeft() {
        // Animación de deslizamiento a la izquierda
        val animator = ObjectAnimator.ofFloat(cardView, "translationX", 0f, -1000f)
        animator.duration = 300
        animator.start()
    }

    private fun loadFlashcards() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@FlashcardsActivity)
                val response = api.getDueFlashcards()

                if (response.isSuccessful && response.body() != null) {
                    flashcards = response.body()!!.toMutableList()

                    if (flashcards.isEmpty()) {
                        showEmptyState("¡No tienes repasos pendientes! 🎉")
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
        if (currentIndex >= flashcards.size) {
            showEmptyState("¡Repaso completado! 🎉")
            return
        }

        val card = flashcards[currentIndex]
        isAnswerRevealed = false

        // Resetear posición de la tarjeta (por si venía de animación)
        cardView.translationX = 0f
        cardView.alpha = 1f
        tvIntervalIndicator.visibility = View.GONE

        tvWord.text = card.word.replaceFirstChar { it.uppercase() }
        tvContextQuestion.text = "\"${card.example}\""
        tvTranslation.text = card.translation

        // Ocultar respuesta y flechas
        tvTranslation.visibility = View.INVISIBLE
        divider.visibility = View.INVISIBLE
        layoutSwipeArrows.visibility = View.INVISIBLE
        touchOverlay.visibility = View.VISIBLE
        tvTapHint.visibility = View.VISIBLE

        tvCounter.text = "${currentIndex + 1} / ${flashcards.size}"
    }

    private fun revealAnswer() {
        isAnswerRevealed = true

        tvTranslation.visibility = View.VISIBLE
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

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    
                    // Actualizar el indicador con el tiempo REAL del backend
                    val realDays = result.intervalDays
                    val message = when {
                        realDays == 0 -> "La verás hoy mismo"
                        realDays == 1 -> "La verás mañana"
                        else -> "La verás en $realDays días"
                    }
                    
                    tvIntervalIndicator.text = message
                    
                    // Mostrar Toast adicional con el mensaje completo del backend
                    Toast.makeText(this@FlashcardsActivity, result.message, Toast.LENGTH_SHORT).show()
                    
                    // Esperar un poco para que se vea la animación y el indicador
                    cardView.postDelayed({
                        currentIndex++
                        showCard()
                    }, 2000) // 2 segundos para leer el indicador
                } else {
                    Toast.makeText(this@FlashcardsActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                    // Resetear posición si falla
                    cardView.translationX = 0f
                    tvIntervalIndicator.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@FlashcardsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                cardView.translationX = 0f
                tvIntervalIndicator.visibility = View.GONE
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            tvMessage.text = "Cargando..."
            tvMessage.visibility = View.VISIBLE
            cardView.visibility = View.INVISIBLE
        } else {
            tvMessage.visibility = View.GONE
        }
    }

    private fun showEmptyState(msg: String) {
        tvMessage.text = msg
        tvMessage.visibility = View.VISIBLE
        cardView.visibility = View.GONE
    }

    private fun showGameUI() {
        tvMessage.visibility = View.GONE
        cardView.visibility = View.VISIBLE
    }
}