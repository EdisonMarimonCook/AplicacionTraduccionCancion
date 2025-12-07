package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar TokenManager
        tokenManager = TokenManager(this)

        // 2. Auto-Login: Si hay token, vamos dentro
        if (tokenManager.getToken() != null) {
            navigateToSongSelection()
            return
        }

        setContentView(R.layout.activity_main)
        initViews()
        setupValidations()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        passwordLayout = findViewById(R.id.tilPassword)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister = findViewById(R.id.tvRegister)
    }

    private fun setupValidations() {
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateEmail() }
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validatePassword() }
        })
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            if (validateForm()) {
                performLogin()
            }
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        setLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@MainActivity)
                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!

                    // 1. Guardar Tokens
                    tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)

                    // 2. Guardar datos de usuario (Estructura plana corregida)
                    // Usamos el username que viene, o el email si falla, o "Usuario" por defecto
                    val userDisplayName = loginData.username ?: loginData.email ?: "Usuario"

                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                        .putString("USERNAME", userDisplayName)
                        .putString("USER_ID", loginData.userId)     // ✅ Acceso directo
                        .putString("USER_EMAIL", loginData.email)   // ✅ Acceso directo
                        .apply()

                    // 3. Feedback y Navegación
                    Toast.makeText(this@MainActivity, "¡Bienvenido $userDisplayName!", Toast.LENGTH_LONG).show()
                    navigateToSongSelection()

                } else {
                    Toast.makeText(this@MainActivity, "Login fallido: Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                // Manejo genérico de errores (red, servidor, timeout)
                val msg = if (e is IOException) "Error de conexión" else "Error del servidor: ${e.message}"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToSongSelection() {
        val intent = Intent(this, SongSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
        btnLogin.text = if (isLoading) "Conectando..." else "ENTRAR"
    }

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString()
        if (email.isEmpty()) { etEmail.error = "Requerido"; return false }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email no válido"
            return false
        }
        return true
    }

    private fun validatePassword(): Boolean {
        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            passwordLayout.error = "Requerido"
            return false
        } else {
            passwordLayout.error = null
        }
        return true
    }

    private fun validateForm() = validateEmail() && validatePassword()
}