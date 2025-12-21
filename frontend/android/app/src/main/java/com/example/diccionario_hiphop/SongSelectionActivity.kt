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

    private lateinit var adapter: SongAdapter // Esta es la variable importante
    private lateinit var repository: SongRepository

    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // Aplicar tema dinámico al fondo
        window.decorView.setBackgroundColor(
            resources.getColor(android.R.color.background_light, theme)
        )

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

        // --- 3. DICCIONARIO ---
        val irDiccionario = View.OnClickListener {
            startActivity(Intent(this, DictionaryActivity::class.java))
            toggleMenu(false)
        }
        fabDict.setOnClickListener(irDiccionario)
        txtDict.setOnClickListener(irDiccionario)

        // --- 4. FLASHCARDS ---
        val irFlashcards = View.OnClickListener {
            startActivity(Intent(this, FlashcardsActivity::class.java))
            toggleMenu(false)
        }
        fabFlash.setOnClickListener(irFlashcards)
        txtFlash.setOnClickListener(irFlashcards)

        // --- 5. PERFIL ---
        val irPerfil = View.OnClickListener {
            startActivityForResult(Intent(this, ProfileActivity::class.java), 300) // 🔥 Código 300 para detectar cambios en perfil
            toggleMenu(false)
        }
        fabProfile.setOnClickListener(irPerfil)
        txtProfile.setOnClickListener(irPerfil)
    }

    private fun toggleMenu(open: Boolean) {
        isMenuOpen = open
        if (open) {
            fabMain.animate().rotation(45f).setDuration(300).start()
            viewDimmer.visibility = View.VISIBLE

            showFab(fabDict, txtDict)
            showFab(fabFlash, txtFlash)
            showFab(fabProfile, txtProfile)
            fabMain.bringToFront()
        } else {
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
        fab.bringToFront()
        txt.bringToFront()
        fab.isClickable = true
        txt.isClickable = true
        fab.alpha = 0f
        fab.animate().alpha(1f).translationY(0f).setDuration(300).start()
        txt.alpha = 0f
        txt.animate().alpha(1f).translationY(0f).setDuration(300).start()
    }

    private fun hideFab(fab: FloatingActionButton, txt: TextView) {
        fab.visibility = View.GONE
        txt.visibility = View.GONE
        fab.isClickable = false
        txt.isClickable = false
    }

    private fun setupRecyclerView() {
        // Inicializamos el adaptador con lista vacía
        adapter = SongAdapter(emptyList()) { song ->
            val intent = Intent(this, SongLearningActivity::class.java)
            
            // PASAMOS LOS DATOS EXACTOS DEL MODELO SongItem
            intent.putExtra("song_title", song.title)
            intent.putExtra("song_artist", song.artist)
            intent.putExtra("song_image", song.imageUrl) // ✅ CORREGIDO: Antes era coverUrl
            intent.putExtra("song_audio", song.previewUrl) 
            
            startActivityForResult(intent, 200) // 🔥 Código 200 para detectar si añadió palabras
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
                // Tu ApiService ya devuelve Response<List<SongItem>>
                val response = repository.getTopGrammy("en")
                
                if (response.isSuccessful && response.body() != null) {
                    val songs = response.body()!!
                    
                    // ✅ CORREGIDO: Pasamos la lista directa. 
                    // No hace falta hacer .map { SongItem(...) } porque YA SON SongItem.
                    adapter.updateData(songs) 
                } else {
                    Toast.makeText(this@SongSelectionActivity, "Error cargando Top", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace() // Imprime el error real en consola
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
                // Tu repositorio ya devuelve una lista de SongItem (List<SongItem>)
                val response = repository.searchSongs(query)
                
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!

                    // ❌ ANTES HACÍAS ESTO (Y DABA ERROR):
                    // val items = results.map { SongItem(it.id, it.coverUrl, ...) } 
                    // adapter.updateData(items)

                    // ✅ AHORA HAZ SOLO ESTO (Directo):
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

    // 🔥 Propagar RESULT_OK si vuelve de SongLearningActivity con palabras añadidas
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            200 -> { // Volvió de SongLearningActivity
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK) // Propagar para que ProfileActivity se recargue si está abierta
                }
            }
            300 -> { // Volvió de ProfileActivity (no hacemos nada, pero está preparado para futuro)
                // Aquí podrías recargar algo si fuera necesario
            }
        }
    }
}