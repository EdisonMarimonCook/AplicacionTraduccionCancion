package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var spinnerNativeLanguage: Spinner
    private lateinit var llLanguageCheckboxes: LinearLayout
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView
    private lateinit var progressBar: ProgressBar

    // Estructura para almacenar idiomas seleccionados
    private val selectedLanguages = mutableListOf<LanguageSelection>()

    data class LanguageSelection(
        val code: String,
        val name: String,
        val level: String
    )

    // Idiomas soportados (de level_mapper.py)
    private val supportedLanguages = listOf(
        "en" to "🇬🇧 Inglés",
        "es" to "🇪🇸 Español",
        "fr" to "🇫🇷 Francés",
        "de" to "🇩🇪 Alemán",
        "it" to "🇮🇹 Italiano",
        "pt" to "🇵🇹 Portugués",
        "ja" to "🇯🇵 Japonés",
        "zh" to "🇨🇳 Chino",
        "ko" to "🇰🇷 Coreano"
    )

    // Niveles por sistema
    private val cefrLevels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    private val jlptLevels = listOf("N5", "N4", "N3", "N2", "N1")
    private val hskLevels = listOf("1", "2", "3", "4", "5", "6")
    private val topikLevels = listOf("1", "2", "3", "4", "5", "6")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupNativeLanguageSpinner()
        setupLanguageCheckboxes()
        setupListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        spinnerNativeLanguage = findViewById(R.id.spinnerNativeLanguage)
        llLanguageCheckboxes = findViewById(R.id.llLanguageCheckboxes)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupNativeLanguageSpinner() {
        val nativeLanguages = listOf("Selecciona tu idioma nativo") + supportedLanguages.map { it.second }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nativeLanguages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNativeLanguage.adapter = adapter
        
        // Pre-seleccionar Español por defecto
        val spanishIndex = supportedLanguages.indexOfFirst { it.first == "es" } + 1
        spinnerNativeLanguage.setSelection(spanishIndex)
        
        // 🔥 Listener para refrescar checkboxes cuando cambia idioma nativo
        spinnerNativeLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setupLanguageCheckboxes()  // Refrescar lista
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupLanguageCheckboxes() {
        // 🔥 Limpiar checkboxes anteriores
        llLanguageCheckboxes.removeAllViews()
        
        // 🔥 Obtener idioma nativo seleccionado
        val nativeLanguageIndex = spinnerNativeLanguage.selectedItemPosition - 1
        val nativeLanguageCode = if (nativeLanguageIndex >= 0) {
            supportedLanguages[nativeLanguageIndex].first
        } else {
            null
        }
        
        supportedLanguages.forEach { (code, name) ->
            // 🔥 Saltar si es el idioma nativo
            if (code == nativeLanguageCode) {
                return@forEach
            }
            
            // Crear fila horizontal para checkbox + spinner
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 12
                }
            }

            // Checkbox del idioma
            val checkbox = CheckBox(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Spinner de nivel (inicialmente oculto)
            val levelSpinner = Spinner(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0.7f
                )
                visibility = View.GONE
            }

            // Configurar niveles según el idioma
            val levels = when (code) {
                "ja" -> jlptLevels
                "zh" -> hskLevels
                "ko" -> topikLevels
                else -> cefrLevels
            }
            val levelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
            levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            levelSpinner.adapter = levelAdapter

            // Listener: mostrar/ocultar spinner y actualizar lista
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                levelSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE

                if (isChecked) {
                    // Nivel por defecto según el sistema
                    val defaultLevel = when (code) {
                        "ja" -> "N5"
                        "zh", "ko" -> "1"
                        else -> "A1"
                    }
                    selectedLanguages.add(LanguageSelection(code, name, defaultLevel))
                } else {
                    // Remover
                    selectedLanguages.removeAll { it.code == code }
                }
            }

            // Listener del spinner: actualizar nivel seleccionado
            levelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val currentLevels = when (code) {
                        "ja" -> jlptLevels
                        "zh" -> hskLevels
                        "ko" -> topikLevels
                        else -> cefrLevels
                    }
                    val selectedLevel = currentLevels[position]
                    // Actualizar en la lista
                    selectedLanguages.find { it.code == code }?.let {
                        val index = selectedLanguages.indexOf(it)
                        selectedLanguages[index] = it.copy(level = selectedLevel)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            rowLayout.addView(checkbox)
            rowLayout.addView(levelSpinner)
            llLanguageCheckboxes.addView(rowLayout)
        }
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener { performRegister() }
        tvLoginLink.setOnClickListener { finish() }
    }

    private fun performRegister() {
        val fullName = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validaciones
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar idioma nativo seleccionado
        if (spinnerNativeLanguage.selectedItemPosition == 0) {
            Toast.makeText(this, "Selecciona tu idioma nativo", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar al menos 1 idioma de aprendizaje
        if (selectedLanguages.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un idioma para aprender", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@RegisterActivity)

                // Obtener código del idioma nativo
                val nativeLanguageCode = supportedLanguages[spinnerNativeLanguage.selectedItemPosition - 1].first

                // Convertir selectedLanguages a formato API
                val learningLanguages = selectedLanguages.map { lang ->
                    LearningLanguageRequest(
                        language = lang.code,
                        level = lang.level,
                        startedAt = null
                    )
                }

                val request = RegisterRequest(
                    email = email,
                    username = username,
                    fullName = fullName,
                    password = password,
                    nativeLanguage = nativeLanguageCode,
                    learningLanguages = learningLanguages
                )

                val response = apiService.register(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "📧 ¡Cuenta creada! Revisa tu email para verificar.",
                        Toast.LENGTH_LONG
                    ).show()

                    // ✅ NAVEGAR A PANTALLA DE VERIFICACIÓN
                    val intent = Intent(this@RegisterActivity, VerifyAccountActivity::class.java)
                    intent.putExtra("email", email)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(this@RegisterActivity, "Error: $errorBody", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                val msg = when (e) {
                    is IOException -> "Error de conexión"
                    is HttpException -> "Error del servidor: ${e.message}"
                    else -> "Error: ${e.message}"
                }
                Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
    }
}