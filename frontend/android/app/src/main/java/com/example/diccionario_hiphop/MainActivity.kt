package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView
    private lateinit var layoutLevelSelection: LinearLayout
    private lateinit var spinnerLevel: Spinner
    private lateinit var progressBar: ProgressBar // Asegúrate de tener esto en tu XML o usa una existente

    // Lógica
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar TokenManager
        tokenManager = TokenManager(this)

        // 2. Auto-Login: Si ya tenemos token, saltamos directo
        if (tokenManager.getToken() != null) {
            navigateToSongSelection()
            return // Importante para no cargar la UI de login
        }

        setContentView(R.layout.activity_main)
        initViews()
        setupValidations()
        setupClickListeners()
        setupSpinner()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        passwordLayout = findViewById(R.id.passwordLayout)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegister = findViewById(R.id.tvRegister)
        layoutLevelSelection = findViewById(R.id.layoutLevelSelection)
        spinnerLevel = findViewById(R.id.spinnerLevel)

        // Si no tienes ProgressBar en tu XML, puedes ignorar esto o añadirla
        // progressBar = findViewById(R.id.progressBar)
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
                // Si el spinner de nivel está visible, hacemos el login final
                if (layoutLevelSelection.isVisible) {
                    performRealLogin()
                } else {
                    // Primer paso: mostrar selección de nivel
                    showLevelSelection()
                }
            }
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        tvRegister.setOnClickListener {
            // Aquí iríamos a la RegisterActivity real
            // val intent = Intent(this, RegisterActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Crea RegisterActivity primero", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------
    // 🔥 AQUÍ ESTÁ LA INTEGRACIÓN CON RETROFIT
    // ---------------------------------------------------
    private fun performRealLogin() {
        // Verificar que eligió nivel
        if (spinnerLevel.selectedItemPosition == 0) {
            Toast.makeText(this, "Select your level of English", Toast.LENGTH_SHORT).show()
            return
        }

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val selectedLevel = getSelectedLevelCode()

        setLoading(true)

        lifecycleScope.launch {
            try {
                // 1. Instancia del servicio
                val apiService = RetrofitService.getInstance(this@MainActivity)

                // 2. Petición de Login
                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!

                    // 3. Guardar Token
                    tokenManager.saveToken(loginData.accessToken)

                    // 4. Guardar Nivel del usuario
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                        .putString("USER_LEVEL", selectedLevel)
                        .apply()

                    // ✅ CORRECCIÓN: Accedemos a .user.username
                    Toast.makeText(this@MainActivity, "¡Bienvenido ${loginData.user.username}!", Toast.LENGTH_LONG).show()
                    navigateToSongSelection()

                } else {
                    // Error: 401 (Contraseña mal) o 404 (Usuario no existe)
                    Toast.makeText(this@MainActivity, "Login fallido: Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }

            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, "Error de conexión: ¿Servidor encendido?", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Toast.makeText(this@MainActivity, "Error del servidor: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToSongSelection() {
        // Recuperamos el nivel guardado
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val level = prefs.getString("USER_LEVEL", "B1") ?: "B1"

        val intent = Intent(this, SongSelectionActivity::class.java)
        intent.putExtra("USER_LEVEL", level)
        // Limpiar backstack para que no vuelva al login al dar atrás
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ---------------------------------------------------
    // UTILIDADES
    // ---------------------------------------------------

    private fun showLevelSelection() {
        layoutLevelSelection.visibility = View.VISIBLE
        btnLogin.text = "🎵 Start Learning"
    }

    private fun getSelectedLevelCode(): String {
        return when (spinnerLevel.selectedItemPosition) {
            1 -> "A1"
            2 -> "A2"
            3 -> "B1"
            4 -> "B2"
            5 -> "C1"
            6 -> "C2"
            else -> "B1"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
        btnLogin.text = if (isLoading) "Conectando..." else "🎵 Start Learning"
    }

    // Configuración del Spinner (igual que tenías)
    private fun setupSpinner() {
        val levels = arrayOf("Select your level", "A1 - Beginner", "A2 - Elementary", "B1 - Intermediate", "B2 - Upper Intermediate", "C1 - Advanced", "C2 - Proficient")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLevel.adapter = adapter
    }

    // Validaciones (igual que tenías)
    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString()
        if (email.isEmpty()) { etEmail.error = "El email es obligatorio"; return false }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.error = "Email no válido"; return false }
        return true
    }

    private fun validatePassword(): Boolean {
        val password = etPassword.text.toString()
        if (password.isEmpty()) { passwordLayout.error = "Contraseña obligatoria"; return false }
        return true
    }

    private fun validateForm() = validateEmail() && validatePassword()
}