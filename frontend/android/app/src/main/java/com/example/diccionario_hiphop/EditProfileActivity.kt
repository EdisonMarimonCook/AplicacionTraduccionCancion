package com.example.diccionario_hiphop

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import com.example.diccionario_hiphop.utils.WindowInsetsHelper

class EditProfileActivity : AppCompatActivity() {

    // Contenedores (Menú y Formularios)
    private lateinit var layoutMenu: View
    private lateinit var layoutFormName: View
    private lateinit var layoutFormEmail: View
    private lateinit var layoutFormPass: View

    // Header y Navegación
    private lateinit var tvHeaderTitle: TextView
    private lateinit var btnBack: ImageButton

    // Inputs (Campos de texto)
    private lateinit var etNewName: EditText
    private lateinit var etNewEmail: EditText
    private lateinit var etCurrentPassEmail: EditText
    private lateinit var etNewPass: EditText
    private lateinit var etCurrentPassPass: EditText

    // Botones de Guardar
    private lateinit var btnSaveName: Button
    private lateinit var btnSaveEmail: Button
    private lateinit var btnSavePass: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // 🔧 Aplicar WindowInsets para todos los elementos
        val rootView = findViewById<View>(android.R.id.content)
        WindowInsetsHelper.applySystemBarInsets(rootView)

        initViews()
        setupMenuNavigation()
        setupSaveActions()
        handleBackPress()
    }

    private fun initViews() {
        // Layouts
        layoutMenu = findViewById(R.id.layoutMenu)
        layoutFormName = findViewById(R.id.layoutFormName)
        layoutFormEmail = findViewById(R.id.layoutFormEmail)
        layoutFormPass = findViewById(R.id.layoutFormPass)

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)

        // Inputs
        etNewName = findViewById(R.id.etNewName)
        etNewEmail = findViewById(R.id.etNewEmail)
        etCurrentPassEmail = findViewById(R.id.etCurrentPassEmail)
        etNewPass = findViewById(R.id.etNewPass)
        etCurrentPassPass = findViewById(R.id.etCurrentPassPass)

        // Botones
        btnSaveName = findViewById(R.id.btnSaveName)
        btnSaveEmail = findViewById(R.id.btnSaveEmail)
        btnSavePass = findViewById(R.id.btnSavePass)
    }

    private fun setupMenuNavigation() {
        findViewById<CardView>(R.id.cardOptionName).setOnClickListener {
            showView(layoutFormName, "Cambiar Nombre")
        }

        findViewById<CardView>(R.id.cardOptionEmail).setOnClickListener {
            showView(layoutFormEmail, "Cambiar Correo")
        }

        findViewById<CardView>(R.id.cardOptionPass).setOnClickListener {
            showView(layoutFormPass, "Cambiar Contraseña")
        }

        btnBack.setOnClickListener {
            goBackOrFinish()
        }
    }

    private fun showView(view: View, title: String) {
        layoutMenu.visibility = View.GONE
        layoutFormName.visibility = View.GONE
        layoutFormEmail.visibility = View.GONE
        layoutFormPass.visibility = View.GONE

        view.visibility = View.VISIBLE
        tvHeaderTitle.text = title
    }

    private fun showMenu() {
        layoutMenu.visibility = View.VISIBLE
        layoutFormName.visibility = View.GONE
        layoutFormEmail.visibility = View.GONE
        layoutFormPass.visibility = View.GONE
        tvHeaderTitle.text = "Ajustes de Cuenta"

        // Limpiar campos por seguridad
        etNewName.text.clear()
        etNewEmail.text.clear()
        etCurrentPassEmail.text.clear()
        etNewPass.text.clear()
        etCurrentPassPass.text.clear()
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBackOrFinish()
            }
        })
    }

    private fun goBackOrFinish() {
        if (layoutMenu.visibility != View.VISIBLE) {
            showMenu()
        } else {
            finish()
        }
    }

    private fun setupSaveActions() {
        // 1. GUARDAR NOMBRE
        btnSaveName.setOnClickListener {
            val name = etNewName.text.toString().trim()
            if (name.isEmpty()) {
                etNewName.error = "Escribe un nombre"
                return@setOnClickListener
            }
            performAction(shouldLogout = false) { api ->
                val req = UpdateProfileRequest(username = name)
                val response = api.updateProfile(req)
                if (response.isSuccessful) {
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit().putString("USERNAME", name).apply()
                    "Nombre actualizado correctamente"
                } else {
                    throw Exception(parseError(response))
                }
            }
        }

        // 2. GUARDAR EMAIL
        btnSaveEmail.setOnClickListener {
            val email = etNewEmail.text.toString().trim()
            val pass = etCurrentPassEmail.text.toString().trim()

            if (email.isEmpty()) { etNewEmail.error = "Requerido"; return@setOnClickListener }
            if (pass.isEmpty()) { etCurrentPassEmail.error = "Requerido"; return@setOnClickListener }

            performAction(shouldLogout = true) { api ->
                // 🔥 CORRECCIÓN: Usamos 'newEmail' (camelCase) como en Models.kt
                val req = ChangeEmailRequest(newEmail = email, password = pass)
                val response = api.changeEmail(req)
                if (response.isSuccessful) "Email cambiado. Inicia sesión de nuevo."
                else throw Exception(parseError(response))
            }
        }

        // 3. GUARDAR PASSWORD
        btnSavePass.setOnClickListener {
            val newPass = etNewPass.text.toString().trim()
            val currentPass = etCurrentPassPass.text.toString().trim()

            if (newPass.isEmpty()) { etNewPass.error = "Requerido"; return@setOnClickListener }
            if (currentPass.isEmpty()) { etCurrentPassPass.error = "Requerido"; return@setOnClickListener }

            performAction(shouldLogout = true) { api ->
                // 🔥 CORRECCIÓN: Usamos 'currentPassword', 'newPassword', etc.
                val req = ChangePasswordRequest(
                    currentPassword = currentPass,
                    newPassword = newPass,
                    confirmPassword = newPass
                )
                val response = api.changePassword(req)
                if (response.isSuccessful) "Contraseña cambiada. Inicia sesión de nuevo."
                else throw Exception(parseError(response))
            }
        }
    }

    private fun performAction(shouldLogout: Boolean = false, action: suspend (ApiService) -> String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@EditProfileActivity)
                val successMsg = action(api)
                Toast.makeText(this@EditProfileActivity, successMsg, Toast.LENGTH_LONG).show()

                if (shouldLogout) {
                    val tokenManager = TokenManager(this@EditProfileActivity)
                    tokenManager.forceLogout()
                } else {
                    showMenu()
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Error desconocido"
                Toast.makeText(this@EditProfileActivity, "Error: $msg", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun parseError(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                val json = JSONObject(errorBody)
                json.optString("detail", "Error ${response.code()}")
            } else {
                "Error ${response.code()}"
            }
        } catch (e: Exception) {
            "Error de servidor"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        layoutMenu.isEnabled = !isLoading
        layoutFormName.isEnabled = !isLoading
        layoutFormEmail.isEnabled = !isLoading
        layoutFormPass.isEnabled = !isLoading
    }
}