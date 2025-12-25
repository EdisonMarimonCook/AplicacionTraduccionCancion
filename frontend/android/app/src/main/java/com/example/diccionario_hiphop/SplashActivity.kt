package com.example.diccionario_hiphop

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen") // Ignoramos warning de API 31+
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Esperar 2 segundos para mostrar la marca y luego navegar
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, 2000)
    }

    private fun checkSessionAndNavigate() {
        val tokenManager = TokenManager(this)
        val token = tokenManager.getToken()

        if (token != null) {
            // Si hay token, vamos directo a la App
            val intent = Intent(this, SongSelectionActivity::class.java)
            startActivity(intent)
        } else {
            // Si no, vamos al Login
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Cerramos el Splash para que no se pueda volver atrás
        finish()
    }
}