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
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.diccionario_hiphop.utils.LevelIndicator
import com.example.diccionario_hiphop.utils.WindowInsetsHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

class ManageLanguagesActivity : AppCompatActivity() {

    private lateinit var rvLanguages: RecyclerView
    private lateinit var btnAddLanguage: Button
    private lateinit var progressBar: ProgressBar
    
    private var userLanguages = mutableListOf<LearningLanguage>()
    private var primaryLanguageCode = "en"
    private var nativeLanguageCode = "es"  // 🔥 Guardar idioma nativo
    private var adapter: ManageLanguagesAdapter? = null  // 🔥 Hacerlo nullable
    private var loadJob: Job? = null  // 🔥 Para cancelar recargas concurrentes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_languages)

        // 🔧 Aplicar WindowInsets para botón inferior
        val rootView = findViewById<View>(android.R.id.content)
        WindowInsetsHelper.applySystemBarInsets(rootView)

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
        // 🔥 Cancelar cualquier carga anterior pendiente
        loadJob?.cancel()
        
        progressBar.visibility = View.VISIBLE
        
        loadJob = lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                val response = apiService.getProfile()

                android.util.Log.d("ManageLanguages", "📥 getProfile: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    primaryLanguageCode = user.primaryLanguage
                    nativeLanguageCode = user.nativeLanguage  // 🔥 Guardar nativo
                    
                    val newLanguages = user.learningLanguages ?: emptyList()
                    
                    android.util.Log.d("ManageLanguages", "🌍 Total idiomas: ${newLanguages.size}")
                    newLanguages.forEach { lang ->
                        android.util.Log.d("ManageLanguages", "  - ${lang.language} (${lang.level}) active=${lang.isActive}")
                    }
                    
                    // 🔥 Actualizar lista sin recrear adapter
                    userLanguages.clear()
                    userLanguages.addAll(newLanguages)
                    
                    // 🔥 Crear adapter solo la primera vez
                    if (adapter == null) {
                        adapter = ManageLanguagesAdapter(
                            userLanguages,
                            primaryLanguageCode,
                            onToggleActive = { language -> toggleLanguageActive(language) },
                            onSetPrimary = { language -> setPrimaryLanguage(language) },
                            onEditGoal = { language -> editDailyGoal(language) },
                            onEditLevel = { language -> editLevel(language) }
                        )
                        rvLanguages.adapter = adapter
                        
                        // 🔥 Configurar drag & drop para reordenar (solo una vez)
                        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
                        ) {
                            override fun onMove(
                                recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder
                            ): Boolean {
                                val fromPos = viewHolder.adapterPosition
                                val toPos = target.adapterPosition
                                
                                // Mover en la lista local
                                val item = userLanguages.removeAt(fromPos)
                                userLanguages.add(toPos, item)
                                adapter?.notifyItemMoved(fromPos, toPos)
                                return true
                            }
                            
                            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                                // No hacer nada (swipe deshabilitado)
                            }
                            
                            // 🔥 Animación cuando se empieza a arrastrar
                            override fun onSelectedChanged(
                                viewHolder: RecyclerView.ViewHolder?,
                                actionState: Int
                            ) {
                                super.onSelectedChanged(viewHolder, actionState)
                                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                                    viewHolder?.itemView?.apply {
                                        alpha = 0.7f
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                    }
                                }
                            }
                            
                            // 🔥 Restaurar apariencia cuando se suelta
                            override fun clearView(
                                recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder
                            ) {
                                super.clearView(recyclerView, viewHolder)
                                viewHolder.itemView.apply {
                                    alpha = 1.0f
                                    scaleX = 1.0f
                                    scaleY = 1.0f
                                }
                                // Cuando termina el drag, enviar nuevo orden al backend
                                saveNewOrder()
                            }
                        })
                        itemTouchHelper.attachToRecyclerView(rvLanguages)
                    } else {
                        // 🔥 Solo actualizar primary y notificar cambios
                        adapter?.updatePrimaryLanguage(primaryLanguageCode)
                        adapter?.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ManageLanguages", "💀 Error: ${e.message}", e)
                Toast.makeText(this@ManageLanguagesActivity, "Error cargando idiomas", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun toggleLanguageActive(language: LearningLanguage) {
        // ⚠️ Validar que haya al menos 1 idioma activo
        val activeCount = userLanguages.count { it.isActive }
        
        if (language.isActive && activeCount == 1) {
            Toast.makeText(this, "⚠️ Debe haber al menos un idioma activo", Toast.LENGTH_LONG).show()
            return
        }
        
        // 🔥 Deshabilitar UI mientras se procesa
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                val response = apiService.toggleLanguage(language.language)
                
                if (response.isSuccessful) {
                    // ✅ Solo recargar, SIN reordenar (el backend ya maneja todo)
                    delay(300)
                    loadUserLanguages()
                } else {
                    Toast.makeText(this@ManageLanguagesActivity, "Error en toggle", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ManageLanguages", "💀 Error toggle: ${e.message}", e)
                Toast.makeText(this@ManageLanguagesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // 🔥 Restaurar UI
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun saveNewOrder() {
        // 🔥 Deduplicar antes de enviar
        val newOrder = userLanguages.map { it.language }.distinct()
        
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                val request = ReorderLanguagesRequest(newOrder)
                val response = apiService.reorderLanguages(request)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageLanguagesActivity, "✅ Orden guardado", Toast.LENGTH_SHORT).show()
                    // ✅ Recargar para reflejar primary_language actualizado
                    delay(500)
                    loadUserLanguages()
                } else {
                    Toast.makeText(this@ManageLanguagesActivity, "Error guardando orden", Toast.LENGTH_SHORT).show()
                    loadUserLanguages()  // Revertir si falló
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageLanguagesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                loadUserLanguages()  // Revertir si falló
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
                    delay(500)
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

    private fun editLevel(language: LearningLanguage) {
        val levels = when (language.language) {
            "ja" -> listOf("N5", "N4", "N3", "N2", "N1")
            "zh" -> listOf("1", "2", "3", "4", "5", "6")
            "ko" -> listOf("1", "2", "3", "4", "5", "6")
            else -> listOf("A1", "A2", "B1", "B2", "C1", "C2")
        }
        
        val currentIndex = levels.indexOf(language.level)
        
        AlertDialog.Builder(this)
            .setTitle("Nivel de ${getLanguageName(language.language)}")
            .setSingleChoiceItems(levels.toTypedArray(), currentIndex) { dialog, which ->
                val newLevel = levels[which]
                dialog.dismiss()
                
                lifecycleScope.launch {
                    try {
                        val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                        val request = UpdateLevelRequest(newLevel)
                        val response = apiService.updateLevel(language.language, request)
                        
                        if (response.isSuccessful) {
                            Toast.makeText(this@ManageLanguagesActivity, "✅ Nivel actualizado a $newLevel", Toast.LENGTH_SHORT).show()
                            loadUserLanguages()  // Recargar
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                            Toast.makeText(this@ManageLanguagesActivity, "⚠️ Error: $errorBody", Toast.LENGTH_LONG).show()
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

        // 🔥 Filtrar idiomas: Excluir los que ya tiene + su idioma nativo
        val availableLanguages = supportedLanguages.filter { (code, _) ->
            // No está en learning_languages
            val notInLearning = userLanguages.none { it.language == code }
            // No es su idioma nativo
            val notNative = code != nativeLanguageCode
            notInLearning && notNative
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
                android.util.Log.d("ManageLanguages", "📤 Enviando: code=$code, level=$level")
                val apiService = RetrofitService.getInstance(this@ManageLanguagesActivity)
                val request = AddLanguageRequest(code, level)
                val response = apiService.addLanguage(request)
                
                android.util.Log.d("ManageLanguages", "📥 Respuesta: ${response.code()}, body=${response.body()}")
                
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageLanguagesActivity, "✅ Idioma añadido correctamente", Toast.LENGTH_SHORT).show()
                    delay(300)  // Pequeña pausa antes de recargar
                    loadUserLanguages()  // Recargar
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    android.util.Log.e("ManageLanguages", "❌ Error body: $errorBody")
                    Toast.makeText(this@ManageLanguagesActivity, "Error añadiendo idioma: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ManageLanguages", "💀 Excepción: ${e.message}", e)
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
        setResult(RESULT_OK)  // Notificar a ProfileFragment que recargue
        finish()
        return true
    }
    
    override fun onBackPressed() {
        setResult(RESULT_OK)  // Notificar a ProfileFragment que recargue
        super.onBackPressed()
    }

    // Adapter para gestionar idiomas
    inner class ManageLanguagesAdapter(
        private val languages: List<LearningLanguage>,
        private var primaryLanguageCode: String,
        private val onToggleActive: (LearningLanguage) -> Unit,
        private val onSetPrimary: (LearningLanguage) -> Unit,
        private val onEditGoal: (LearningLanguage) -> Unit,
        private val onEditLevel: (LearningLanguage) -> Unit
    ) : RecyclerView.Adapter<ManageLanguagesAdapter.ViewHolder>() {

        // 🔥 Método para actualizar el idioma principal sin recrear adapter
        fun updatePrimaryLanguage(newPrimary: String) {
            primaryLanguageCode = newPrimary
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvLanguageName: TextView = view.findViewById(R.id.tvManageLanguageName)
            val tvStats: TextView = view.findViewById(R.id.tvManageStats)
            val pbLevelIndicator: ProgressBar = view.findViewById(R.id.pbLevelIndicator)
            val tvLevelDifficulty: TextView = view.findViewById(R.id.tvLevelDifficulty)
            val switchActive: SwitchMaterial = view.findViewById(R.id.switchActive)
            val btnPrimary: Button = view.findViewById(R.id.btnSetPrimary)
            val btnLevel: Button = view.findViewById(R.id.btnEditLevel)
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
                        // 🔥 Actualizar indicador visual de nivel
            val levelInfo = LevelIndicator.getLevelInfo(lang.level, lang.language)
            holder.pbLevelIndicator.progress = levelInfo.progress
            holder.pbLevelIndicator.progressTintList = android.content.res.ColorStateList.valueOf(levelInfo.color)
            holder.tvLevelDifficulty.text = levelInfo.description
            holder.tvLevelDifficulty.setTextColor(levelInfo.color)
                        //Contar idiomas activos para deshabilitar switch del último
            val activeCount = languages.count { it.isActive }
            
            // 🔧 Remover listener antes de cambiar el estado para evitar triggers dobles
            holder.switchActive.setOnCheckedChangeListener(null)
            holder.switchActive.isChecked = lang.isActive
            
            // 🔥 Deshabilitar switch si es el único idioma activo
            if (lang.isActive && activeCount == 1) {
                holder.switchActive.isEnabled = false
                holder.switchActive.alpha = 0.5f  // Visual feedback
            } else {
                holder.switchActive.isEnabled = true
                holder.switchActive.alpha = 1.0f
            }
            
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

            holder.btnLevel.setOnClickListener { onEditLevel(lang) }
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
