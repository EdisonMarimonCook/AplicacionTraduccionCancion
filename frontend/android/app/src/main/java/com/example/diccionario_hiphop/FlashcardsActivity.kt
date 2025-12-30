package com.example.diccionario_hiphop

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
    private lateinit var tvExplanation: TextView
    private lateinit var divider: View
    private lateinit var tvTapHint: TextView
    private lateinit var touchOverlay: View
    private lateinit var layoutSwipeArrows: View
    private lateinit var tvMessage: TextView
    private lateinit var cardView: CardView
    private lateinit var tvIntervalIndicator: TextView
    private lateinit var progressBar: ProgressBar


    // Variables de control de Swipe Físico
    private var dX = 0f
    private var dY = 0f
    private var initialRawX = 0f
    private var isAnswerRevealed = false
    private val SWIPE_THRESHOLD = 300f // Distancia necesaria para considerar swipe

    // Data
    private var flashcards: MutableList<FlashcardData> = mutableListOf()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcards)

           initViews()
        setupCardPhysics() // 🔥 Nueva función de física
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
        progressBar = findViewById(R.id.progressBar)

        touchOverlay.setOnClickListener { revealAnswer() }
    }

    // 🍎 FÍSICA NEWTONIANA (Adiós parpadeos)
    private fun setupCardPhysics() {
        cardView.setOnTouchListener { view, event ->
            // Solo permitimos mover la carta si la respuesta ya se mostró
            if (!isAnswerRevealed) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Guardamos la diferencia entre donde tocamos y la posición de la vista
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    initialRawX = event.rawX
                    true // Consumimos el evento
                }

                MotionEvent.ACTION_MOVE -> {
                    // Calculamos la nueva posición ABSOLUTA
                    val newX = event.rawX + dX
                    val deltaX = event.rawX - initialRawX // Cuánto me he movido desde el inicio

                    // Aplicamos movimiento horizontal
                    view.animate()
                        .x(newX)
                        .setDuration(0) // 0 duración = movimiento instantáneo (seguimiento del dedo)
                        .start()

                    // Rotación suave basada en el desplazamiento
                    // Dividimos por 40 para que no gire demasiado rápido
                    val rotation = (deltaX / 40f).coerceIn(-15f, 15f)
                    view.rotation = rotation

                    // Feedback visual (Verde/Rojo)
                    updateColorFeedback(deltaX)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - initialRawX
                    
                    // Si soltamos, verificamos si cruzamos el umbral
                    if (abs(deltaX) > SWIPE_THRESHOLD) {
                        if (deltaX > 0) {
                            swipeRight() // FÁCIL
                        } else {
                            swipeLeft() // DIFÍCIL
                        }
                    } else {
                        // Si no llegamos lejos, volvemos al centro (efecto muelle)
                        resetCardPosition()
                    }
                    // Limpiamos colores
                    tvIntervalIndicator.visibility = View.GONE
                    cardView.foreground = null
                    true
                }
                else -> false
            }
        }
    }

    private fun updateColorFeedback(deltaX: Float) {
        if (deltaX > 100) {
            // Verde - Derecha
            cardView.foreground = ColorDrawable(Color.parseColor("#4D4CAF50")) // Transparente verde
            tvIntervalIndicator.visibility = View.VISIBLE
            tvIntervalIndicator.text = "FÁCIL ✓"
            tvIntervalIndicator.setBackgroundColor(Color.parseColor("#4CAF50"))
        } else if (deltaX < -100) {
            // Rojo - Izquierda
            cardView.foreground = ColorDrawable(Color.parseColor("#4DF44336")) // Transparente rojo
            tvIntervalIndicator.visibility = View.VISIBLE
            tvIntervalIndicator.text = "DIFÍCIL ✗"
            tvIntervalIndicator.setBackgroundColor(Color.parseColor("#F44336"))
        } else {
            // Zona muerta (centro)
            cardView.foreground = null
            tvIntervalIndicator.visibility = View.GONE
        }
    }

    private fun resetCardPosition() {
        // Animación de retorno elástica (OvershootInterpolator)
        cardView.animate()
            .x(0f) // Volver a X=0 (relativo al padre layout, ajustar si usas constraints complejos)
            .translationX(0f) // Asegurar reset de translation
            .rotation(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.5f)) // Efecto rebote
            .start()
        // Nota: Si usas ConstraintLayout, view.x puede comportarse distinto. 
        // Si ves que se va a la izquierda de la pantalla, usa .translationX(0f) solamente.
        // Aquí forzamos ambas por seguridad:
        cardView.animate().translationX(0f).rotation(0f).setDuration(300).start()
    }

    private fun swipeRight() {
        showIntervalPreview(true)
        // Animamos salida hacia la derecha
        cardView.animate()
            .translationX(1500f) // Fuera de pantalla
            .rotation(20f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    submitReview(4) // 4 = Fácil
                }
            }).start()
    }

    private fun swipeLeft() {
        showIntervalPreview(false)
        // Animamos salida hacia la izquierda
        cardView.animate()
            .translationX(-1500f) // Fuera de pantalla
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
        tvTranslation.text = "📖 ${card.translation}"
        tvExplanation.text = card.explanation ?: "Sin explicación disponible"

        // Ocultar respuesta y flechas
        tvTranslation.visibility = View.INVISIBLE
        tvExplanation.visibility = View.INVISIBLE
        divider.visibility = View.INVISIBLE
        layoutSwipeArrows.visibility = View.INVISIBLE
        touchOverlay.visibility = View.VISIBLE
        tvTapHint.visibility = View.VISIBLE

        tvCounter.text = "${currentIndex + 1} / ${flashcards.size}"
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