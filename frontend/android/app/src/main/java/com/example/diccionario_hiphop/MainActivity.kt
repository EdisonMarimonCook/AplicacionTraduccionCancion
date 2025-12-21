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

        // 2. Auto-Login: Si hay token guardado, intentar validarlo con el backend
        // Esto permite que usuarios que marcaron "Recordarme" no tengan que volver a ingresar credenciales
        if (tokenManager.getToken() != null) {
            tryAutoLogin()
            return
        }

        // 3. Si no hay token, mostrar pantalla de login normal
        setContentView(R.layout.activity_main)
        initViews()
        setupValidations()
        setupClickListeners()
    }

    /**
     * Intenta hacer auto-login validando el token guardado con el backend.
     * Si el token es válido, navega directamente a SongSelectionActivity.
     * Si es inválido o hay error de red, limpia los tokens y muestra la pantalla de login.
     */    private fun tryAutoLogin() {
        lifecycleScope.launch {
            try {
                // Validar token con el backend
                val apiService = RetrofitService.getInstance(this@MainActivity)
                val response = apiService.getProfile()

                if (response.isSuccessful && response.body() != null) {
                    // Token válido, navegamos directamente
                    navigateToSongSelection()
                } else {
                    // Token inválido, limpiar tokens y mostrar login
                    tokenManager.clearTokens()
                    showLoginScreen()
                }
            } catch (e: Exception) {
                // Error de red o token inválido, limpiar tokens y mostrar login
                tokenManager.clearTokens()
                showLoginScreen()
            }
        }
    }

    private fun showLoginScreen() {
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
                val apiService = RetrofitService.getInstance(this@MainActivity)
                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!

                    // Guardar tokens de forma persistente solo si "Remember Me" está activo
                    if (rememberMe) {
                        tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)

                        // Guardar datos de usuario
                        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("username", loginData.username)
                            putString("USER_LEVEL", "B1")
                            apply()
                        }
                    } else {
                        // Si NO marca "Recuérdame", guardamos tokens solo para esta sesión
                        // Los tokens se guardan temporalmente pero se limpiarán al cerrar la app
                        tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)
                    }

                    navigateToSongSelection()
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Email o contraseña incorrectos"
                        403 -> "Cuenta no verificada. Revisa tu email."
                        else -> "Error en el servidor"
                    }
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    403 -> "⚠️ Debes verificar tu email antes de iniciar sesión"
                    else -> "Error: ${e.message()}"
                }
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
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