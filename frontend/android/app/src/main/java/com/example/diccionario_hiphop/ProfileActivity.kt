package com.example.diccionario_hiphop

import android.content.Context
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

    // UI Elements
    private lateinit var btnBack: ImageButton
    private lateinit var tvUsername: TextView // 🔥 Aquí mostraremos el nombre real
    private lateinit var tvUserLevel: TextView
    private lateinit var tvWordsCount: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvFlashcardsDone: TextView
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

        // 1️⃣ Cargar nombre del usuario desde memoria local (Instantáneo)
        loadLocalUserData()

        // 2️⃣ Cargar estadísticas desde el servidor (Asíncrono)
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

    private fun loadLocalUserData() {
        // Recuperar nombre guardado en Login
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "Usuario")
        tvUsername.text = username
    }

    private fun loadUserProgress() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@ProfileActivity)
                val response = api.getUserProgress()

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Actualizar contadores
                    tvWordsCount.text = data.totalWordsLearned.toString()
                    tvStreak.text = "🔥 ${data.currentStreak}"
                    tvFlashcardsDone.text = "0" // Placeholder si la API no lo devuelve

                    // Calcular nivel (MVP: Hardcoded o basado en palabras)
                    // Como quitamos statsByLanguage del modelo simple, usamos una lógica básica
                    val level = if (data.totalWordsLearned > 50) "B1" else "A2"
                    tvUserLevel.text = "Nivel $level"

                } else {
                    Toast.makeText(this@ProfileActivity, "No se pudieron actualizar stats", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Si falla la red, no pasa nada grave, el nombre ya se cargó localmente
                Toast.makeText(this@ProfileActivity, "Error de conexión al perfil", Toast.LENGTH_SHORT).show()
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