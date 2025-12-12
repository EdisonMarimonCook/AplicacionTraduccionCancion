package com.example.diccionario_hiphop

import android.content.Intent
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

class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var spinnerLevel: Spinner
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupSpinner()
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
        val levels = arrayOf(
            "Selecciona nivel de Inglés", // Placeholder
            "A1 - Principiante",
            "A2 - Básico",
            "B1 - Intermedio",
            "B2 - Intermedio Alto",
            "C1 - Avanzado",
            "C2 - Experto"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLevel.adapter = adapter
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
        val selectedLevelCode = getSelectedLevelCode()

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLevelCode == null) {
            Toast.makeText(this, "Selecciona tu nivel de Inglés", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@RegisterActivity)

                val primaryLanguage = LearningLanguageRequest(
                    language = "en",
                    level = selectedLevelCode,
                    startedAt = null
                )

                val request = RegisterRequest(
                    email = email,
                    username = username,
                    fullName = fullName,
                    password = password,
                    nativeLanguage = "es",
                    learningLanguages = listOf(primaryLanguage)
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

    private fun getSelectedLevelCode(): String? {
        return when (spinnerLevel.selectedItemPosition) {
            1 -> "A1"; 2 -> "A2"; 3 -> "B1"; 4 -> "B2"; 5 -> "C1"; 6 -> "C2"
            else -> null
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
    }
}