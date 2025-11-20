package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.launch

class SongSelectionActivity : AppCompatActivity() {

    // Declaración de variables para los elementos de la interfaz
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchView: SearchView
    private lateinit var adapter: SongAdapter
    private lateinit var repository: SongRepository

    // Botones de navegación
    private lateinit var btnOpenDict: ExtendedFloatingActionButton
    private lateinit var btnProfile: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // 1. Inicializamos el repositorio (pasando el contexto 'this')
        repository = SongRepository(this)

        // 2. Inicializamos las vistas y configuramos la lógica
        initViews()
        setupRecyclerView()
        setupSearch()
        setupClickListeners()

        // 3. Cargar canciones iniciales
        loadTopSongs()
    }

    private fun initViews() {
        // Buscamos cada elemento por su ID del XML
        recyclerView = findViewById(R.id.rvSongs)
        progressBar = findViewById(R.id.progressBar)
        searchView = findViewById(R.id.searchView)
        btnOpenDict = findViewById(R.id.btnOpenDict)
        btnProfile = findViewById(R.id.btnProfile)
    }

    private fun setupClickListeners() {
        // Navegar al Diccionario
        btnOpenDict.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            startActivity(intent)
        }

        // Navegar al Perfil
        btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        // Configuramos la lista vacía inicialmente
        adapter = SongAdapter(emptyList()) { songItem ->
            // Qué hacer al pulsar una canción: Ir a la pantalla de aprendizaje
            val intent = Intent(this, SongLearningActivity::class.java)
            intent.putExtra("song_title", songItem.title)
            intent.putExtra("song_artist", songItem.artist)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadTopSongs() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // Llamada al repositorio para obtener el Top
                val response = repository.getTopSongs("es")

                if (response.isSuccessful && response.body() != null) {
                    val songList = response.body()!!.songs
                    adapter.updateData(songList)
                } else {
                    Toast.makeText(this@SongSelectionActivity, "Error al cargar canciones", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SongSelectionActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchSongs(query)
                }
                // Ocultar teclado
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Aquí podrías implementar búsqueda en tiempo real si quisieras
                return false
            }
        })
    }

    private fun searchSongs(query: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = repository.searchSongs(query)
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!.results
                    adapter.updateData(results)
                } else {
                    Toast.makeText(this@SongSelectionActivity, "Sin resultados", Toast.LENGTH_SHORT).show()
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