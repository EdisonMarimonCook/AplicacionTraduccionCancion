package com.example.diccionario_hiphop

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvRegister: TextView
    private lateinit var layoutLevelSelection: LinearLayout
    private lateinit var spinnerLevel: Spinner
    private lateinit var sharedPreferences: SharedPreferences

    // Nuevas views para status de conexión
    private lateinit var connectionStatus: TextView
    private lateinit var connectionIndicator: ImageView
    private lateinit var reconnectButton: Button
    private lateinit var connectionTester: ConnectionTester

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya está logueado SOLO si recordó sesión
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (shouldAutoLogin()) {
            testConnectionAndNavigate()
            return
        }

        setContentView(R.layout.activity_main)
        initViews()
        setupValidations()
        setupClickListeners()
        setupSpinner()
        testBackendConnection() // Probar conexión al iniciar
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

        // Nuevas views de conexión
        connectionStatus = findViewById(R.id.connectionStatus)
        connectionIndicator = findViewById(R.id.connectionIndicator)
        reconnectButton = findViewById(R.id.reconnectButton)
        connectionTester = ConnectionTester()
    }

    private fun setupValidations() {
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEmail()
            }
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword()
            }
        })
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            if (validateForm()) {
                if (layoutLevelSelection.isVisible) {
                    proceedToApp()
                } else {
                    showLevelSelection()
                }
            }
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        tvRegister.setOnClickListener {
            simulateQuickRegistration()
        }

        reconnectButton.setOnClickListener {
            testBackendConnection()
        }
    }

    private fun setupSpinner() {
        val levels = arrayOf("Select your level", "A1 - Beginner", "A2 - Elementary", "B1 - Intermediate", "B2 - Upper Intermediate", "C1 - Advanced", "C2 - Proficient")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLevel.adapter = adapter
    }

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString()
        return when {
            email.isEmpty() -> {
                etEmail.error = "El email es obligatorio"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Email no válido"
                false
            }
            else -> {
                etEmail.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = etPassword.text.toString()
        return when {
            password.isEmpty() -> {
                passwordLayout.error = "La contraseña es obligatoria"
                false
            }
            password.length < 6 -> {
                passwordLayout.error = "Mínimo 6 caracteres"
                false
            }
            else -> {
                passwordLayout.error = null
                true
            }
        }
    }

    private fun validateForm(): Boolean {
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        return isEmailValid && isPasswordValid
    }

    private fun showLevelSelection() {
        layoutLevelSelection.visibility = View.VISIBLE
        btnLogin.text = "🎵 Start Learning"
    }

    private fun proceedToApp() {
        if (spinnerLevel.selectedItemPosition == 0) {
            Toast.makeText(this, "Select your level of English", Toast.LENGTH_SHORT).show()
            return
        }

        val email = etEmail.text.toString()
        val level = when (spinnerLevel.selectedItemPosition) {
            1 -> "A1"
            2 -> "A2"
            3 -> "B1"
            4 -> "B2"
            5 -> "C1"
            6 -> "C2"
            else -> "B1"
        }

        val rememberMe = cbRememberMe.isChecked

        // Guardar preferencias SOLO si "Remember Me" está activado
        saveUserPreferences(email, level, rememberMe)

        Toast.makeText(this, "¡Welcome to HipHop Dictionary! Level: $level", Toast.LENGTH_LONG).show()
        navigateToSongSelection()
    }

    private fun saveUserPreferences(email: String, level: String, rememberMe: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("isLoggedIn", rememberMe)
            putString("userEmail", if (rememberMe) email else "")
            putString("userLevel", level)
            putBoolean("rememberMe", rememberMe)
            apply()
        }
    }

    // ✅ SOLO auto-login si "Remember Me" estaba activado
    private fun shouldAutoLogin(): Boolean {
        return sharedPreferences.getBoolean("rememberMe", false) &&
                sharedPreferences.getBoolean("isLoggedIn", false)
    }

    private fun testConnectionAndNavigate() {
        showConnectionTesting()

        CoroutineScope(Dispatchers.Main).launch {
            val isConnected = connectionTester.testBackendConnection()

            if (isConnected) {
                showConnectionSuccess()
                navigateToSongSelection()
            } else {
                showConnectionError()
                // Mostrar login normal si no hay conexión
                setContentView(R.layout.activity_main)
                initViews()
                setupValidations()
                setupClickListeners()
                setupSpinner()
                testBackendConnection()
            }
        }
    }

    private fun testBackendConnection() {
        showConnectionTesting()

        CoroutineScope(Dispatchers.Main).launch {
            val isConnected = connectionTester.testBackendConnection()

            if (isConnected) {
                showConnectionSuccess()
            } else {
                showConnectionError()
            }
        }
    }

    private fun showConnectionTesting() {
        connectionStatus.text = "Conectando con el servidor..."
        connectionIndicator.setImageResource(R.drawable.ic_connection_loading)
        connectionIndicator.setColorFilter(resources.getColor(R.color.orange, theme))
        reconnectButton.visibility = View.GONE
    }

    private fun showConnectionSuccess() {
        connectionStatus.text = "Conectado al servidor ✓"
        connectionIndicator.setImageResource(R.drawable.ic_connection_success)
        connectionIndicator.setColorFilter(resources.getColor(R.color.green, theme))
        reconnectButton.visibility = View.GONE

        // Habilitar botón de login si estaba deshabilitado
        btnLogin.isEnabled = true
    }

    private fun showConnectionError() {
        connectionStatus.text = "Error de conexión ✗"
        connectionIndicator.setImageResource(R.drawable.ic_connection_error)
        connectionIndicator.setColorFilter(resources.getColor(R.color.red, theme))
        reconnectButton.visibility = View.VISIBLE

        // Deshabilitar botón de login si no hay conexión
        btnLogin.isEnabled = false
    }

    private fun navigateToSongSelection() {
        val level = sharedPreferences.getString("userLevel", "B1") ?: "B1"
        val intent = Intent(this, SongSelectionActivity::class.java)
        intent.putExtra("USER_LEVEL", level)
        startActivity(intent)
        finish()
    }

    private fun simulateQuickRegistration() {
        etEmail.setText("user@hiphop.com")
        etPassword.setText("123456")
        // NO marcar "Remember Me" por defecto
        cbRememberMe.isChecked = false
        Toast.makeText(this, "Demo user created! Now login", Toast.LENGTH_SHORT).show()

        // Auto-validar después de llenar
        validateEmail()
        validatePassword()
    }

    override fun onResume() {
        super.onResume()
        // Volver a verificar conexión cuando la actividad se reanude
        if (!shouldAutoLogin()) {
            testBackendConnection()
        }
    }
}