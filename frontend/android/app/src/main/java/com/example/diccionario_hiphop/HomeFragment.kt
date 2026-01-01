package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
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
        
        // Carga inicial
        loadTopSongs()
    }

    private fun initViews(view: View) {
        // Usamos tus IDs reales
        recyclerView = view.findViewById(R.id.rvSongs)
        progressBar = view.findViewById(R.id.progressBar)
        searchView = view.findViewById(R.id.searchView)
        tvHeader = view.findViewById(R.id.tvHeader)
        
        // Ocultar elementos antiguos que ya no se usan en la nueva UI
        view.findViewById<View>(R.id.fabMain)?.visibility = View.GONE
        view.findViewById<View>(R.id.viewDimmer)?.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(emptyList()) { song ->
            val intent = Intent(requireContext(), SongLearningActivity::class.java)
            // Pasamos los extras tal cual los tenías
            intent.putExtra("song_title", song.title)
            intent.putExtra("song_artist", song.artist)
            intent.putExtra("song_image", song.imageUrl)
            intent.putExtra("song_audio", song.previewUrl)
            
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
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
                val response = repository.getTopGrammy("en")
                if (response.isSuccessful && response.body() != null) {
                    adapter.updateData(response.body()!!)
                } else {
                    Toast.makeText(requireContext(), "Error cargando Top", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Silencioso
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
                    adapter.updateData(response.body()!!)
                } else {
                    Toast.makeText(requireContext(), "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error en la búsqueda", Toast.LENGTH_SHORT).show()
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