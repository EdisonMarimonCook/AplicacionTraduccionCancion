package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyState: TextView
    private lateinit var backButton: ImageButton

    private val songRepository = SongRepository()
    private val adapter = SongAdapter()
    private var allSongs: List<Song> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        initializeViews()
        setupRecyclerView()
        setupSearchView()
        setupClickListeners()
        loadTopSongs()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.songsRecyclerView)
        searchView = findViewById(R.id.searchView)
        loadingProgress = findViewById(R.id.loadingProgress)
        emptyState = findViewById(R.id.emptyStateText)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener { song ->
            openSongLearning(song)
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSongs(newText ?: "")
                return true
            }
        })
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadTopSongs() {
        showLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val songs = withContext(Dispatchers.IO) {
                    songRepository.getTopSongs("es") // Idioma por defecto español
                }

                allSongs = songs
                adapter.submitList(songs)
                showEmptyState(songs.isEmpty())
                showLoading(false)

            } catch (e: Exception) {
                showLoading(false)
                showError("Error cargando canciones: ${e.message}")
                showEmptyState(true)
            }
        }
    }

    private fun filterSongs(query: String) {
        val filteredSongs = if (query.isBlank()) {
            allSongs
        } else {
            allSongs.filter { song ->
                song.title.contains(query, true) || song.artist.contains(query, true)
            }
        }

        adapter.submitList(filteredSongs)
        showEmptyState(filteredSongs.isEmpty())
    }

    private fun openSongLearning(song: Song) {
        val intent = Intent(this, SongLearningActivity::class.java).apply {
            putExtra("song_title", song.title)
            putExtra("song_artist", song.artist)
            putExtra("song_album_cover", song.albumCover)
            putExtra("song_rank", song.spotifyRank)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        emptyState.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}