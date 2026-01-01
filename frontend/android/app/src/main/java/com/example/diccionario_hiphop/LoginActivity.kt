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

class LoginActivity : AppCompatActivity() {

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

        tokenManager = TokenManager(this)

        // Auto-Login: Si hay token guardado, intentamos entrar directo
        if (tokenManager.getToken() != null) {
            tryAutoLogin()
            return
        }

        setContentView(R.layout.activity_login)
        initViews()
        setupValidations()
        setupClickListeners()
    }

    private fun tryAutoLogin() {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@LoginActivity)
                val response = apiService.getProfile()

                if (response.isSuccessful && response.body() != null) {
                    navigateToMainApp()
                } else {
                    // Token inválido o expirado
                    tokenManager.clearSession()
                    recreate() // Recargar para mostrar el login
                }
            } catch (e: Exception) {
                tokenManager.clearSession()
                recreate()
            }
        }
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
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val rememberMe = cbRememberMe.isChecked

        setLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@LoginActivity)
                val request = LoginRequest(email, password)
                
                // 1. Llamada a la API
                val response = apiService.login(request)

                // 2. Verificación y desempaquetado seguro
                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!

                    if (loginData.accessToken.isNotEmpty()) {
                        
                        // Guardar datos básicos del usuario
                        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("username", loginData.username)
                            putString("USER_LEVEL", "B1") 
                            apply()
                        }

                        // ✅ AQUÍ ESTÁ LA MAGIA QUE COINCIDE CON TU TOKENMANAGER
                        if (rememberMe) {
                            tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)
                        } else {
                            tokenManager.saveTemporaryTokens(loginData.accessToken, loginData.refreshToken)
                        }

                        navigateToMainApp()
                    } else {
                        Toast.makeText(this@LoginActivity, "Error: Token vacío", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Error del servidor (ej: 401)
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }

            } catch (e: HttpException) {
                Toast.makeText(this@LoginActivity, "Error de servicio: ${e.code()}", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this@LoginActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToMainApp() {
        val intent = Intent(this, MainActivity::class.java)
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