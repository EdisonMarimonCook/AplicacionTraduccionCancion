package com.example.diccionario_hiphop

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class DictionaryFragment : Fragment(R.layout.fragment_dictionary) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var chipGroupLanguages: ChipGroup
    private lateinit var chipGroupType: ChipGroup
    private lateinit var chipWords: Chip
    private lateinit var chipExpressions: Chip

    private lateinit var adapter: DictionaryAdapter
    private lateinit var repository: DictionaryRepository
    
    // 🔥 Estado de filtros
    private var selectedLanguage: String? = null
    private var selectedType: String? = null
    private var userLanguages: List<LearningLanguage> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = DictionaryRepository(requireContext())

        initViews(view)
        setupRecyclerView()
        loadUserLanguages()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvDictionary)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        chipGroupLanguages = view.findViewById(R.id.chipGroupLanguages)
        chipGroupType = view.findViewById(R.id.chipGroupType)
        chipWords = view.findViewById(R.id.chipWords)
        chipExpressions = view.findViewById(R.id.chipExpressions)
        
        // 🔥 Listeners para chips de tipo
        chipWords.setOnClickListener {
            selectedType = if (chipWords.isChecked) "word" else null
            loadDictionary()
        }
        
        chipExpressions.setOnClickListener {
            selectedType = if (chipExpressions.isChecked) "expression" else null
            loadDictionary()
        }
    }

    private fun setupRecyclerView() {
        adapter = DictionaryAdapter(mutableListOf()) { word ->
            // Acción al borrar (opcional por ahora)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun loadUserLanguages() {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(requireContext())
                val response = apiService.getProfile()
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    userLanguages = user.learningLanguages?.filter { it.isActive } ?: emptyList()
                    
                    // 🔥 Crear chips de idiomas dinámicamente
                    chipGroupLanguages.removeAllViews()
                    
                    userLanguages.forEach { lang ->
                        val chip = Chip(requireContext())
                        chip.text = "${getLanguageFlag(lang.language)} ${getLanguageName(lang.language)}"
                        chip.isCheckable = true
                        chip.setOnClickListener {
                            selectedLanguage = if (chip.isChecked) lang.language else null
                            loadDictionary()
                        }
                        chipGroupLanguages.addView(chip)
                    }
                    
                    // 🔥 Cargar diccionario (por defecto: primary_language)
                    loadDictionary()
                } else {
                    Toast.makeText(requireContext(), "Error cargando idiomas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDictionary() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val response = repository.getDictionary(selectedLanguage, selectedType)

                if (response.isSuccessful && response.body() != null) {
                    val words = response.body()!!
                    
                    if (words.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.updateData(words)
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al cargar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun getLanguageFlag(code: String): String {
        return when (code) {
            "en" -> "🇬🇧"
            "es" -> "🇪🇸"
            "fr" -> "🇫🇷"
            "de" -> "🇩🇪"
            "it" -> "🇮🇹"
            "pt" -> "🇵🇹"
            "ja" -> "🇯🇵"
            "zh" -> "🇨🇳"
            "ko" -> "🇰🇷"
            else -> "🌐"
        }
    }
    
    private fun getLanguageName(code: String): String {
        return when (code) {
            "en" -> "Inglés"
            "es" -> "Español"
            "fr" -> "Francés"
            "de" -> "Alemán"
            "it" -> "Italiano"
            "pt" -> "Portugués"
            "ja" -> "Japonés"
            "zh" -> "Chino"
            "ko" -> "Coreano"
            else -> code.uppercase()
        }
    }
}