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
import com.example.diccionario_hiphop.utils.NetworkMonitor
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var searchView: SearchView
    private lateinit var tvHeader: TextView
    private lateinit var tvOfflineMessage: TextView
    private lateinit var adapter: SongAdapter
    private lateinit var repository: SongRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var networkMonitor: NetworkMonitor
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
        profileRepository = ProfileRepository(requireContext())
        networkMonitor = NetworkMonitor.getInstance(requireContext())
        
        initViews(view)
        setupRecyclerView()
        setupSearchView()
        
        // 🌐 Observar cambios de conectividad para actualizar UI
        lifecycleScope.launch {
            networkMonitor.isConnected.collect { isOnline ->
                if (!isOnline) {
                    showOfflineState()
                }
                // No cargar automáticamente cuando vuelve online - onResume lo hará
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Verificar conectividad y cargar solo si hay conexión
        if (networkMonitor.isConnected.value) {
            loadUserProfile()
        } else {
            showOfflineState()
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvSongs)
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        searchView = view.findViewById(R.id.searchView)
        tvHeader = view.findViewById(R.id.tvHeader)
        tvOfflineMessage = view.findViewById(R.id.tvOfflineMessage)
        chipGroup = view.findViewById(R.id.chipGroupLanguages)
        chipScrollView = view.findViewById(R.id.chipScrollView)
        chipScrollView.isSmoothScrollingEnabled = true
        swipeRefresh = view.findViewById(R.id.swipeRefresh)  // 🔄 Pull-to-refresh
        
        // Configurar SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            loadUserProfile()  // Recargar perfil y recomendaciones
        }
        
        // 🔥 Mejorar scroll de chips: evitar conflicto con ViewPager2
        chipScrollView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_MOVE -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            false
        }
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(emptyList(), { song ->
            // Validar idioma antes de abrir la actividad
            checkLanguageBeforeOpening(song)
        }, selectedLanguage)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * Valida si el idioma de la canción está en los idiomas de aprendizaje del usuario
     */
    private fun checkLanguageBeforeOpening(song: SongItem) {
        val isLanguageActive = userLanguages.any { it.language == selectedLanguage && it.isActive }
        
        if (!isLanguageActive) {
            // Mostrar diálogo de advertencia
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Idioma no configurado")
                .setMessage("Esta canción está en ${getLanguageName(selectedLanguage)}, pero no lo tienes como idioma de aprendizaje activo.\n\n¿Quieres continuar? No podrás guardar palabras ni crear flashcards.")
                .setPositiveButton("Continuar de todos modos") { _, _ ->
                    openSongLearningActivity(song, allowSaving = false)
                }
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Configurar idiomas") { _, _ ->
                    // Abrir pantalla de gestión de idiomas
                    val intent = Intent(requireContext(), ManageLanguagesActivity::class.java)
                    startActivity(intent)
                }
                .show()
        } else {
            // Idioma válido, abrir normalmente
            openSongLearningActivity(song, allowSaving = true)
        }
    }

    /**
     * Abre SongLearningActivity con los datos de la canción
     */
    private fun openSongLearningActivity(song: SongItem, allowSaving: Boolean) {
        val intent = Intent(requireContext(), SongLearningActivity::class.java)
        intent.putExtra("song_title", song.title)
        intent.putExtra("song_artist", song.artist)
        intent.putExtra("song_image", song.imageUrl)
        intent.putExtra("song_audio", song.previewUrl)
        intent.putExtra("allow_saving", allowSaving)
        intent.putExtra("language_code", selectedLanguage)
        startActivity(intent)
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
        // Re-habilitar controles (por si volvimos de offline)
        searchView.isEnabled = true
        searchView.alpha = 1f
        swipeRefresh.isEnabled = true
        chipScrollView.visibility = View.VISIBLE
        tvHeader.visibility = View.VISIBLE
        tvOfflineMessage.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                // 🚀 Usar ProfileRepository que aprovecha la precarga de MainActivity
                val response = profileRepository.getProfile()
                
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
                // Recrear adapter con nuevo idioma para Grammy checks
                setupRecyclerView()
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
                    // Solo mostrar error si estamos online
                    if (networkMonitor.isConnected.value) {
                        Toast.makeText(requireContext(), "Error al buscar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { 
                e.printStackTrace()
                // Solo mostrar error si estamos online
                if (networkMonitor.isConnected.value) {
                    Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
                }
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
    
    private fun showOfflineState() {
        // Ocultar loading
        showLoading(false)
        
        // Limpiar resultados
        adapter.updateData(emptyList())
        
        // Deshabilitar búsqueda
        searchView.isEnabled = false
        searchView.alpha = 0.5f
        
        // Ocultar chips y header
        chipScrollView.visibility = View.GONE
        tvHeader.visibility = View.GONE
        
        // Mostrar mensaje centrado
        tvOfflineMessage.visibility = View.VISIBLE
        
        // Detener refresh si está activo
        swipeRefresh.isRefreshing = false
        swipeRefresh.isEnabled = false
    }
}
