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
    private lateinit var tvHeader: TextView
    private lateinit var btnOpenDict: ExtendedFloatingActionButton
    private lateinit var btnProfile: ImageButton

    private lateinit var adapter: SongAdapter
    private lateinit var repository: SongRepository
    
    // Debounce para no saturar el buscador
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        repository = SongRepository(this)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupListeners()

        // Carga inicial: Top Grammys
        loadTopSongs()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvSongs)
        progressBar = findViewById(R.id.progressBar)
        searchView = findViewById(R.id.searchView)
        tvHeader = findViewById(R.id.tvHeader)
        btnOpenDict = findViewById(R.id.btnOpenDict)
        btnProfile = findViewById(R.id.btnProfile)
    }

    private fun setupListeners() {
        btnOpenDict.setOnClickListener {
            startActivity(Intent(this, DictionaryActivity::class.java))
        }
        
        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(emptyList()) { songItem ->
            // Al hacer click en una canción:
            val intent = Intent(this, SongLearningActivity::class.java).apply {
                putExtra("SONG_ID", songItem.id)
                putExtra("SONG_TITLE", songItem.title)
                putExtra("SONG_ARTIST", songItem.artist)
                putExtra("PREVIEW_URL", songItem.previewUrl)
                putExtra("COVER_URL", songItem.imageUrl)
                putExtra("SPOTIFY_URL", songItem.spotifyUrl) // ✅ Importante para abrir Spotify
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchJob?.cancel()
                    performSearch(query)
                    searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Debounce: Esperar 500ms antes de buscar
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500) // Espera un poco para no spamear Spotify
                    if (!newText.isNullOrEmpty() && newText.length >= 2) {
                        performSearch(newText)
                    } else if (newText.isNullOrEmpty()) {
                        loadTopSongs()
                    }
                }
                return true
            }
        })
    }

    private fun loadTopSongs() {
        tvHeader.text = "🏆 Top Grammy & Hits"
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Llamamos al endpoint de Top Grammy del Backend v4.0
                // ✅ CAMBIO: getTopGrammy devuelve List<SongItem> directamente
                val response = repository.getTopGrammy("en") 
                
                if (response.isSuccessful && response.body() != null) {
                    val songs = response.body()!!
                    adapter.updateData(songs)
                } else {
                    Toast.makeText(this@SongSelectionActivity, "Error cargando Top", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SongSelectionActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun performSearch(query: String) {
        tvHeader.text = "Resultados para '$query'"
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Llamada real al backend -> Spotify
                // ✅ CAMBIO: searchSongs devuelve List<SongItem> directamente
                val response = repository.searchSongs(query)

                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!
                    
                    if (results.isEmpty()) {
                        Toast.makeText(this@SongSelectionActivity, "No se encontraron canciones", Toast.LENGTH_SHORT).show()
                    }
                    adapter.updateData(results)
                } else {
                    Toast.makeText(this@SongSelectionActivity, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SongSelectionActivity, "Error buscando: ${e.message}", Toast.LENGTH_SHORT).show()
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