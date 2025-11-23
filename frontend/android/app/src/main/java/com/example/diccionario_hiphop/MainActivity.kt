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

    // Elementos de la UI
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView

    // Gestor de Sesión
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar TokenManager
        tokenManager = TokenManager(this)

        // 2. Auto-Login: Si ya hay un token guardado, saltamos el login
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
        // Asegúrate de que el ID en tu XML sea 'tilPassword' para el layout de contraseña
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

        // Navegar a la pantalla de Registro
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
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

                // Llamada al servidor
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!

                    // 🔥 CORRECCIÓN AQUÍ: Usamos saveTokens (plural) con ambos valores
                    // El modelo LoginResponse ahora tiene accessToken Y refreshToken
                    tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)

                    // Guardar nombre de usuario en preferencias para el perfil
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                        .putString("username", loginData.user.username)
                        .apply()

                    // Mensaje de éxito
                    Toast.makeText(this@MainActivity, "¡Bienvenido ${loginData.user.username}!", Toast.LENGTH_LONG).show()

                    // Entrar a la app
                    navigateToSongSelection()

                } else {
                    // Error 401 o 404
                    Toast.makeText(this@MainActivity, "Login fallido: Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }

            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, "Error de conexión. Verifica que el servidor esté activo.", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Toast.makeText(this@MainActivity, "Error del servidor: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToSongSelection() {
        val intent = Intent(this, SongSelectionActivity::class.java)

        // Limpiar historial para que al dar "Atrás" no vuelva al login
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