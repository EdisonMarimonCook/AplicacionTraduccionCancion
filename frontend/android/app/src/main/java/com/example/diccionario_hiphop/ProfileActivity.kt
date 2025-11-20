package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvWordsCount: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvLevel: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar

    // 1️⃣ Declaramos el botón nuevo
    private lateinit var btnFlashcards: Button

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tokenManager = TokenManager(this)

        initViews()
        loadUserProfile()
        setupListeners()
    }

    private fun initViews() {
        tvUsername = findViewById(R.id.tvUsername)
        tvEmail = findViewById(R.id.tvEmail)
        tvWordsCount = findViewById(R.id.tvWordsCount)
        tvStreak = findViewById(R.id.tvStreak)
        tvLevel = findViewById(R.id.tvLevel)
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)

        // 2️⃣ Inicializamos el botón
        btnFlashcards = findViewById(R.id.btnFlashcards)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnLogout.setOnClickListener { performLogout() }

        // 3️⃣ Lógica para abrir Flashcards
        btnFlashcards.setOnClickListener {
            val intent = Intent(this, FlashcardsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserProfile() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ProfileActivity)

                // Llamada al endpoint de progreso
                val response = apiService.getUserProgress()

                if (response.isSuccessful && response.body() != null) {
                    val progress = response.body()!!

                    // Actualizar UI con datos del servidor
                    tvWordsCount.text = progress.totalWordsLearned.toString()
                    tvStreak.text = "🔥 ${progress.currentStreak}"

                    // Obtener nivel de inglés
                    val englishStats = progress.statsByLanguage["en"]
                    tvLevel.text = englishStats?.estimatedLevel ?: "A1"

                    // Datos básicos (placeholder o de prefs)
                    tvUsername.text = "MC Learner"

                } else {
                    // Si falla la carga de datos, mostramos 0 pero no bloqueamos la app
                    tvWordsCount.text = "0"
                    tvStreak.text = "🔥 0"
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Sin conexión: mostrando datos locales", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun performLogout() {
        // 1. Borrar token
        tokenManager.clearSession()

        // 2. Borrar preferencias
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

        // 3. Ir al Login y limpiar historial
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}