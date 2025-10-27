package com.example.diccionario_hiphop  // ⬅️ Usa tu package real

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SongSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerViewSongs: RecyclerView
    private lateinit var tvUserInfo: TextView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // Conectar elementos del XML
        recyclerViewSongs = findViewById(R.id.recyclerViewSongs)
        tvUserInfo = findViewById(R.id.tvUserInfo)
        btnBack = findViewById(R.id.btnBack)

        // Obtener nivel del usuario desde el login
        val userLevel = intent.getStringExtra("USER_LEVEL") ?: "beginner"

        // Configurar la pantalla
        setupUserInfo(userLevel)
        setupRecyclerView(userLevel)
        setupBackButton()
    }

    private fun setupUserInfo(level: String) {
        val levelText = when (level) {
            "beginner" -> "Principiante 🎵"
            "intermediate" -> "Intermedio 🎤"
            "advanced" -> "Avanzado 🔥"
            else -> "Principiante 🎵"
        }
        tvUserInfo.text = "Nivel: $levelText"
    }

    private fun setupRecyclerView(userLevel: String) {
        // Configurar el RecyclerView
        recyclerViewSongs.layoutManager = LinearLayoutManager(this)

        // Obtener canciones según el nivel
        val songs = getSongsByLevel(userLevel)

        // Crear y configurar el adapter
        val adapter = SongAdapter { song ->
            // Cuando se selecciona una canción
            openSongLearning(song)
        }

        recyclerViewSongs.adapter = adapter
        adapter.submitList(songs)
    }

    private fun getSongsByLevel(level: String): List<Song> {
        // Datos de ejemplo - luego vendrán de API/BD
        return listOf(
            Song(
                id = 1,
                title = "Lose Yourself",
                artist = "Eminem",
                difficulty = "intermediate",
                lyrics = "Look, if you had one shot, or one opportunity..."
            ),
            Song(
                id = 2,
                title = "Hotline Bling",
                artist = "Drake",
                difficulty = "beginner",
                lyrics = "You used to call me on my cell phone..."
            ),
            Song(
                id = 3,
                title = "God's Plan",
                artist = "Drake",
                difficulty = "beginner",
                lyrics = "I been movin' calm, don't start no trouble with me..."
            ),
            Song(
                id = 4,
                title = "Sicko Mode",
                artist = "Travis Scott",
                difficulty = "advanced",
                lyrics = "Astro, yeah, Sun is down, freezin' cold..."
            )
        ).filter { it.difficulty == level }
    }

    private fun openSongLearning(song: Song) {
        // Por ahora mostramos un mensaje
        // En el siguiente paso crearemos la pantalla de aprendizaje
        android.widget.Toast.makeText(
            this,
            "Seleccionaste: ${song.title} - ${song.artist}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            // Volver al login
            finish()
        }
    }
}