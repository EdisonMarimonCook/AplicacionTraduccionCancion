package com.example.diccionario_hiphop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var rvLanguages: RecyclerView
    private lateinit var btnAddLanguage: Button
    private lateinit var progressBar: ProgressBar
    
    private var userLanguages = mutableListOf<LearningLanguage>()
    private var primaryLanguageCode = "en"
    private lateinit var adapter: ManageLanguagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_languages)

        supportActionBar?.apply {
            title = "Gestionar idiomas"
            setDisplayHomeAsUpEnabled(true)
        }

        initViews()
        loadUserLanguages()
    }

    private fun initViews() {
        rvLanguages = findViewById(R.id.rvManageLanguages)
        btnAddLanguage = findViewById(R.id.btnAddLanguage)
        progressBar = findViewById(R.id.progressBar)

        rvLanguages.layoutManager = LinearLayoutManager(this)
        
        btnAddLanguage.setOnClickListener {
            showAddLanguageDialog()
        }
    }

    private fun loadUserLanguages() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                val response = apiService.getProfile()

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    primaryLanguageCode = user.primaryLanguage
                    userLanguages = user.learningLanguages?.toMutableList() ?: mutableListOf()
                    
                    adapter = ManageLanguagesAdapter(
                        userLanguages,
                        primaryLanguageCode,
                        onToggleActive = { language -> toggleLanguageActive(language) },
                        onSetPrimary = { language -> setPrimaryLanguage(language) },
                        onEditGoal = { language -> editDailyGoal(language) }
                    )
                    rvLanguages.adapter = adapter
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageLanguagesActivity, "Error cargando idiomas", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun toggleLanguageActive(language: LearningLanguage) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                val response = apiService.toggleLanguage(language.language)
                
                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Idioma actualizado"
                    Toast.makeText(this@ManageLanguagesActivity, message, Toast.LENGTH_SHORT).show()
                    loadUserLanguages()  // Recargar
                } else {
                    Toast.makeText(this@ManageLanguagesActivity, "Error actualizando idioma", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageLanguagesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setPrimaryLanguage(language: LearningLanguage) {
        // Reordenar poniendo este idioma primero
        val newOrder = mutableListOf(language.language)
        newOrder.addAll(userLanguages.filter { it.language != language.language }.map { it.language })
        
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                val request = ReorderLanguagesRequest(newOrder)
                val response = apiService.reorderLanguages(request)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageLanguagesActivity, "✅ ${getLanguageName(language.language)} es ahora tu idioma principal", Toast.LENGTH_SHORT).show()
                    loadUserLanguages()  // Recargar
                } else {
                    Toast.makeText(this@ManageLanguagesActivity, "Error cambiando idioma principal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageLanguagesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun editDailyGoal(language: LearningLanguage) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(language.dailyGoal.toString())
        }

        AlertDialog.Builder(this)
            .setTitle("Meta diaria - ${getLanguageName(language.language)}")
            .setMessage("¿Cuántas palabras quieres aprender por día?")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newGoal = input.text.toString().toIntOrNull() ?: 10
                
                lifecycleScope.launch {
                    try {
                        val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                        val request = UpdateDailyGoalRequest(newGoal)
                        val response = apiService.updateDailyGoal(language.language, request)
                        
                        if (response.isSuccessful) {
                            Toast.makeText(this@ManageLanguagesActivity, "✅ Meta actualizada", Toast.LENGTH_SHORT).show()
                            loadUserLanguages()  // Recargar
                        } else {
                            Toast.makeText(this@ManageLanguagesActivity, "Error actualizando meta", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ManageLanguagesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddLanguageDialog() {
        val supportedLanguages = listOf(
            "en" to "🇬🇧 Inglés",
            "es" to "🇪🇸 Español",
            "fr" to "🇫🇷 Francés",
            "de" to "🇩🇪 Alemán",
            "it" to "🇮🇹 Italiano",
            "pt" to "🇵🇹 Portugués",
            "ja" to "🇯🇵 Japonés",
            "zh" to "🇨🇳 Chino",
            "ko" to "🇰🇷 Coreano"
        )

        val availableLanguages = supportedLanguages.filter { (code, _) ->
            userLanguages.none { it.language == code }
        }

        if (availableLanguages.isEmpty()) {
            Toast.makeText(this, "Ya tienes todos los idiomas disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val items = availableLanguages.map { it.second }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Añadir idioma")
            .setItems(items) { _, which ->
                val (code, name) = availableLanguages[which]
                showLevelSelectionDialog(code, name)
            }
            .show()
    }

    private fun showLevelSelectionDialog(code: String, name: String) {
        val levels = when (code) {
            "ja" -> listOf("N5", "N4", "N3", "N2", "N1")
            "zh" -> listOf("1", "2", "3", "4", "5", "6")
            "ko" -> listOf("1", "2", "3", "4", "5", "6")
            else -> listOf("A1", "A2", "B1", "B2", "C1", "C2")
        }

        AlertDialog.Builder(this)
            .setTitle("Selecciona tu nivel en $name")
            .setItems(levels.toTypedArray()) { _, which ->
                val level = levels[which]
                addNewLanguage(code, level)
            }
            .show()
    }
    
    private fun addNewLanguage(code: String, level: String) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                val request = AddLanguageRequest(code, level)
                val response = apiService.addLanguage(request)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageLanguagesActivity, "✅ Idioma añadido correctamente", Toast.LENGTH_SHORT).show()
                    loadUserLanguages()  // Recargar
                } else {
                    Toast.makeText(this@ManageLanguagesActivity, "Error añadiendo idioma", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageLanguagesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // Adapter para gestionar idiomas
    inner class ManageLanguagesAdapter(
        private val languages: List<LearningLanguage>,
        private val primaryLanguageCode: String,
        private val onToggleActive: (LearningLanguage) -> Unit,
        private val onSetPrimary: (LearningLanguage) -> Unit,
        private val onEditGoal: (LearningLanguage) -> Unit
    ) : RecyclerView.Adapter<ManageLanguagesAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvLanguageName: TextView = view.findViewById(R.id.tvManageLanguageName)
            val tvStats: TextView = view.findViewById(R.id.tvManageStats)
            val switchActive: SwitchMaterial = view.findViewById(R.id.switchActive)
            val btnPrimary: Button = view.findViewById(R.id.btnSetPrimary)
            val btnGoal: Button = view.findViewById(R.id.btnEditGoal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_manage_language, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val lang = languages[position]
            val flag = getLanguageFlag(lang.language)
            val name = getLanguageName(lang.language)

            holder.tvLanguageName.text = "$flag $name • ${lang.level}"
            holder.tvStats.text = "${lang.wordsLearned} palabras | Meta: ${lang.dailyGoal}/día"
            
            holder.switchActive.isChecked = lang.isActive
            holder.switchActive.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != lang.isActive) {
                    onToggleActive(lang)
                }
            }

            if (lang.language == primaryLanguageCode) {
                holder.btnPrimary.text = "✓ Principal"
                holder.btnPrimary.isEnabled = false
            } else {
                holder.btnPrimary.text = "Hacer principal"
                holder.btnPrimary.isEnabled = true
                holder.btnPrimary.setOnClickListener { onSetPrimary(lang) }
            }

            holder.btnGoal.setOnClickListener { onEditGoal(lang) }
        }

        override fun getItemCount() = languages.size

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
    }
}
