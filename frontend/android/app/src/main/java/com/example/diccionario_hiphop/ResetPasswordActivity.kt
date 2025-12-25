package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var tilCode: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etCode: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnResetPassword: Button

    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        userEmail = intent.getStringExtra("email") ?: ""
        
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Email no proporcionado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilCode = findViewById(R.id.tilCode)
        tilNewPassword = findViewById(R.id.tilNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etCode = findViewById(R.id.etCode)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
    }

    private fun setupListeners() {
        btnResetPassword.setOnClickListener {
            if (validateForm()) {
                resetPassword()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val code = etCode.text.toString().trim()
        if (code.length != 4) {
            tilCode.error = "Ingresa el código de 4 dígitos"
            isValid = false
        } else {
            tilCode.error = null
        }

        val newPassword = etNewPassword.text.toString().trim()
        if (newPassword.length < 6) {
            tilNewPassword.error = "Mínimo 6 caracteres"
            isValid = false
        } else {
            tilNewPassword.error = null
        }

        val confirmPassword = etConfirmPassword.text.toString().trim()
        if (confirmPassword != newPassword) {
            tilConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        return isValid
    }

    private fun resetPassword() {
        val code = etCode.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()

        setLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ResetPasswordActivity)
                val request = ResetPasswordRequest(userEmail, code, newPassword)
                val response = apiService.resetPassword(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "✅ Contraseña cambiada correctamente",
                        Toast.LENGTH_LONG
                    ).show()

                    // Navegar al login
                    val intent = Intent(this@ResetPasswordActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Código incorrecto o expirado"
                        404 -> "Usuario no encontrado"
                        else -> "Error al cambiar contraseña"
                    }
                    Toast.makeText(this@ResetPasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Error: ${e.message()}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: IOException) {
                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnResetPassword.isEnabled = !isLoading
        etCode.isEnabled = !isLoading
        etNewPassword.isEnabled = !isLoading
        etConfirmPassword.isEnabled = !isLoading
        btnResetPassword.text = if (isLoading) "Cambiando..." else "✅ CAMBIAR CONTRASEÑA"
    }
}