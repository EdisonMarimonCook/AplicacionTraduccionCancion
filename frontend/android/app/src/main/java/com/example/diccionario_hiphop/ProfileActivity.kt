package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var btnBack: ImageButton
    private lateinit var tvUsername: TextView
    private lateinit var tvUserLevel: TextView

    // Stats Cards
    private lateinit var tvWordsCount: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvFlashcardsDone: TextView

    // Menu Buttons
    private lateinit var btnMyDictionary: Button
    private lateinit var btnFlashcards: Button
    private lateinit var btnLogout: Button

    private lateinit var progressBar: ProgressBar
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tokenManager = TokenManager(this)

        initViews()
        setupListeners()

        // Cargar datos del usuario
        loadUserData()
        // Cargar estadísticas del servidor
        loadUserProgress()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvUsername = findViewById(R.id.tvUsername)
        tvUserLevel = findViewById(R.id.tvUserLevel)

        tvWordsCount = findViewById(R.id.tvWordsCount)
        tvStreak = findViewById(R.id.tvStreak)
        tvFlashcardsDone = findViewById(R.id.tvFlashcardsDone)

        btnMyDictionary = findViewById(R.id.btnMyDictionary)
        btnFlashcards = findViewById(R.id.btnFlashcards)
        btnLogout = findViewById(R.id.btnLogout)

        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnMyDictionary.setOnClickListener {
            startActivity(Intent(this, DictionaryActivity::class.java))
        }

        btnFlashcards.setOnClickListener {
            startActivity(Intent(this, FlashcardsActivity::class.java))
        }

        btnLogout.setOnClickListener { performLogout() }
    }

    private fun loadUserData() {
        // Recuperar nombre guardado en Login (si lo guardaste en prefs)
        // Si no, usamos un valor por defecto
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = prefs.getString("username", "MC Learner")
        tvUsername.text = username
    }

    private fun loadUserProgress() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@ProfileActivity)

                // Llamada al endpoint de progreso
                val response = api.getUserProgress()

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // 1. Total Palabras Aprendidas
                    tvWordsCount.text = data.totalWordsLearned.toString()

                    // 2. Racha Actual (Días seguidos)
                    tvStreak.text = "🔥 ${data.currentStreak}"

                    // 3. Flashcards (Usamos placeholder si la API no lo devuelve aún)
                    // Si tu backend devuelve este dato, úsalo aquí.
                    tvFlashcardsDone.text = "0"

                    // 4. Nivel estimado de inglés (del mapa stats_by_language)
                    val enStats = data.statsByLanguage["en"]
                    val level = enStats?.estimatedLevel ?: "A1"
                    tvUserLevel.text = "Nivel $level"

                } else {
                    Toast.makeText(this@ProfileActivity, "No se pudieron actualizar las estadísticas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun performLogout() {
        tokenManager.forceLogout()
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}