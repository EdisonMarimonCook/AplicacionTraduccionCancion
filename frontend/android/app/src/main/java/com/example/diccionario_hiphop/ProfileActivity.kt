package com.example.diccionario_hiphop

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var btnBack: ImageButton
    private lateinit var tvUsername: TextView
    private lateinit var tvUserLevel: TextView
    private lateinit var tvWordsCount: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvFlashcardsDone: TextView

    private lateinit var btnEditProfile: Button // 🔥 Botón para ir a Editar
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

        // Carga inicial
        loadLocalUserData()
        loadFullProfileData()
    }

    // 🔥 IMPORTANTE: Recargar datos al volver de "Modificar Datos"
    // Esto asegura que si cambiaste el nombre, se vea actualizado al instante.
    override fun onResume() {
        super.onResume()
        loadLocalUserData() // Carga rápida (caché)
        loadFullProfileData() // Carga real (servidor)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvUsername = findViewById(R.id.tvUsername)
        tvUserLevel = findViewById(R.id.tvUserLevel)
        tvWordsCount = findViewById(R.id.tvWordsCount)
        tvStreak = findViewById(R.id.tvStreak)
        tvFlashcardsDone = findViewById(R.id.tvFlashcardsDone)

        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnMyDictionary = findViewById(R.id.btnMyDictionary)
        btnFlashcards = findViewById(R.id.btnFlashcards)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // 1. Ir a Modificar Datos
        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // 2. Ir al Diccionario
        btnMyDictionary.setOnClickListener {
            startActivity(Intent(this, DictionaryActivity::class.java))
        }

        // 3. Ir a Flashcards
        btnFlashcards.setOnClickListener {
            startActivity(Intent(this, FlashcardsActivity::class.java))
        }

        // 4. Cerrar Sesión
        btnLogout.setOnClickListener { performLogout() }
    }

    private fun loadLocalUserData() {
        // Recuperamos el nombre guardado en las preferencias locales para mostrar algo rápido
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "Usuario")
        tvUsername.text = username
    }

    private fun loadFullProfileData() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@ProfileActivity)

                // 1️⃣ Obtener Perfil (Nombre actualizado)
                val profileRes = api.getProfile()
                if (profileRes.isSuccessful && profileRes.body() != null) {
                    val profile = profileRes.body()!!
                    tvUsername.text = profile.username

                    // Actualizar caché local
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit().putString("USERNAME", profile.username).apply()
                }

                // 2️⃣ Obtener Estadísticas (Progreso)
                val progressRes = api.getUserProgress()
                if (progressRes.isSuccessful && progressRes.body() != null) {
                    val data = progressRes.body()!!

                    tvWordsCount.text = data.totalWordsLearned.toString()
                    tvStreak.text = "🔥 ${data.currentStreak}"
                    tvFlashcardsDone.text = "0" // Placeholder

                    // Nivel calculado según palabras aprendidas
                    val level = if (data.totalWordsLearned > 50) "B1" else "A2"
                    tvUserLevel.text = "Nivel $level"
                }

            } catch (e: Exception) {
                // Silencioso: Si falla la red, el usuario ve los datos locales
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