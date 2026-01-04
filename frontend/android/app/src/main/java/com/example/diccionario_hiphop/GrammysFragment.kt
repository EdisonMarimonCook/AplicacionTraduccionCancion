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
    private var userLanguages: List<LearningLanguage> = emptyList()
    private var selectedLanguage: String = "en"
    private lateinit var chipGroup: com.google.android.material.chip.ChipGroup

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SongRepository(requireContext())
        
        initViews(view)
        setupRecyclerView()
        loadUserProfileAndGrammys()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar idiomas cuando volvemos de ManageLanguagesActivity
        loadUserProfileAndGrammys()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvSongs)
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        tvHeader = view.findViewById(R.id.tvHeader)
        chipGroup = view.findViewById(R.id.chipGroupLanguages)
        
        // OCULTAR BUSCADOR EN ESTA PANTALLA
        view.findViewById<SearchView>(R.id.searchView).visibility = View.GONE
        
        tvHeader.text = "🏆 Top Hits & Grammys"
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

    private fun loadUserProfileAndGrammys() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // 1. Obtener idiomas del usuario
                val apiService = RetrofitService.getInstance(requireContext())
                val profileResponse = apiService.getProfile()
                
                if (profileResponse.isSuccessful && profileResponse.body() != null) {
                    val user = profileResponse.body()!!
                    userLanguages = user.learningLanguages?.filter { it.isActive } ?: emptyList()
                    
                    if (userLanguages.isNotEmpty()) {
                        // Encontrar idioma principal
                        val primaryLang = userLanguages.find { it.language == user.primaryLanguage } 
                            ?: userLanguages.first()
                        
                        selectedLanguage = primaryLang.language
                        
                        // Crear chips dinámicos
                        setupLanguageChips()
                    }
                }
                
                // 2. Cargar Grammys con el idioma seleccionado
                loadGrammyNominees()
            } catch (e: Exception) { 
                e.printStackTrace()
                loadGrammyNominees()
            }
        }
    }
    
    private fun loadGrammyNominees() {
        lifecycleScope.launch {
            try {
                val response = repository.getTopGrammy(selectedLanguage) 
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
    
    private fun setupLanguageChips() {
        chipGroup.removeAllViews()
        
        userLanguages.forEach { lang ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = "${getLanguageEmoji(lang.language)} ${getLanguageName(lang.language)}"
            chip.isCheckable = true
            chip.setChipBackgroundColorResource(R.color.chip_background_selector)
            chip.setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
            
            // Seleccionar el idioma actual
            if (lang.language == selectedLanguage) {
                chip.isChecked = true
            }
            
            chip.setOnClickListener {
                selectedLanguage = lang.language
                showLoading(true)
                loadGrammyNominees()
            }
            
            chipGroup.addView(chip)
        }
    }
    
    private fun getLanguageEmoji(code: String): String {
        return when(code) {
            "es" -> "🇪🇸"
            "fr" -> "🇫🇷"
            "de" -> "🇩🇪"
            "pt" -> "🇵🇹"
            "it" -> "🇮🇹"
            "ja" -> "🇯🇵"
            "ko" -> "🇰🇷"
            "zh" -> "🇨🇳"
            else -> "🇬🇧"
        }
    }
    
    private fun getLanguageName(code: String): String {
        return when(code) {
            "es" -> "Español"
            "fr" -> "Francés"
            "de" -> "Alemán"
            "pt" -> "Portugués"
            "it" -> "Italiano"
            "ja" -> "Japonés"
            "ko" -> "Coreano"
            "zh" -> "Chino"
            else -> "Inglés"
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