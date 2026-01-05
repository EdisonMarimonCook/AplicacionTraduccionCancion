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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var swipeRefresh: SwipeRefreshLayout  // 🔄 Pull-to-refresh
    private var searchJob: Job? = null
    private var userLanguages: List<LearningLanguage> = emptyList()
    private var selectedLanguage: String = "en"
    private var selectedLevel: String = "B1"
    private lateinit var chipGroup: com.google.android.material.chip.ChipGroup
    private lateinit var chipScrollView: android.widget.HorizontalScrollView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SongRepository(requireContext())
        
        initViews(view)
        setupRecyclerView()
        setupSearchView()
        
        // Cargar perfil para obtener idioma del usuario
        loadUserProfile()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar idiomas cuando volvemos de ManageLanguagesActivity
        loadUserProfile()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvSongs)
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        searchView = view.findViewById(R.id.searchView)
        tvHeader = view.findViewById(R.id.tvHeader)
        chipGroup = view.findViewById(R.id.chipGroupLanguages)
        chipScrollView = view.findViewById(R.id.chipScrollView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)  // 🔄 Pull-to-refresh
        
        // Configurar SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            loadUserProfile()  // Recargar perfil y recomendaciones
        }
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
                    // Ocultar chips en búsquedas personalizadas
                    chipScrollView.visibility = View.GONE
                }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                // Cancelar búsqueda anterior
                searchJob?.cancel()
                
                when {
                    newText.isNullOrEmpty() -> {
                        // Si está vacío, volver a recomendaciones personalizadas
                        chipScrollView.visibility = View.VISIBLE
                        performSearch(getRecommendationQuery(selectedLanguage, selectedLevel), isRecommendation = true)
                    }
                    newText.length >= 2 -> {
                        // Mostrar shimmer INMEDIATAMENTE
                        showLoading(true)
                        // Ocultar chips durante búsqueda
                        chipScrollView.visibility = View.GONE
                        
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
            chipScrollView.visibility = View.VISIBLE
            performSearch(getRecommendationQuery(selectedLanguage, selectedLevel), isRecommendation = true)
            false
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(requireContext())
                val response = apiService.getProfile()
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    userLanguages = user.learningLanguages?.filter { it.isActive } ?: emptyList()
                    
                    if (userLanguages.isNotEmpty()) {
                        // Encontrar idioma principal
                        val primaryLang = userLanguages.find { it.language == user.primaryLanguage } 
                            ?: userLanguages.first()
                        
                        selectedLanguage = primaryLang.language
                        selectedLevel = primaryLang.level
                        
                        // Crear chips dinámicos
                        setupLanguageChips()
                        
                        // Cargar recomendaciones según idioma y nivel
                        performSearch(getRecommendationQuery(selectedLanguage, selectedLevel), isRecommendation = true)
                    } else {
                        performSearch("Viral 50 Global", isRecommendation = true)
                    }
                } else {
                    performSearch("Viral 50 Global", isRecommendation = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                performSearch("Viral 50 Global", isRecommendation = true)
            } finally {
                swipeRefresh.isRefreshing = false  // 🔄 Detener animación de refresh
            }
        }
    }
    
    private fun setupLanguageChips() {
        chipGroup.removeAllViews()
        
        userLanguages.forEach { lang ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = "${getLanguageEmoji(lang.language)} ${getLanguageName(lang.language)} (${lang.level})"
            chip.isCheckable = true
            chip.setChipBackgroundColorResource(R.color.chip_background_selector)
            chip.setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
            
            // Seleccionar el idioma actual
            if (lang.language == selectedLanguage) {
                chip.isChecked = true
            }
            
            chip.setOnClickListener {
                selectedLanguage = lang.language
                selectedLevel = lang.level
                performSearch(getRecommendationQuery(selectedLanguage, selectedLevel), isRecommendation = true)
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
    
    private fun getRecommendationQuery(lang: String, level: String): String {
        // Búsqueda INTELIGENTE por género/playlists para evitar duplicados
        return when(lang) {
            "es" -> when(level) {
                "A1", "A2" -> "Top 50 Spain"  // Español claro y popular
                "B1", "B2" -> "Reggaeton Latino"  // Más dinámico
                else -> "Rock en Español"
            }
            "fr" -> when(level) {
                "A1", "A2" -> "Top 50 France"  // Francés estándar
                "B1", "B2" -> "Chanson Française"
                else -> "Rap Français"
            }
            "de" -> when(level) {
                "A1", "A2" -> "Top 50 Germany"  // Alemán claro
                "B1", "B2" -> "Deutschpop"
                else -> "Neue Deutsche Welle"
            }
            "pt" -> when(level) {
                "A1", "A2" -> "Top 50 Brazil"  // Portugués brasileño
                "B1", "B2" -> "Samba MPB"
                else -> "Funk Brasileiro"
            }
            "it" -> when(level) {
                "A1", "A2" -> "Top 50 Italy"  // Italiano estándar
                "B1", "B2" -> "Pop Italiano"
                else -> "Indie Italiano"
            }
            "ja" -> when(level) {
                "N5", "N4" -> "J-Pop Hits"  // Japonés claro y popular
                "N3", "N2" -> "Japanese City Pop"  // Más complejo
                else -> "J-Rock Anime"
            }
            "ko" -> when(level) {
                "1", "2" -> "K-Pop Daebak"  // K-Pop mainstream
                "3", "4" -> "Korean Indie"  // Más variado
                else -> "K-Hip Hop"
            }
            "zh" -> when(level) {
                "1", "2" -> "Mandopop"  // Mandarín estándar
                "3", "4" -> "C-Pop Hits"  // Chino popular
                else -> "Taiwan Pop"
            }
            else -> when(level) {  // Inglés
                "A1", "A2" -> "Today's Top Hits"  // Inglés claro
                "B1", "B2" -> "Pop Rising"
                else -> "RapCaviar"
            }
        }
    }

    private fun performSearch(query: String, showShimmer: Boolean = true, isRecommendation: Boolean = false) {
        // Determinar si es una búsqueda de recomendaciones automática
        val isAutoRecommendation = isRecommendation || 
            query.startsWith("Viral 50") || 
            query.startsWith("Top ") ||
            query.contains("BTS") ||
            query.contains("YOASOBI") ||
            query.contains("Shakira") ||
            query.contains("Stromae") ||
            query.contains("Nena") ||
            query.contains("Anitta") ||
            query.contains("Pausini") ||
            query.contains("周杰伦") ||
            query.contains("邓紫棋")
        
        tvHeader.text = if(isAutoRecommendation) "🎧 Descubrir" else "Resultados para '$query'"
        
        if (showShimmer) {
            showLoading(true)
        }
        
        lifecycleScope.launch {
            try {
                val response = repository.searchSongs(query)
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!
                    adapter.updateData(results)
                    
                    // Si no hay resultados, mostrar mensaje (excepto en recomendaciones)
                    val isRecommendation = query.startsWith("Viral 50")
                    if (results.isEmpty() && !isRecommendation) {
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
