package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class VerifyAccountActivity : AppCompatActivity() {

    private lateinit var tilCode: TextInputLayout
    private lateinit var etCode: EditText
    private lateinit var btnVerify: Button
    private lateinit var tvResendCode: TextView
    private lateinit var tvInstructions: TextView

    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_account)

        userEmail = intent.getStringExtra("email") ?: ""
        
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Error: Email no proporcionado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        updateInstructions()
    }

    private fun initViews() {
        tilCode = findViewById(R.id.tilCode)
        etCode = findViewById(R.id.etCode)
        btnVerify = findViewById(R.id.btnVerify)
        tvResendCode = findViewById(R.id.tvResendCode)
        tvInstructions = findViewById(R.id.tvInstructions)
    }

    private fun updateInstructions() {
        tvInstructions.text = "Te hemos enviado un código de 4 dígitos a:\n$userEmail"
    }

    private fun setupListeners() {
        etCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 4) {
                    verifyAccount()
                }
            }
        })

        btnVerify.setOnClickListener {
            if (validateCode()) {
                verifyAccount()
            }
        }

        tvResendCode.setOnClickListener {
            resendCode()
        }
    }

    private fun validateCode(): Boolean {
        val code = etCode.text.toString()
        if (code.length != 4) {
            tilCode.error = "Ingresa el código de 4 dígitos"
            return false
        }
        tilCode.error = null
        return true
    }

    private fun verifyAccount() {
        val code = etCode.text.toString().trim()
        
        setLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@VerifyAccountActivity)
                val request = VerifyAccountRequest(userEmail, code)
                val response = apiService.verifyAccount(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@VerifyAccountActivity,
                        "✅ Cuenta verificada correctamente",
                        Toast.LENGTH_LONG
                    ).show()

                    val intent = Intent(this@VerifyAccountActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Código incorrecto o expirado"
                        404 -> "Usuario no encontrado"
                        else -> "Error en la verificación"
                    }
                    Toast.makeText(this@VerifyAccountActivity, errorMsg, Toast.LENGTH_LONG).show()
                    tilCode.error = errorMsg
                }
            } catch (e: HttpException) {
                Toast.makeText(
                    this@VerifyAccountActivity,
                    "Error: ${e.message()}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: IOException) {
                Toast.makeText(
                    this@VerifyAccountActivity,
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun resendCode() {
        Toast.makeText(this, "📧 Código reenviado a $userEmail", Toast.LENGTH_LONG).show()
    }

    private fun setLoading(isLoading: Boolean) {
        btnVerify.isEnabled = !isLoading
        etCode.isEnabled = !isLoading
        tvResendCode.isEnabled = !isLoading
        btnVerify.text = if (isLoading) "Verificando..." else "✅ VERIFICAR"
    }
}