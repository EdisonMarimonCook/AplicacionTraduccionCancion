package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.diccionario_hiphop.utils.NetworkMonitor
import kotlinx.coroutines.launch

class GrammysFragment : Fragment(R.layout.fragment_grammys) {

    private lateinit var repository: SongRepository
    private lateinit var networkMonitor: NetworkMonitor

    // UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutOffline: View

    // 🔥 Referencia al adaptador externo
    private lateinit var adapter: GrammysAdapter

    private var selectedLanguage: String = "en"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = SongRepository(requireContext())
        networkMonitor = NetworkMonitor.getInstance(requireContext())

        initViews(view)
        setupRecyclerView()

        // Lógica de carga
        lifecycleScope.launch {
            networkMonitor.isConnected.collect { isOnline ->
                if (isOnline) loadGrammysFromApi() else showOfflineState(true)
            }
        }
    }

    private fun initViews(view: View) {
        // Asegúrate que estos ID coinciden con fragment_grammys.xml
        recyclerView = view.findViewById(R.id.rvGrammySongs)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        layoutOffline = view.findViewById(R.id.layoutOffline)

        swipeRefresh.setOnRefreshListener {
            if (networkMonitor.isConnected.value == true) loadGrammysFromApi()
            else {
                swipeRefresh.isRefreshing = false
                showOfflineState(true)
            }
        }
    }

    private fun setupRecyclerView() {
        // 1. Configurar GRID de 2 columnas (Aspecto estantería)
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.setHasFixedSize(true)

        // 2. Inicializar el adaptador vacío (para evitar errores de null)
        adapter = GrammysAdapter(emptyList()) { songItem ->
            navigateToSong(songItem)
        }
        recyclerView.adapter = adapter
    }

    private fun loadGrammysFromApi() {
        showLoading(true)
        showOfflineState(false)

        lifecycleScope.launch {
            try {
                val response = repository.getTopGrammy(selectedLanguage)

                if (response.isSuccessful && response.body() != null) {
                    val songs = response.body()!!

                    if (songs.isNotEmpty()) {
                        // Pasamos los datos al adaptador
                        adapter.updateList(songs)

                        recyclerView.visibility = View.VISIBLE
                        // Animación suave de entrada
                        recyclerView.alpha = 0f
                        recyclerView.animate().alpha(1f).duration = 300
                    } else {
                        showOfflineState(true)
                    }
                } else {
                    showOfflineState(true)
                }
            } catch (e: Exception) {
                showOfflineState(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun navigateToSong(item: SongItem) {
        val intent = Intent(requireContext(), SongLearningActivity::class.java).apply {
            putExtra("song_title", item.title)
            putExtra("song_artist", item.artist)
            putExtra("song_image", item.imageUrl)
            putExtra("song_audio", item.previewUrl)

            // 🏆 Activar tema Grammy
            putExtra("is_grammy_theme", true)
        }
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        swipeRefresh.isRefreshing = isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showOfflineState(isOffline: Boolean) {
        layoutOffline.visibility = if (isOffline) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isOffline) View.GONE else View.VISIBLE
        progressBar.visibility = View.GONE
    }
}