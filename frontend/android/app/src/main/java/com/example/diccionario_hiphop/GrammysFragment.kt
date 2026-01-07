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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.diccionario_hiphop.utils.NetworkMonitor
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.launch

class GrammysFragment : Fragment(R.layout.fragment_home) { // Reutilizamos layout

    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var tvHeader: TextView
    private lateinit var tvOfflineMessage: TextView
    private lateinit var adapter: SongAdapter
    private lateinit var repository: SongRepository
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var swipeRefresh: SwipeRefreshLayout  // 🔄 Pull-to-refresh
    private var userLanguages: List<LearningLanguage> = emptyList()
    private var selectedLanguage: String = "en"
    private lateinit var chipGroup: com.google.android.material.chip.ChipGroup

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SongRepository(requireContext())
        networkMonitor = NetworkMonitor.getInstance(requireContext())
        
        initViews(view)
        setupRecyclerView()
        
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
            loadUserProfileAndGrammys()
        } else {
            showOfflineState()
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvSongs)
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        tvHeader = view.findViewById(R.id.tvHeader)
        tvOfflineMessage = view.findViewById(R.id.tvOfflineMessage)
        chipGroup = view.findViewById(R.id.chipGroupLanguages)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)  // 🔄 Pull-to-refresh
        
        // OCULTAR BUSCADOR EN ESTA PANTALLA
        view.findViewById<SearchView>(R.id.searchView).visibility = View.GONE
        
        tvHeader.text = "🏆 Top Hits & Grammys"
        
        // Configurar SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener {
            loadUserProfileAndGrammys()  // Recargar todo
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

    private fun loadUserProfileAndGrammys() {
        // Re-habilitar controles (por si volvimos de offline)
        swipeRefresh.isEnabled = true
        chipGroup.visibility = View.VISIBLE
        tvHeader.visibility = View.VISIBLE
        tvOfflineMessage.visibility = View.GONE
        
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
            } finally {
                swipeRefresh.isRefreshing = false  // 🔄 Detener animación
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
    
    private fun showOfflineState() {
        // Ocultar loading
        showLoading(false)
        
        // Limpiar resultados
        adapter.updateData(emptyList())
        
        // Ocultar chips y header
        chipGroup.visibility = View.GONE
        tvHeader.visibility = View.GONE
        
        // Mostrar mensaje centrado
        tvOfflineMessage.visibility = View.VISIBLE
        
        // Detener refresh si está activo
        swipeRefresh.isRefreshing = false
        swipeRefresh.isEnabled = false
    }
}