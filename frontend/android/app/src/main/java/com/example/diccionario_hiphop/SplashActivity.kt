package com.example.diccionario_hiphop

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Esperar 2 segundos para mostrar la marca y luego comprobar sesión
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, 2000)
    }

    private fun checkSessionAndNavigate() {
        val tokenManager = TokenManager(this)
        val token = tokenManager.getToken()

        if (token != null) {
            // 🔥 SI HAY SESIÓN: Vamos al NUEVO contenedor principal (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // 🔥 SI NO HAY SESIÓN: Vamos al NUEVO Login (LoginActivity)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        finish() // Cerramos Splash
    }
}