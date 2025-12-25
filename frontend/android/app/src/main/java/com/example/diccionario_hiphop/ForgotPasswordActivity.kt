package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: EditText
    private lateinit var btnSendCode: Button
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        btnSendCode = findViewById(R.id.btnSendCode)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
    }

    private fun setupListeners() {
        btnSendCode.setOnClickListener {
            if (validateEmail()) {
                sendRecoveryCode()
            }
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "Ingresa tu email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email no válido"
            return false
        }
        tilEmail.error = null
        return true
    }

    private fun sendRecoveryCode() {
        val email = etEmail.text.toString().trim()
        
        setLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ForgotPasswordActivity)
                val request = ForgotPasswordRequest(email)
                val response = apiService.forgotPassword(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "📧 Código enviado a $email",
                        Toast.LENGTH_LONG
                    ).show()

                    val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = when (response.code()) {
                        404 -> "No existe una cuenta con ese email"
                        else -> "Error al enviar el código"
                    }
                    Toast.makeText(this@ForgotPasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                    tilEmail.error = errorMsg
                }
            } catch (e: HttpException) {
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Error: ${e.message()}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: IOException) {
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnSendCode.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        btnSendCode.text = if (isLoading) "Enviando..." else "📧 ENVIAR CÓDIGO"
    }
}