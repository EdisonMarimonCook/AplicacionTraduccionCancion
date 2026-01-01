package com.example.diccionario_hiphop

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var radioGroupLevel: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        initViews()
        loadCurrentLevel()
        setupClickListeners()
    }

    private fun initViews() {
        radioGroupLevel = findViewById(R.id.radioGroupLevel)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun loadCurrentLevel() {
        // Por defecto usamos B1 (Intermedio)
        val currentLevel = sharedPreferences.getString("userLevel", "B1")

        // Mapeamos los códigos CEFR a los botones de la UI
        when (currentLevel) {
            "A1", "A2" -> findViewById<RadioButton>(R.id.radioBeginner).isChecked = true
            "B1", "B2" -> findViewById<RadioButton>(R.id.radioIntermediate).isChecked = true
            "C1", "C2" -> findViewById<RadioButton>(R.id.radioAdvanced).isChecked = true
            else -> findViewById<RadioButton>(R.id.radioIntermediate).isChecked = true
        }
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveLevel()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun saveLevel() {
        val selectedId = radioGroupLevel.checkedRadioButtonId
        
        // Mapeamos la selección visual a códigos CEFR para la IA
        val level = when (selectedId) {
            R.id.radioBeginner -> "A2"       // Principiante
            R.id.radioIntermediate -> "B1"  // Intermedio (Estándar)
            R.id.radioAdvanced -> "C1"      // Avanzado
            else -> "B1"
        }

        sharedPreferences.edit().putString("userLevel", level).apply()

        Toast.makeText(this, "Nivel actualizado a: $level", Toast.LENGTH_SHORT).show()

        // ✅ CORRECCIÓN: Volver a MainActivity (el nuevo contenedor)
        val intent = Intent(this, MainActivity::class.java)
        // Mantenemos los flags para limpiar la pila y reiniciar la app con la nueva configuración
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}