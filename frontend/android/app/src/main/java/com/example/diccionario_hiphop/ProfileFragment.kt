package com.example.diccionario_hiphop

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import android.graphics.BitmapFactory
import java.io.FileOutputStream

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    // Vistas 
    private lateinit var ivProfileImage: ImageView
    private lateinit var btnEditProfilePic: ImageView 
    private lateinit var tvUsername: TextView
    private lateinit var tvLevelInfo: TextView
    
    // Contadores
    private lateinit var tvWordCount: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvReviews: TextView

    // Botones grandes
    private lateinit var btnEditProfile: Button
    private lateinit var btnDictionary: Button
    private lateinit var btnFlashcards: Button
    private lateinit var btnLogout: Button

    private var isLoadingProfile = false
    private var currentUser: User? = null  // ✨ Para el BottomSheet

    // 🔥 NUEVO: Primero seleccionamos la imagen
private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let {
        // Redimensionar y luego abrir cropper
        val resizedUri = resizeImageBeforeCrop(it)
        openCropper(resizedUri)
    }
}

// 🔥 NUEVO: Ahora el cropper es un launcher separado
private val cropImage = registerForActivityResult(CropImageContract()) { result ->
    if (result.isSuccessful) {
        val uriContent = result.uriContent
        val uriFilePath = result.getUriFilePath(requireContext())
        val finalUri = uriFilePath?.let { Uri.fromFile(File(it)) } ?: uriContent
        if (finalUri != null) {
            uploadAvatar(finalUri)
        }
    }
}

/**
 * 🔥 Redimensiona la imagen a un tamaño manejable ANTES del crop
 */
private fun resizeImageBeforeCrop(uri: Uri): Uri {
    try {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        if (originalBitmap == null) return uri
        
        // 🔥 DIMENSIONES MÁXIMAS (ajusta según necesites)
        val maxWidth = 1080
        val maxHeight = 1920  // Máximo para portrait
        
        val width = originalBitmap.width
        val height = originalBitmap.height
        
        // Calcular escala para que quepa en los límites
        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height,
            1f  // No agrandar si ya es pequeña
        )
        
        // Si la imagen ya es pequeña, no hacer nada
        if (scale >= 1f) return uri
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        val resizedBitmap = Bitmap.createScaledBitmap(
            originalBitmap, newWidth, newHeight, true
        )
        
        // Guardar en caché temporal
        val tempFile = File(requireContext().cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        outputStream.close()
        
        // Liberar memoria
        originalBitmap.recycle()
        if (resizedBitmap != originalBitmap) {
            resizedBitmap.recycle()
        }
        
        return Uri.fromFile(tempFile)
        
    } catch (e: Exception) {
        android.util.Log.e("ProfileFragment", "Error redimensionando imagen", e)
        return uri  // Si falla, usar la original
    }
}

/**
 * 🔥 Abre el cropper con la imagen ya redimensionada
 */
private fun openCropper(uri: Uri) {
    val options = CropImageOptions(
        cropShape = CropImageView.CropShape.OVAL,
        fixAspectRatio = true,
        aspectRatioX = 1,
        aspectRatioY = 1,
        guidelines = CropImageView.Guidelines.ON,
        outputCompressFormat = Bitmap.CompressFormat.JPEG,
        outputCompressQuality = 90,
        
        // Visual
        activityTitle = "Ajustar Foto",
        toolbarColor = ContextCompat.getColor(requireContext(), R.color.purple_700),
        toolbarTitleColor = Color.WHITE,
        toolbarBackButtonColor = Color.WHITE,
        activityMenuIconColor = Color.WHITE,
        activityBackgroundColor = ContextCompat.getColor(requireContext(), R.color.black),
        
        // Configuración optimizada
        initialCropWindowPaddingRatio = 0.1f,
        autoZoomEnabled = true,
        maxZoom = 4,
        scaleType = CropImageView.ScaleType.FIT_CENTER,
        
        minCropWindowWidth = 100,
        minCropWindowHeight = 100,
        
        showCropOverlay = true,
        allowRotation = true,
        allowFlipping = false
    )
    
    cropImage.launch(CropImageContractOptions(uri, options))  // 🔥 Pasar la URI redimensionada
}
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun initViews(view: View) {
        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        btnEditProfilePic = view.findViewById(R.id.btnEditProfilePic)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvLevelInfo = view.findViewById(R.id.tvLevelInfo)

        tvWordCount = view.findViewById(R.id.tvWordCount)
        tvStreak = view.findViewById(R.id.tvStreak)
        tvReviews = view.findViewById(R.id.tvReviews)

        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnDictionary = view.findViewById(R.id.btnDictionary)
        btnFlashcards = view.findViewById(R.id.btnFlashcards)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    private fun setupListeners() {
        // Editar texto
        btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent) 
        }

        // Editar foto 
        val imageClickListener = View.OnClickListener { startCrop() }
        ivProfileImage.setOnClickListener(imageClickListener)
        btnEditProfilePic.setOnClickListener(imageClickListener)

        // ✨ Click en idioma abre BottomSheet
        tvLevelInfo.setOnClickListener {
            currentUser?.let { user -> showLanguagesBottomSheet(user) }
        }

        // Navegación 
        btnDictionary.setOnClickListener {
            val intent = Intent(requireContext(), DictionaryActivity::class.java)
            startActivity(intent)
        }

        btnFlashcards.setOnClickListener {
            val intent = Intent(requireContext(), FlashcardsActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
    lifecycleScope.launch {
        try {
            val tokenManager = TokenManager(requireContext())
            
            // 1. Limpiar tokens PRIMERO
            tokenManager.clearSession()  // 🔥 CAMBIO AQUÍ
            
            // 2. Delay de seguridad
            delay(300)
            
            // 3. Navegar a Login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            
            // 4. Cerrar actividad actual
            requireActivity().finish()
            
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error en logout", e)
            Toast.makeText(requireContext(), "Error cerrando sesión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


   private fun startCrop() {
    // 🔥 Ahora solo lanzamos el picker de galería
    pickImage.launch("image/*")
}

    private fun loadUserProfile() {
        if (isLoadingProfile) return
        isLoadingProfile = true
        
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(requireContext())
                val response = apiService.getProfile() 

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    currentUser = user  // ✨ Guardar para BottomSheet

                    tvUsername.text = user.username
                    
                    // ✨ MOSTRAR IDIOMA PRINCIPAL
                    val primaryLang = user.learningLanguages?.find { it.language == user.primaryLanguage }
                    if (primaryLang != null) {
                        val flag = getLanguageFlag(primaryLang.language)
                        val langName = getLanguageName(primaryLang.language)
                        tvLevelInfo.text = "$flag $langName • ${primaryLang.level}"
                    } else {
                        tvLevelInfo.text = "EN • A1"
                    }

                    tvWordCount.text = user.wordsCount.toString()
                    tvStreak.text = "🔥 ${user.currentStreak}" 
                    tvReviews.text = user.reviewsCount.toString()

                    if (!user.avatarUrl.isNullOrEmpty()) {
                        val fullImageUrl = if (user.avatarUrl.startsWith("http")) {
                            user.avatarUrl
                        } else {
                            "${RetrofitService.BASE_URL.removeSuffix("/")}${user.avatarUrl}"
                        }

                        if (isAdded) {
                            Glide.with(this@ProfileFragment)
                                .load(fullImageUrl)
                                .signature(ObjectKey(System.currentTimeMillis())) 
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .placeholder(R.drawable.ic_person) 
                                .into(ivProfileImage)
                        }
                    } else {
                        ivProfileImage.setImageResource(R.drawable.ic_person) 
                    }
                }
            } catch (e: Exception) {
                // Error silencioso o log
            } finally {
                isLoadingProfile = false
            }
        }
    }

    // ✨ NUEVO: Mostrar BottomSheet con idiomas
    private fun showLanguagesBottomSheet(user: User) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_languages, null)
        bottomSheetDialog.setContentView(view)

        val rvLanguages = view.findViewById<RecyclerView>(R.id.rvLanguages)
        val tvTotalWords = view.findViewById<TextView>(R.id.tvTotalWords)
        val tvTotalReviews = view.findViewById<TextView>(R.id.tvTotalReviews)
        val btnManageLanguages = view.findViewById<Button>(R.id.btnManageLanguages)

        // Filtrar idiomas activos
        val activeLanguages = user.learningLanguages?.filter { it.isActive } ?: emptyList()

        // Calcular totales
        val totalWords = activeLanguages.sumOf { it.wordsLearned }
        val totalReviews = activeLanguages.sumOf { it.reviewsPending }

        tvTotalWords.text = "Total: $totalWords palabras"
        tvTotalReviews.text = "Repasos pendientes: $totalReviews"

        // Configurar RecyclerView
        rvLanguages.layoutManager = LinearLayoutManager(requireContext())
        rvLanguages.adapter = LanguageItemAdapter(activeLanguages, user.primaryLanguage)

        // Botón gestionar (por ahora solo cierra)
        btnManageLanguages.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(requireContext(), "Gestión de idiomas disponible en futuras versiones", Toast.LENGTH_SHORT).show()
        }

        bottomSheetDialog.show()
    }

    // ✨ NUEVO: Obtener bandera emoji del idioma
    private fun getLanguageFlag(code: String): String {
        return when (code) {
            "en" -> "🇬🇧"
            "es" -> "🇪🇸"
            "fr" -> "🇫🇷"
            "de" -> "🇩🇪"
            "it" -> "🇮🇹"
            "pt" -> "🇵🇹"
            "ja" -> "🇯🇵"
            "zh" -> "🇨🇳"
            "ko" -> "🇰🇷"
            else -> "🌐"
        }
    }

    // ✨ NUEVO: Obtener nombre del idioma
    private fun getLanguageName(code: String): String {
        return when (code) {
            "en" -> "Inglés"
            "es" -> "Español"
            "fr" -> "Francés"
            "de" -> "Alemán"
            "it" -> "Italiano"
            "pt" -> "Portugués"
            "ja" -> "Japonés"
            "zh" -> "Chino"
            "ko" -> "Coreano"
            else -> code.uppercase()
        }
    }

    private fun uploadAvatar(uri: Uri) {
        Toast.makeText(requireContext(), "Subiendo...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                // Truco para obtener el path real desde la URI si es necesario
                val filePath = uri.path ?: return@launch
                val file = File(filePath)
                
                // Nota: A veces con URIs de galería modernas necesitas un ContentResolver para copiar el fichero
                // Si te falla al subir diciendo "File not found", avísame y te paso la función auxiliar
                
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val apiService = RetrofitService.getInstance(requireContext())
                val response = apiService.uploadAvatar(body)

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "¡Foto actualizada!", Toast.LENGTH_SHORT).show()
                    delay(1000)
                    loadUserProfile() 
                } else {
                    Toast.makeText(requireContext(), "Error al subir", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Fallo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ✨ NUEVO: Mostrar BottomSheet con idiomas
    private fun showLanguagesBottomSheet(user: User) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_languages, null)
        bottomSheetDialog.setContentView(view)

        val rvLanguages = view.findViewById<RecyclerView>(R.id.rvLanguages)
        val tvTotalWords = view.findViewById<TextView>(R.id.tvTotalWords)
        val tvTotalReviews = view.findViewById<TextView>(R.id.tvTotalReviews)
        val btnManageLanguages = view.findViewById<Button>(R.id.btnManageLanguages)

        // Filtrar idiomas activos
        val activeLanguages = user.learningLanguages?.filter { it.isActive } ?: emptyList()

        // Calcular totales
        val totalWords = activeLanguages.sumOf { it.wordsLearned }
        val totalReviews = activeLanguages.sumOf { it.reviewsPending }

        tvTotalWords.text = "Total: $totalWords palabras"
        tvTotalReviews.text = "Repasos pendientes: $totalReviews"

        // Configurar RecyclerView
        rvLanguages.layoutManager = LinearLayoutManager(requireContext())
        rvLanguages.adapter = LanguageItemAdapter(activeLanguages, user.primaryLanguage)

        // Botón gestionar (por ahora solo cierra)
        btnManageLanguages.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(requireContext(), "Gestión de idiomas disponible en futuras versiones", Toast.LENGTH_SHORT).show()
        }

        bottomSheetDialog.show()
    }

    // ✨ NUEVO: Obtener bandera emoji del idioma
    private fun getLanguageFlag(code: String): String {
        return when (code) {
            "en" -> "🇬🇧"
            "es" -> "🇪🇸"
            "fr" -> "🇫🇷"
            "de" -> "🇩🇪"
            "it" -> "🇮🇹"
            "pt" -> "🇵🇹"
            "ja" -> "🇯🇵"
            "zh" -> "🇨🇳"
            "ko" -> "🇰🇷"
            else -> "🌐"
        }
    }

    // ✨ NUEVO: Obtener nombre del idioma
    private fun getLanguageName(code: String): String {
        return when (code) {
            "en" -> "Inglés"
            "es" -> "Español"
            "fr" -> "Francés"
            "de" -> "Alemán"
            "it" -> "Italiano"
            "pt" -> "Portugués"
            "ja" -> "Japonés"
            "zh" -> "Chino"
            "ko" -> "Coreano"
            else -> code.uppercase()
        }
    }

    // ✨ NUEVO: Adapter para RecyclerView de idiomas
    inner class LanguageItemAdapter(
        private val languages: List<LearningLanguage>,
        private val primaryLanguageCode: String
    ) : RecyclerView.Adapter<LanguageItemAdapter.LanguageViewHolder>() {

        inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvLanguageName: TextView = view.findViewById(R.id.tvLanguageName)
            val tvPrimaryBadge: TextView = view.findViewById(R.id.tvPrimaryBadge)
            val tvWords: TextView = view.findViewById(R.id.tvWords)
            val tvReviews: TextView = view.findViewById(R.id.tvReviews)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LanguageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
            return LanguageViewHolder(view)
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            val lang = languages[position]
            val flag = getLanguageFlag(lang.language)
            val name = getLanguageName(lang.language)
            
            holder.tvLanguageName.text = "$flag $name • ${lang.level}"
            holder.tvWords.text = "${lang.wordsLearned} palabras"
            holder.tvReviews.text = "${lang.reviewsPending} repasos"
            
            // Mostrar badge si es idioma principal
            if (lang.language == primaryLanguageCode) {
                holder.tvPrimaryBadge.visibility = View.VISIBLE
            } else {
                holder.tvPrimaryBadge.visibility = View.GONE
            }
        }

        override fun getItemCount() = languages.size
    }
}