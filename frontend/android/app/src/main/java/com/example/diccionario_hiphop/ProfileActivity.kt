package com.example.diccionario_hiphop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey // 🔥 AÑADIR ESTA LÍNEA
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvLevel: TextView
    
    // Contadores
    private lateinit var tvWordsCount: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var tvReviewCount: TextView

    private lateinit var btnEditProfile: Button
    private lateinit var btnDictionary: Button
    private lateinit var btnFlashcards: Button
    private lateinit var btnLogout: Button
    private lateinit var btnBack: ImageButton

    // 1. Seleccionar imagen de la galería
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { startCrop(it) } // Al elegir, mandamos a recortar
    }

    // 2. Recibir resultado del recorte (uCrop)
    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uploadAvatar(it) } // Al recortar, subimos
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(this, "Error al recortar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initViews()
        setupListeners()
    }

    // 🔥 Recargar perfil SIEMPRE que se muestre la pantalla (así se actualizan los contadores)
    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun initViews() {
        // ✅ IDs CORREGIDOS SEGÚN TU XML
        ivProfile = findViewById(R.id.ivProfile)
        tvUsername = findViewById(R.id.etUsername) // En XML se llama etUsername
        tvEmail = findViewById(R.id.etEmail)       // En XML se llama etEmail
        tvLevel = findViewById(R.id.tvLevel)
        
        // Contadores (Asegúrate de que estos IDs existen en tu XML, si no, coméntalos)
        tvWordsCount = findViewById(R.id.tvWordsCount) // ID: 0 Palabras
        tvStreakCount = findViewById(R.id.tvStreakCount) // ID: 1 Racha
        tvReviewCount = findViewById(R.id.tvReviewCount) // ID: 0 Repasos

        btnEditProfile = findViewById(R.id.btnSave) // En XML se llama btnSave
        btnDictionary = findViewById(R.id.btnMyDictionary) // En XML se llama btnMyDictionary
        btnFlashcards = findViewById(R.id.btnFlashcards) 
        
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        // 1. Ir a Editar Perfil (Botón "MODIFICAR DATOS")
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivityForResult(intent, 100)  // 🔥 CAMBIAR A startActivityForResult
        }

        // 2. Subir Foto
        ivProfile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 3. Ir al Diccionario
        btnDictionary.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            startActivityForResult(intent, 101)  // 🔥 Código 101 para recargar al volver
        }

        // 4. Ir a Flashcards
        btnFlashcards.setOnClickListener {
            val intent = Intent(this, FlashcardsActivity::class.java)
            startActivityForResult(intent, 102)  // 🔥 Código 102 para recargar al volver
        }

        // 5. Cerrar Sesión
        btnLogout.setOnClickListener {
            val tokenManager = TokenManager(this)
            tokenManager.clearTokens() 
            
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnBack.setOnClickListener { finish() }
    }

    // 🔥 INICIAR RECORTE CON UCROP
    private fun startCrop(uri: Uri) {
        val destinationFileName = "avatar_cropped.jpg"
        val uCrop = UCrop.of(uri, Uri.fromFile(File(cacheDir, destinationFileName)))
        
        uCrop.withAspectRatio(1f, 1f) // Cuadrado perfecto
        uCrop.withMaxResultSize(500, 500) // Tamaño máximo
        
        // Opciones visuales
        val options = UCrop.Options()
        options.setCircleDimmedLayer(true) // Máscara circular
        options.setShowCropGrid(false)
        options.setCompressionQuality(80)
        options.setToolbarTitle("Ajustar Foto")
        
        uCrop.withOptions(options)
        
        cropImageLauncher.launch(uCrop.getIntent(this))
    }

    private var isLoadingProfile = false // Flag para evitar múltiples cargas simultáneas

    private fun loadUserProfile() {
        if (isLoadingProfile) return
        isLoadingProfile = true
        
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ProfileActivity)
                val response = apiService.getProfile()

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!

                    tvUsername.text = user.username
                    tvEmail.text = user.email

                    val currentLang = user.learningLanguages?.firstOrNull()
                    if (currentLang != null) {
                        tvLevel.text = "${currentLang.language.uppercase()} • ${currentLang.level}"
                    } else {
                        tvLevel.text = "EN • A1"
                    }

                    // 🔥 USAR EL CONTADOR DIRECTO (NO learningLanguages[0].wordsLearned)
                    tvWordsCount.text = user.wordsCount.toString()
                    tvStreakCount.text = user.streak.toString()
                    tvReviewCount.text = user.reviewsCount.toString()

                    // FOTO
                    if (!user.avatarUrl.isNullOrEmpty()) {
                        val fullImageUrl = if (user.avatarUrl.startsWith("http")) {
                            user.avatarUrl
                        } else {
                            "${RetrofitService.BASE_URL.removeSuffix("/")}${user.avatarUrl}"
                        }

                        android.util.Log.d("AVATAR_DEBUG", "avatarUrl recibida: ${user.avatarUrl}")
                        android.util.Log.d("AVATAR_DEBUG", "BASE_URL: ${RetrofitService.BASE_URL}")
                        android.util.Log.d("AVATAR_DEBUG", "URL completa: $fullImageUrl")

                        Glide.with(this@ProfileActivity)
                            .load(fullImageUrl)
                            .signature(ObjectKey(System.currentTimeMillis()))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    android.util.Log.e("AVATAR_DEBUG", "❌ Glide falló: ${e?.message}")
                                    e?.logRootCauses("AVATAR_DEBUG")
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    android.util.Log.d("AVATAR_DEBUG", "✅ Glide cargó exitosamente")
                                    return false
                                }
                            })
                            .into(ivProfile)
                    } else {
                        android.util.Log.d("AVATAR_DEBUG", "avatarUrl está vacío o null")
                        ivProfile.setImageResource(R.drawable.ic_person)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PROFILE_ERROR", "Error: ${e.message}")
            } finally {
                isLoadingProfile = false
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        Toast.makeText(this, "Subiendo...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val file = File(uri.path!!)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val apiService = RetrofitService.getInstance(this@ProfileActivity)
                val response = apiService.uploadAvatar(body)

                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "¡Foto actualizada!", Toast.LENGTH_SHORT).show()
                    
                    // 🔥 ESPERAR 1 SEGUNDO PARA QUE LA BD SE ACTUALICE
                    kotlinx.coroutines.delay(1000)
                    
                    // 🔥 RECARGAR PERFIL (ahora con la nueva URL)
                    loadUserProfile()
                } else {
                    Toast.makeText(this@ProfileActivity, "Error al subir", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Fallo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔥 RECARGAR AL VOLVER DE EDITPROFILE, DICTIONARY O FLASHCARDS
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            100 -> loadUserProfile()  // Volvimos de EditProfile
            101 -> loadUserProfile()  // Volvimos de Dictionary (puede haber añadido/borrado palabras)
            102 -> loadUserProfile()  // Volvimos de Flashcards (puede haber hecho repasos)
        }
    }
}