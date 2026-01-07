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
    private lateinit var profileRepository: ProfileRepository  // 🆕 Para invalidar caché
    private lateinit var networkMonitor: com.example.diccionario_hiphop.utils.NetworkMonitor
    private lateinit var offlineManager: com.example.diccionario_hiphop.utils.OfflineManager  // 🗑️ Para borrar flashcards
    
    // 🔥 Estado de filtros
    private var selectedLanguage: String? = null
    private var selectedType: String? = null
    private var userLanguages: List<LearningLanguage> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = DictionaryRepository(requireContext())
        profileRepository = ProfileRepository(requireContext())  // 🆕 Inicializar
        networkMonitor = com.example.diccionario_hiphop.utils.NetworkMonitor.getInstance(requireContext())
        offlineManager = com.example.diccionario_hiphop.utils.OfflineManager(requireContext())  // 🗑️ Inicializar

        initViews(view)
        setupRecyclerView()
        loadUserLanguages()
    }
    
    override fun onResume() {
        super.onResume()
        // Sincronizar borrados pendientes si hay conexión
        if (networkMonitor.isConnected.value) {
            syncPendingDeletions()
        }
    }
    
    private fun syncPendingDeletions() {
        lifecycleScope.launch {
            try {
                val synced = repository.syncPendingDeletions()
                if (synced > 0) {
                    Toast.makeText(requireContext(), "✓ $synced palabras sincronizadas", Toast.LENGTH_SHORT).show()
                    loadDictionary() // Recargar para actualizar UI
                }
            } catch (e: Exception) {
                // Silencioso si falla
            }
        }
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
        adapter = DictionaryAdapter(
            mutableListOf(),
            onDeleteClick = { word ->
                // Mostrar diálogo de confirmación
                showDeleteConfirmation(word)
            },
            onAudioClick = { word ->
                // 🎵 Reproducir audio
                playWordAudio(word)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun showDeleteConfirmation(word: UserWord) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("¿Eliminar palabra?")
            .setMessage("¿Estás seguro de que quieres eliminar \"${word.word}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteWord(word)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun deleteWord(word: UserWord) {
        lifecycleScope.launch {
            try {
                val response = repository.deleteWord(word.id)
                
                if (response.isSuccessful) {
                    // Actualizar UI
                    adapter.removeWord(word)
                    
                    // 🆕 Decrementar contador en caché del perfil
                    profileRepository.decrementCachedCounter("words", 1)
                    
                    // 🗑️ Eliminar flashcard asociada de la caché offline
                    offlineManager.deleteFlashcard(word.id)
                    
                    val isOnline = networkMonitor.isConnected.value
                    val message = if (isOnline) {
                        "Palabra eliminada"
                    } else {
                        "Palabra eliminada (se sincronizará al conectar)"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // No mostrar toast de error de conexión en modo offline
            }
        }
    }
    
    /**
     * 🎵 REPRODUCIR AUDIO DE UNA PALABRA
     * Usa AudioPlayerHelper con prioridad: fragmento original > TTS
     */
    private fun playWordAudio(word: UserWord) {
        lifecycleScope.launch {
            try {
                AudioPlayerHelper.playAudio(
                    context = requireContext(),
                    text = word.word,
                    language = word.language,
                    songYoutubeUrl = word.songYoutubeUrl,
                    timestampStart = word.timestampStart,
                    timestampEnd = word.timestampEnd
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al reproducir audio", Toast.LENGTH_SHORT).show()
            }
        }
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
                    // Solo mostrar error si estamos online
                    val isOnline = networkMonitor.isConnected.value
                    if (isOnline) {
                        Toast.makeText(requireContext(), "Error cargando idiomas", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // No mostrar toast en modo offline
                val isOnline = networkMonitor.isConnected.value
                if (isOnline) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
                        val isOnline = networkMonitor.isConnected.value
                        tvEmpty.text = if (isOnline) {
                            "No tienes palabras guardadas aún"
                        } else {
                            "📴 Sin conexión\nNo hay datos guardados offline"
                        }
                        tvEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.updateData(words)
                    }
                }
            } catch (e: Exception) {
                // Error silencioso - el banner ya muestra que no hay conexión
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        AudioPlayerHelper.releasePlayer()
    }
}