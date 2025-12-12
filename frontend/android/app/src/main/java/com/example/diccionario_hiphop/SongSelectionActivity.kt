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
        // 1. ABRIR / CERRAR MENÚ
        fabMain.setOnClickListener { toggleMenu(!isMenuOpen) }
        viewDimmer.setOnClickListener { if (isMenuOpen) toggleMenu(false) }

        // --- 3. DICCIONARIO (Texto + Icono) ---
        val irDiccionario = View.OnClickListener {
            startActivity(Intent(this, DictionaryActivity::class.java))
            toggleMenu(false)
        }
        fabDict.setOnClickListener(irDiccionario)
        txtDict.setOnClickListener(irDiccionario)

        // --- 4. FLASHCARDS (Texto + Icono) ---
        val irFlashcards = View.OnClickListener {
            startActivity(Intent(this, FlashcardsActivity::class.java))
            toggleMenu(false)
        }
        fabFlash.setOnClickListener(irFlashcards)
        txtFlash.setOnClickListener(irFlashcards)

        // --- 5. PERFIL (Texto + Icono) ---
        val irPerfil = View.OnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            toggleMenu(false)
        }
        fabProfile.setOnClickListener(irPerfil)
        txtProfile.setOnClickListener(irPerfil)
    }

    private fun toggleMenu(open: Boolean) {
        isMenuOpen = open

        if (open) {
            // ABRIR MENÚ
            fabMain.animate().rotation(45f).setDuration(300).start()
            viewDimmer.visibility = View.VISIBLE

            showFab(fabDict, txtDict)
            showFab(fabFlash, txtFlash)
            showFab(fabProfile, txtProfile)
            
            // 🔥 Botón principal al final para que quede encima de todo
            fabMain.bringToFront()

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

        // 🔥 Texto primero, FAB después (FAB queda más arriba en Z)
        fab.bringToFront()
        txt.bringToFront()

        // ✅ Ambos clickables
        fab.isClickable = true
        txt.isClickable = true  // 🔥 AHORA SÍ ES CLICKABLE

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