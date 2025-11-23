package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SongSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchView: SearchView
    private lateinit var tvHeader: TextView // Para cambiar el título "Top" vs "Resultados"

    private lateinit var adapter: SongAdapter
    private lateinit var repository: SongRepository
    private lateinit var btnOpenDict: ExtendedFloatingActionButton
    private lateinit var btnProfile: ImageButton

    // 🔥 Variable para controlar el "Debounce" (espera al escribir)
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        repository = SongRepository(this)

        initViews()
        setupRecyclerView()
        setupSearch()     // Configuración clave de la búsqueda
        setupClickListeners()

        // Carga inicial: Top 10
        loadTopSongs()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvSongs)
        progressBar = findViewById(R.id.progressBar)
        searchView = findViewById(R.id.searchView)
        tvHeader = findViewById(R.id.tvHeader) // Asegúrate de tener este ID en el XML
        btnOpenDict = findViewById(R.id.btnOpenDict)
        btnProfile = findViewById(R.id.btnProfile)
    }

    // ... (setupClickListeners y setupRecyclerView siguen igual) ...
    private fun setupClickListeners() {
        btnOpenDict.setOnClickListener { startActivity(Intent(this, DictionaryActivity::class.java)) }
        btnProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(emptyList()) { songItem ->
            val intent = Intent(this, SongLearningActivity::class.java)
            intent.putExtra("song_id", songItem.id)
            intent.putExtra("song_title", songItem.title)
            intent.putExtra("song_artist", songItem.artist)
            intent.putExtra("song_image", songItem.imageUrl)
            intent.putExtra("song_preview", songItem.previewUrl)
            intent.putExtra("song_spotify_url", songItem.spotifyUrl)
            intent.putExtra("song_has_preview", songItem.hasPreview)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    // ⭐ LÓGICA DE BÚSQUEDA HÍBRIDA
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            // Cuando el usuario pulsa "Enter" en el teclado
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchJob?.cancel()
                if (!query.isNullOrEmpty()) {
                    performSearch(query)
                }
                searchView.clearFocus()
                return true
            }

            // Cuando el usuario escribe letra a letra
            override fun onQueryTextChange(newText: String?): Boolean {
                // 1. Cancelar cualquier búsqueda anterior pendiente
                searchJob?.cancel()

                // 2. Si borró todo, volver al Top 10
                if (newText.isNullOrEmpty()) {
                    loadTopSongs()
                    return true
                }

                // 3. Si hay texto, esperar 500ms antes de llamar al servidor (Debounce)
                searchJob = lifecycleScope.launch {
                    delay(500)
                    if (newText.length >= 2) { // Buscar solo si hay 2+ letras
                        performSearch(newText)
                    }
                }
                return true
            }
        })
    }

    // Carga el Top 10 (Estado por defecto)
    private fun loadTopSongs() {
        tvHeader.text = "Top Canciones" // Título de la sección
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = repository.getTopSongs("es")
                if (response.isSuccessful && response.body() != null) {
                    adapter.updateData(response.body()!!.songs)
                }
            } catch (e: Exception) {
                Toast.makeText(this@SongSelectionActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    // Realiza la búsqueda (Estado Activo)
    private fun performSearch(query: String) {
        tvHeader.text = "Resultados para '$query'" // Cambiamos el título
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = repository.searchSongs(query)
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!.results
                    if (results.isEmpty()) {
                        Toast.makeText(this@SongSelectionActivity, "No se encontraron canciones", Toast.LENGTH_SHORT).show()
                    }
                    adapter.updateData(results)
                }
            } catch (e: Exception) {
                Toast.makeText(this@SongSelectionActivity, "Error buscando", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}
