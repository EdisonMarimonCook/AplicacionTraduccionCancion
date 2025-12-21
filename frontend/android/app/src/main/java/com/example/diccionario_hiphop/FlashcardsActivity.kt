package com.example.diccionario_hiphop

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
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
    private lateinit var progressBar: ProgressBar

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
        progressBar = findViewById(R.id.progressBar)

        touchOverlay.setOnClickListener { revealAnswer() }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean = true

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // Mostrar feedback visual mientras arrastra (solo si respuesta revelada)
                if (!isAnswerRevealed) return false

                val diffX = e2.x - (e1?.x ?: 0f)

                // Mover la tarjeta con el dedo
                cardView.translationX = diffX

                // Rotar según la dirección (máximo ±15 grados)
                val rotation = (diffX / 30f).coerceIn(-15f, 15f)
                cardView.rotation = rotation

                // Cambiar overlay de color
                if (diffX > 50) {
                    // Verde para FÁCIL (derecha)
                    cardView.foreground = ColorDrawable(Color.parseColor("#4D4CAF50"))
                    tvIntervalIndicator.visibility = View.VISIBLE
                    tvIntervalIndicator.text = "FÁCIL ✓"
                    tvIntervalIndicator.setBackgroundColor(Color.parseColor("#4CAF50"))
                } else if (diffX < -50) {
                    // Rojo para DIFÍCIL (izquierda)
                    cardView.foreground = ColorDrawable(Color.parseColor("#4DF44336"))
                    tvIntervalIndicator.visibility = View.VISIBLE
                    tvIntervalIndicator.text = "DIFÍCIL ✗"
                    tvIntervalIndicator.setBackgroundColor(Color.parseColor("#F44336"))
                } else {
                    // Sin color si no pasa el umbral
                    cardView.foreground = null
                    tvIntervalIndicator.visibility = View.GONE
                }

                return true
            }

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

                // Reducir umbral a 50 para facilitar en emulador
                if (abs(diffX) > abs(diffY) && abs(diffX) > 50) {
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
                } else {
                    // Si no pasó el umbral, resetear la tarjeta
                    resetCardPosition()
                }
                return false
            }
        })

        // Capturar eventos táctiles en el cardView
        cardView.setOnTouchListener { _, event ->
            val handled = gestureDetector.onTouchEvent(event)

            // Cuando suelta el dedo, resetear si no fue un fling válido
            if (event.action == MotionEvent.ACTION_UP && isAnswerRevealed) {
                if (abs(cardView.translationX) < 50) {
                    resetCardPosition()
                }
            }

            handled
        }
    }

    private fun showIntervalPreview(isEasy: Boolean) {
        val card = flashcards[currentIndex]
        val currentReps = card.repetitions
        val currentInterval = card.interval

        // Calcular intervalo aproximado
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

    private fun resetCardPosition() {
        cardView.animate()
            .translationX(0f)
            .rotation(0f)
            .setDuration(200)
            .start()
        cardView.foreground = null
        tvIntervalIndicator.visibility = View.GONE
    }

    private fun animateSwipeRight() {
        cardView.animate()
            .translationX(1000f)
            .rotation(15f)
            .setDuration(300)
            .start()
    }

    private fun animateSwipeLeft() {
        cardView.animate()
            .translationX(-1000f)
            .rotation(-15f)
            .setDuration(300)
            .start()
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

        // Resetear posición, rotación y overlay de la tarjeta
        cardView.translationX = 0f
        cardView.rotation = 0f
        cardView.alpha = 1f
        cardView.foreground = null
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

                    // Actualizar indicador con tiempo real
                    val realDays = result.intervalDays
                    val message = if (quality >= 3) {
                        "✓ La verás en $realDays ${if (realDays == 1) "día" else "días"}"
                    } else {
                        "✗ La verás mañana"
                    }
                    tvIntervalIndicator.text = message

                    // Esperar a que termine la animación
                    cardView.postDelayed({
                        currentIndex++
                        showCard()
                    }, 350)
                }
            } catch (e: Exception) {
                // Error silencioso, continuar
                cardView.postDelayed({
                    currentIndex++
                    showCard()
                }, 350)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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