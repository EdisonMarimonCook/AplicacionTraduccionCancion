package com.example.diccionario_hiphop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DictionaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Colocamos el DictionaryFragment ocupando toda la pantalla
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, DictionaryFragment())
                .commit()
        }
    }
}