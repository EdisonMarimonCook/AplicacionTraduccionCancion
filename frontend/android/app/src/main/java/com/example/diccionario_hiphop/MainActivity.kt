package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Conectar los elementos
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val spinnerLevel = findViewById<Spinner>(R.id.spinnerLevel)
        val btnStart = findViewById<Button>(R.id.btnStart)

        // Configurar el Spinner
        val levels = arrayOf("Selecciona tu nivel", "Principiante 🎵", "Intermedio 🎤", "Avanzado 🔥")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        spinnerLevel.adapter = adapter

        // Configurar el botón
        btnStart.setOnClickListener {
            if (isFormValid(etEmail, etPassword, spinnerLevel)) {
                val email = etEmail.text.toString()
                val level = when (spinnerLevel.selectedItemPosition) {
                    1 -> "beginner"
                    2 -> "intermediate"
                    3 -> "advanced"
                    else -> "beginner"
                }

                // Navegar a la pantalla de canciones
                val intent = Intent(this, SongSelectionActivity::class.java)
                intent.putExtra("USER_LEVEL", level)
                startActivity(intent)

                Toast.makeText(this, "¡Bienvenido $email!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // FUNCIÓN QUE FALTABA
    private fun isFormValid(
        etEmail: EditText,
        etPassword: EditText,
        spinnerLevel: Spinner
    ): Boolean {
        return etEmail.text.isNotEmpty() &&
                etPassword.text.isNotEmpty() &&
                spinnerLevel.selectedItemPosition > 0
    }
}