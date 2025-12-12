package com.example.diccionario_hiphop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var btnBack: ImageButton
    private lateinit var ivAvatar: ImageView
    private lateinit var fabEditAvatar: FloatingActionButton
    private lateinit var tvUsername: TextView
    private lateinit var tvUserLevel: TextView
    private lateinit var tvWordsCount: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvFlashcardsDone: TextView

    private lateinit var btnEditProfile: Button
    private lateinit var btnMyDictionary: Button
    private lateinit var btnFlashcards: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var tokenManager: TokenManager

    // Para seleccionar imagen de galería
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                // Cargar imagen circular
                ivAvatar.load(imageUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                
                // TODO: Aquí enviarías la imagen al backend
                Toast.makeText(this, "Foto seleccionada (falta subir al servidor)", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    override fun onResume() {
        super.onResume()
        loadLocalUserData()
        loadFullProfileData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivAvatar = findViewById(R.id.ivAvatar)
        fabEditAvatar = findViewById(R.id.fabEditAvatar)
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

        // Botón para cambiar foto de perfil
        fabEditAvatar.setOnClickListener {
            openGallery()
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnMyDictionary.setOnClickListener {
            startActivity(Intent(this, DictionaryActivity::class.java))
        }

        btnFlashcards.setOnClickListener {
            startActivity(Intent(this, FlashcardsActivity::class.java))
        }

        btnLogout.setOnClickListener { performLogout() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadLocalUserData() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "Usuario")
        val userLevel = prefs.getString("USER_LEVEL", "A1")
        
        tvUsername.text = username
        tvUserLevel.text = "Nivel $userLevel"
    }

    private fun loadFullProfileData() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@ProfileActivity)

                // 1️⃣ Obtener Perfil
                val profileRes = api.getProfile()
                if (profileRes.isSuccessful && profileRes.body() != null) {
                    val profile = profileRes.body()!!
                    tvUsername.text = profile.username

                    // Actualizar caché local
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit().putString("username", profile.username).apply()

                    // 🆕 Cargar avatar si existe
                    if (!profile.avatarUrl.isNullOrEmpty()) {
                        ivAvatar.load(profile.avatarUrl) {
                            crossfade(true)
                            placeholder(R.drawable.ic_person)
                            error(R.drawable.ic_person)
                            transformations(CircleCropTransformation())
                        }
                    } else {
                        // Imagen por defecto con transformación circular
                        ivAvatar.load(R.drawable.ic_person) {
                            transformations(CircleCropTransformation())
                        }
                    }
                }

                // 2️⃣ Obtener Estadísticas
                val progressRes = api.getUserProgress()
                if (progressRes.isSuccessful && progressRes.body() != null) {
                    val data = progressRes.body()!!

                    tvWordsCount.text = data.totalWordsLearned.toString()
                    tvStreak.text = "🔥 ${data.currentStreak}"

                    // 🆕 Obtener nivel del usuario desde learning_languages
                    val userLevel = data.learningLanguages.firstOrNull { it.language == "en" }?.level ?: "A1"
                    tvUserLevel.text = "Nivel $userLevel"
                    
                    // Guardar nivel en caché
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit().putString("USER_LEVEL", userLevel).apply()
                }

                // 3️⃣ Obtener estadísticas de flashcards
                val flashcardsRes = api.getFlashcardStats()
                if (flashcardsRes.isSuccessful && flashcardsRes.body() != null) {
                    val stats = flashcardsRes.body()!!
                    
                    // 🔥 CAMBIO: Mostrar pendientes de hoy en lugar de "reviewed"
                    val dueToday = stats["due_today"] ?: 0
                    tvFlashcardsDone.text = dueToday.toString()
                }

            } catch (e: Exception) {
                // Silencioso: si falla la red, el usuario ve los datos locales
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