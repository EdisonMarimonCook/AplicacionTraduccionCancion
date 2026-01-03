package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.launch

class GrammysFragment : Fragment(R.layout.fragment_home) { // Reutilizamos layout

    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var tvHeader: TextView
    private lateinit var adapter: SongAdapter
    private lateinit var repository: SongRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SongRepository(requireContext())
        
        initViews(view)
        setupRecyclerView()
        loadGrammyNominees()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvSongs)
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        tvHeader = view.findViewById(R.id.tvHeader)
        
        // OCULTAR BUSCADOR EN ESTA PANTALLA
        view.findViewById<SearchView>(R.id.searchView).visibility = View.GONE
        
        tvHeader.text = "🏆 Canciones Nominadas a los Grammys"
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

    private fun loadGrammyNominees() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = repository.getTopGrammy("en") 
                if (response.isSuccessful && response.body() != null) {
                    adapter.updateData(response.body()!!)
                }
            } catch (e: Exception) { 
                e.printStackTrace()
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