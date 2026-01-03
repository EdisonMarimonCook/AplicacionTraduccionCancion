package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var searchView: SearchView
    private lateinit var tvHeader: TextView
    private lateinit var adapter: SongAdapter
    private lateinit var repository: SongRepository
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SongRepository(requireContext())
        
        initViews(view)
        setupRecyclerView()
        setupSearchView()
        
        // Carga inicial automática (Simula recomendaciones)
        performSearch("Viral 50 Global") 
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvSongs)
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        searchView = view.findViewById(R.id.searchView)
        tvHeader = view.findViewById(R.id.tvHeader)
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(emptyList()) { song ->
            val intent = Intent(requireContext(), SongLearningActivity::class.java)
            intent.putExtra("song_title", song.title)
            intent.putExtra("song_artist", song.artist)
            intent.putExtra("song_image", song.imageUrl)
            intent.putExtra("song_audio", song.previewUrl)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSearchView() {
        // Configurar el EditText interno del SearchView
        val searchEditText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.apply {
            // Evitar popup de sugerencias del teclado
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            // Configurar acción del teclado
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            // Asegurar que se muestren las letras mientras escribe
            isSingleLine = true
        }
        
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
                // Cancelar búsqueda anterior
                searchJob?.cancel()
                
                when {
                    newText.isNullOrEmpty() -> {
                        // Si está vacío, volver a la vista inicial
                        performSearch("Viral 50 Global")
                    }
                    newText.length >= 2 -> {
                        // Mostrar shimmer INMEDIATAMENTE
                        showLoading(true)
                        
                        // Debounce: esperar 300ms antes de buscar (más rápido)
                        searchJob = lifecycleScope.launch {
                            delay(300)
                            performSearch(newText, showShimmer = false) // No mostrar shimmer de nuevo
                        }
                    }
                    else -> {
                        // Si solo hay 1 carácter, ocultar resultados
                        showLoading(false)
                        adapter.updateData(emptyList())
                    }
                }
                return true
            }
        })
        
        // Detectar cuando se cierra la búsqueda (X button)
        searchView.setOnCloseListener {
            searchJob?.cancel()
            performSearch("Viral 50 Global")
            false
        }
    }

    private fun performSearch(query: String, showShimmer: Boolean = true) {
        tvHeader.text = if(query == "Viral 50 Global") "🎧 Descubrir" else "Resultados para '$query'"
        
        if (showShimmer) {
            showLoading(true)
        }
        
        lifecycleScope.launch {
            try {
                val response = repository.searchSongs(query)
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!
                    adapter.updateData(results)
                    
                    // Si no hay resultados, mostrar mensaje
                    if (results.isEmpty() && query != "Viral 50 Global") {
                        Toast.makeText(requireContext(), "No se encontraron canciones", Toast.LENGTH_SHORT).show()
                    }
                } else {
                     Toast.makeText(requireContext(), "Error al buscar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { 
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally { 
                showLoading(false) 
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            shimmerContainer.startShimmer()
            shimmerContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            shimmerContainer.stopShimmer()
            shimmerContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
