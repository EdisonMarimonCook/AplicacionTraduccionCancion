package com.example.diccionario_hiphop

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    // Elementos de la UI
    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var spinnerLevel: Spinner // Spinner para el nivel
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupSpinner() // Configuramos las opciones del spinner
        setupListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        spinnerLevel = findViewById(R.id.spinnerLevel)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinner() {
        // Definimos los niveles disponibles
        val levels = arrayOf(
            "Selecciona tu nivel", // Posición 0
            "A1 - Principiante",   // Posición 1
            "A2 - Básico",         // ...
            "B1 - Intermedio",
            "B2 - Intermedio Alto",
            "C1 - Avanzado",
            "C2 - Experto"
        )

        // Creamos el adaptador para mostrar la lista
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLevel.adapter = adapter
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            performRegister()
        }

        tvLoginLink.setOnClickListener {
            finish() // Vuelve a la pantalla de Login
        }
    }

    private fun performRegister() {
        val fullName = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Obtenemos el código del nivel (ej: "A1") basado en la selección
        val selectedLevelCode = getSelectedLevelCode()

        // --- VALIDACIONES ---
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLevelCode == null) {
            Toast.makeText(this, "Debes seleccionar un nivel de inglés", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // --- LLAMADA A LA API ---
        setLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@RegisterActivity)

                // 1. Generar fecha actual ISO 8601
                val currentDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

                // 2. Crear la estructura compleja de idiomas que pide el backend
                val learningLanguages = listOf(
                    LearningLanguage(
                        language = "en",
                        level = selectedLevelCode,
                        startedAt = currentDate
                    )
                )

                // 3. Crear el objeto de petición
                val request = RegisterRequest(
                    email = email,
                    username = username,
                    fullName = fullName,
                    password = password,
                    learningLanguages = learningLanguages
                )

                val response = apiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@RegisterActivity, "¡Cuenta creada con éxito!", Toast.LENGTH_LONG).show()
                    finish() // Volver al login automáticamente
                } else {
                    // Intentar leer el error del servidor
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(this@RegisterActivity, "Error: $errorBody", Toast.LENGTH_LONG).show()
                }

            } catch (e: IOException) {
                Toast.makeText(this@RegisterActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Toast.makeText(this@RegisterActivity, "Error del servidor: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    // Helper para traducir la posición del spinner al código de nivel
    private fun getSelectedLevelCode(): String? {
        return when (spinnerLevel.selectedItemPosition) {
            1 -> "A1"
            2 -> "A2"
            3 -> "B1"
            4 -> "B2"
            5 -> "C1"
            6 -> "C2"
            else -> null // Posición 0 es el placeholder
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
        etFullName.isEnabled = !isLoading
        etUsername.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
        spinnerLevel.isEnabled = !isLoading
    }
}