package com.example.diccionario_hiphop

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
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
    private lateinit var btnNext: Button
    private lateinit var tvMessage: TextView
    private lateinit var cardView: View

    // Data: Usamos UserWord, NO FlashcardItem
    private var flashcards: MutableList<UserWord> = mutableListOf()
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
        
        btnNext = findViewById(R.id.btnEasy) // Reutilizamos ID existente
        btnNext.text = "Siguiente ➔"
        
        // Ocultar botón hard si existe en el XML
        findViewById<View>(R.id.btnHard)?.visibility = View.GONE

        tvMessage = findViewById(R.id.tvMessage)
        cardView = findViewById(R.id.cardView)

        touchOverlay.setOnClickListener { revealAnswer() }
        btnNext.setOnClickListener { nextCard() }
    }

    private fun loadFlashcards() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@FlashcardsActivity)
                // Usamos getDictionary, no getFlashcardsToReview
                val response = api.getDictionary()

                if (response.isSuccessful && response.body() != null) {
                    val allWords = response.body()!!
                    // Filtramos las que tienen ejemplo
                    flashcards = allWords.filter { !it.example.isNullOrEmpty() }.toMutableList()
                    flashcards.shuffle()

                    if (flashcards.isEmpty()) {
                        showEmptyState("No tienes palabras con ejemplos.")
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
            }
        }
    }

    private fun showCard() {
        if (currentIndex >= flashcards.size) {
            showEmptyState("¡Repaso completado! 🎉")
            return
        }

        val card = flashcards[currentIndex]

        tvWord.text = card.word.replaceFirstChar { it.uppercase() }
        tvContextQuestion.text = "\"${card.example}\""
        tvTranslation.text = card.translation

        tvTranslation.visibility = View.INVISIBLE
        divider.visibility = View.INVISIBLE
        layoutButtons.visibility = View.INVISIBLE
        touchOverlay.visibility = View.VISIBLE
        tvTapHint.visibility = View.VISIBLE

        tvCounter.text = "${currentIndex + 1} / ${flashcards.size}"
    }

    private fun revealAnswer() {
        tvTranslation.visibility = View.VISIBLE
        divider.visibility = View.VISIBLE
        layoutButtons.visibility = View.VISIBLE
        touchOverlay.visibility = View.GONE
        tvTapHint.visibility = View.GONE
    }

    private fun nextCard() {
        currentIndex++
        showCard()
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