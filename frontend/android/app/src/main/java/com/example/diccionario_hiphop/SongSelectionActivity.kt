package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SongSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchView: SearchView
    private lateinit var tvHeader: TextView

    // --- VARIABLES MENÚ FLOTANTE ---
    private var isMenuOpen = false
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabDict: FloatingActionButton
    private lateinit var fabFlash: FloatingActionButton
    private lateinit var fabProfile: FloatingActionButton

    private lateinit var txtDict: TextView
    private lateinit var txtFlash: TextView
    private lateinit var txtProfile: TextView
    private lateinit var viewDimmer: View

    private lateinit var adapter: SongAdapter
    private lateinit var repository: SongRepository

    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        repository = SongRepository(this)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupListeners()

        loadTopSongs()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvSongs)
        progressBar = findViewById(R.id.progressBar)
        searchView = findViewById(R.id.searchView)
        tvHeader = findViewById(R.id.tvHeader)

        // Inicializar vistas del Menú Flotante
        fabMain = findViewById(R.id.fabMain)
        fabDict = findViewById(R.id.fabDictionary)
        fabFlash = findViewById(R.id.fabFlashcards)
        fabProfile = findViewById(R.id.fabProfile)

        txtDict = findViewById(R.id.txtDictionary)
        txtFlash = findViewById(R.id.txtFlashcards)
        txtProfile = findViewById(R.id.txtProfile)
        viewDimmer = findViewById(R.id.viewDimmer)
    }

    private fun setupListeners() {
        // 1. ABRIR / CERRAR MENÚ (Botón Principal)
        fabMain.setOnClickListener { toggleMenu(!isMenuOpen) }

        // 2. CERRAR AL TOCAR FONDO OSCURO
        viewDimmer.setOnClickListener { if (isMenuOpen) toggleMenu(false) }

        // 3. OPCIÓN DICCIONARIO (Icono y Texto)
        val goDict = View.OnClickListener {
            try {
                startActivity(Intent(this, DictionaryActivity::class.java))
                toggleMenu(false)
            } catch (e: Exception) {
                Toast.makeText(this, "Pantalla Diccionario no creada", Toast.LENGTH_SHORT).show()
            }
        }
        fabDict.setOnClickListener(goDict)
        txtDict.setOnClickListener(goDict)

        // 4. OPCIÓN FLASHCARDS (Icono y Texto)
        val goFlash = View.OnClickListener {
            try {
                startActivity(Intent(this, FlashcardsActivity::class.java))
                toggleMenu(false)
            } catch (e: Exception) {
                Toast.makeText(this, "Pantalla Flashcards no creada", Toast.LENGTH_SHORT).show()
            }
        }
        fabFlash.setOnClickListener(goFlash)
        txtFlash.setOnClickListener(goFlash)

        // 5. OPCIÓN PERFIL (Icono y Texto)
        val goProfile = View.OnClickListener {
            try {
                startActivity(Intent(this, ProfileActivity::class.java))
                toggleMenu(false)
            } catch (e: Exception) {
                Toast.makeText(this, "Pantalla Perfil no creada", Toast.LENGTH_SHORT).show()
            }
        }
        fabProfile.setOnClickListener(goProfile)
        txtProfile.setOnClickListener(goProfile)
    }

    private fun toggleMenu(open: Boolean) {
        isMenuOpen = open

        if (open) {
            // ABRIR MENÚ
            fabMain.animate().rotation(45f).setDuration(300).start()
            viewDimmer.visibility = View.VISIBLE

            // 🔥 CRUCIAL: Traer al frente para que reciban el clic por encima de todo
            viewDimmer.bringToFront()
            fabMain.bringToFront()

            showFab(fabDict, txtDict)
            showFab(fabFlash, txtFlash)
            showFab(fabProfile, txtProfile)
        } else {
            // CERRAR MENÚ
            fabMain.animate().rotation(0f).setDuration(300).start()
            viewDimmer.visibility = View.GONE

            hideFab(fabDict, txtDict)
            hideFab(fabFlash, txtFlash)
            hideFab(fabProfile, txtProfile)
        }
    }

    private fun showFab(fab: FloatingActionButton, txt: TextView) {
        fab.visibility = View.VISIBLE
        txt.visibility = View.VISIBLE

        // 🔥 Asegurar que están "encima" y son clicables
        fab.bringToFront()
        txt.bringToFront()

        // Habilitar clic explícitamente
        fab.isClickable = true
        txt.isClickable = true

        // Animación de aparición (Fade in + Slide up)
        fab.alpha = 0f
        fab.animate().alpha(1f).translationY(0f).setDuration(300).start()

        txt.alpha = 0f
        txt.animate().alpha(1f).translationY(0f).setDuration(300).start()
    }

    private fun hideFab(fab: FloatingActionButton, txt: TextView) {
        fab.visibility = View.GONE
        txt.visibility = View.GONE

        // Deshabilitar clic para que no molesten (clics fantasma)
        fab.isClickable = false
        txt.isClickable = false
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(emptyList()) { songItem ->
            val intent = Intent(this, SongLearningActivity::class.java).apply {
                putExtra("SONG_ID", songItem.id)
                putExtra("SONG_TITLE", songItem.title)
                putExtra("SONG_ARTIST", songItem.artist)
                putExtra("PREVIEW_URL", songItem.previewUrl)
                putExtra("COVER_URL", songItem.imageUrl)
                putExtra("SPOTIFY_URL", songItem.spotifyUrl)
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
                // Debounce para no saturar la búsqueda
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500)
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
                val response = repository.searchSongs(query)
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!
                    adapter.updateData(results)
                } else {
                    Toast.makeText(this@SongSelectionActivity, "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SongSelectionActivity, "Error en la búsqueda", Toast.LENGTH_SHORT).show()
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