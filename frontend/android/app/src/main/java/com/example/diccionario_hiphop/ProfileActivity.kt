package com.example.diccionario_hiphop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
// 🔥 MIGRACIÓN A CANHUB (Adiós uCrop)
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvLevel: TextView
    
    private lateinit var tvWordsCount: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var tvReviewCount: TextView

    private lateinit var btnEditProfile: Button
    private lateinit var btnDictionary: Button
    private lateinit var btnFlashcards: Button
    private lateinit var btnLogout: Button
    private lateinit var btnBack: ImageButton

    // 🔥 NUEVO LANZADOR "TODO EN UNO" (Cámara + Galería + Recorte)
    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            val uriFilePath = result.getUriFilePath(this) // Path real para subir el archivo
            
            if (uriFilePath != null) {
                // Creamos un Uri desde el path para reutilizar tu función uploadAvatar
                val fileUri = Uri.fromFile(File(uriFilePath))
                uploadAvatar(fileUri)
            }
        } else {
            val exception = result.error
            // Ignoramos el error si el usuario canceló (exception suele ser null o "User cancelled")
            if (exception != null) {
                 Toast.makeText(this, "Cancelado o Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun initViews() {
        ivProfile = findViewById(R.id.ivProfile)
        tvUsername = findViewById(R.id.etUsername)
        tvEmail = findViewById(R.id.etEmail)
        tvLevel = findViewById(R.id.tvLevel)
        
        tvWordsCount = findViewById(R.id.tvWordsCount)
        tvStreakCount = findViewById(R.id.tvStreakCount)
        tvReviewCount = findViewById(R.id.tvReviewCount)

        btnEditProfile = findViewById(R.id.btnSave)
        btnDictionary = findViewById(R.id.btnMyDictionary)
        btnFlashcards = findViewById(R.id.btnFlashcards)
        
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupListeners() {
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivityForResult(intent, 100)
        }

        // 🔥 AL PULSAR LA FOTO, LANZAMOS EL NUEVO CROPPER
        ivProfile.setOnClickListener {
            startCrop()
        }

        btnDictionary.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            startActivityForResult(intent, 101)
        }

        btnFlashcards.setOnClickListener {
            val intent = Intent(this, FlashcardsActivity::class.java)
            startActivityForResult(intent, 102)
        }

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

    // 🔥 CONFIGURACIÓN DE CANHUB CROPPER
    private fun startCrop() {
        cropImage.launch(
            CropImageContractOptions(
                uri = null, // null = Preguntar al usuario
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeGallery = true,
                    imageSourceIncludeCamera = false, // 🔥 FIX: SOLO GALERÍA (Evita pantalla intermedia)
                    
                    // Configuración visual (Tus colores)
                    activityBackgroundColor = ContextCompat.getColor(this, R.color.bg_page),
                    toolbarColor = ContextCompat.getColor(this, R.color.purple_500),
                    toolbarTitleColor = ContextCompat.getColor(this, R.color.white),
                    
                    // 🔥 FIX IMPORTANTE: Asegurar que los iconos de Confirmar/Cancelar sean BLANCOS
                    activityMenuIconColor = ContextCompat.getColor(this, R.color.white),
                    toolbarBackButtonColor = ContextCompat.getColor(this, R.color.white),
                    
                    // Forma del recorte
                    cropShape = CropImageView.CropShape.OVAL, // ¡Queda mejor para perfiles!
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true,
                    
                    // Calidad
                    outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG,
                    outputCompressQuality = 90
                )
            )
        )
    }

    private var isLoadingProfile = false 

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

                    tvWordsCount.text = user.wordsCount.toString()
                    tvStreakCount.text = user.streak.toString()
                    tvReviewCount.text = user.reviewsCount.toString()

                    if (!user.avatarUrl.isNullOrEmpty()) {
                        val fullImageUrl = if (user.avatarUrl.startsWith("http")) {
                            user.avatarUrl
                        } else {
                            "${RetrofitService.BASE_URL.removeSuffix("/")}${user.avatarUrl}"
                        }

                        Glide.with(this@ProfileActivity)
                            .load(fullImageUrl)
                            .signature(ObjectKey(System.currentTimeMillis()))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(ivProfile)
                    } else {
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
                // CanHub a veces devuelve file:// o content://
                // Esta lógica asegura que tenemos un archivo válido
                val file = File(uri.path ?: return@launch)
                
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val apiService = RetrofitService.getInstance(this@ProfileActivity)
                val response = apiService.uploadAvatar(body)

                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "¡Foto actualizada!", Toast.LENGTH_SHORT).show()
                    kotlinx.coroutines.delay(1000)
                    loadUserProfile()
                } else {
                    Toast.makeText(this@ProfileActivity, "Error al subir", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Fallo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            100 -> loadUserProfile()  
            101 -> loadUserProfile()  
            102 -> loadUserProfile()  
        }
    }
}