package com.example.diccionario_hiphop

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FlashcardsActivity : AppCompatActivity() {

    // Views
    private lateinit var tvCounter: TextView
    private lateinit var tvWord: TextView
    private lateinit var tvContextQuestion: TextView
    private lateinit var tvTranslation: TextView
    private lateinit var divider: View
    private lateinit var tvTapHint: TextView
    private lateinit var touchOverlay: View
    private lateinit var layoutButtons: View
    private lateinit var btnHard: Button
    private lateinit var btnEasy: Button
    private lateinit var tvMessage: TextView
    private lateinit var cardView: View

    // Data
    private var flashcards: MutableList<FlashcardItem> = mutableListOf()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcards)

        initViews()
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
        layoutButtons = findViewById(R.id.layoutButtons)
        btnHard = findViewById(R.id.btnHard)
        btnEasy = findViewById(R.id.btnEasy)
        tvMessage = findViewById(R.id.tvMessage)
        cardView = findViewById(R.id.cardView)

        // Al tocar la tarjeta, revelar respuesta
        touchOverlay.setOnClickListener {
            revealAnswer()
        }

        // Botones de evaluación
        btnHard.setOnClickListener { submitReview(false) }
        btnEasy.setOnClickListener { submitReview(true) }
    }

    private fun loadFlashcards() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@FlashcardsActivity)
                val response = api.getFlashcardsToReview()

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    flashcards = data.flashcards.toMutableList()

                    if (flashcards.isEmpty()) {
                        showEmptyState("¡No tienes repaso pendiente hoy!\nVuelve mañana.")
                    } else {
                        showGameUI()
                        currentIndex = 0
                        showCard()
                    }
                } else {
                    showEmptyState("Error al cargar tarjetas")
                }
            } catch (e: Exception) {
                showEmptyState("Error de conexión")
            }
        }
    }

    private fun showCard() {
        if (currentIndex >= flashcards.size) {
            showEmptyState("¡Repaso completado!\n🎉")
            return
        }

        val card = flashcards[currentIndex]

        // Reset UI (Estado "Pregunta")
        tvWord.text = card.word
        tvContextQuestion.text = "\"${card.context}\""
        tvTranslation.text = card.translation

        tvTranslation.visibility = View.INVISIBLE
        divider.visibility = View.INVISIBLE
        layoutButtons.visibility = View.INVISIBLE
        touchOverlay.visibility = View.VISIBLE // Habilitar toque para revelar
        tvTapHint.visibility = View.VISIBLE

        tvCounter.text = "${currentIndex + 1} / ${flashcards.size}"
    }

    private fun revealAnswer() {
        // Estado "Respuesta"
        tvTranslation.visibility = View.VISIBLE
        divider.visibility = View.VISIBLE
        layoutButtons.visibility = View.VISIBLE

        touchOverlay.visibility = View.GONE // Deshabilitar toque
        tvTapHint.visibility = View.GONE
    }

    private fun submitReview(isCorrect: Boolean) {
        val card = flashcards[currentIndex]
        val resultStr = if (isCorrect) "correct" else "incorrect"
        val request = ReviewRequest(resultStr)

        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@FlashcardsActivity)
                api.submitReview(card.cardId, request)

                // Pasamos a la siguiente independientemente del resultado de la API
                // (Optimistic UI update)
                currentIndex++
                showCard()

            } catch (e: Exception) {
                Toast.makeText(this@FlashcardsActivity, "Error al guardar progreso", Toast.LENGTH_SHORT).show()
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
        layoutButtons.visibility = View.GONE
    }

    private fun showGameUI() {
        tvMessage.visibility = View.GONE
        cardView.visibility = View.VISIBLE
    }
}