package com.example.diccionario_hiphop

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView 
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class DictionaryActivity : AppCompatActivity() {

    private lateinit var rvDictionary: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var searchView: SearchView
    private lateinit var adapter: DictionaryAdapter

    private var fullList: List<UserWord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionary)

        initViews()
        setupRecyclerView()
        loadDictionary()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus() 
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
    }

    private fun initViews() {
        rvDictionary = findViewById(R.id.rvDictionary)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        searchView = findViewById(R.id.searchView)
    }

    private fun setupRecyclerView() {
        adapter = DictionaryAdapter(mutableListOf()) { wordToDelete ->
            confirmDelete(wordToDelete)
        }
        rvDictionary.layoutManager = LinearLayoutManager(this)
        rvDictionary.adapter = adapter
    }

    // --- AQUÍ ESTABA EL ERROR, CORREGIDO ABAJO ---
    private fun filterList(query: String?) {
        if (query.isNullOrEmpty()) {
            adapter.updateData(fullList)
            if (fullList.isEmpty()) tvEmpty.visibility = View.VISIBLE else tvEmpty.visibility = View.GONE
        } else {
            val lowerCaseQuery = query.lowercase()
            
            val filteredList = fullList.filter { wordItem ->
                // CORRECCIÓN: Usamos '?' y '?:' para manejar nulos de forma segura
                // Si word o translation son null, usamos "" (texto vacío)
                val wordText = wordItem.word?.lowercase() ?: ""
                val translationText = wordItem.translation?.lowercase() ?: ""

                wordText.contains(lowerCaseQuery) || translationText.contains(lowerCaseQuery)
            }
            
            adapter.updateData(filteredList)
            
            if (filteredList.isEmpty()) {
                tvEmpty.text = "No hay resultados"
                tvEmpty.visibility = View.VISIBLE
            } else {
                tvEmpty.visibility = View.GONE
            }
        }
    }
    // ---------------------------------------------

    private fun loadDictionary() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@DictionaryActivity)
                val response = apiService.getDictionary(type = null)

                if (response.isSuccessful && response.body() != null) {
                    val words = response.body()!!
                    fullList = words 

                    if (words.isEmpty()) {
                        tvEmpty.text = "Aún no has guardado palabras"
                        tvEmpty.visibility = View.VISIBLE
                        rvDictionary.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvDictionary.visibility = View.VISIBLE
                        adapter.updateData(words)
                    }
                } else {
                    Toast.makeText(this@DictionaryActivity, "Error al cargar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DictionaryActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun confirmDelete(word: UserWord) {
        // Protección extra por si word.word es nulo
        val wordText = word.word ?: "esta palabra"
        
        AlertDialog.Builder(this)
            .setTitle("¿Borrar?")
            .setMessage("¿Eliminar '$wordText'?")
            .setPositiveButton("Sí") { _, _ -> deleteWord(word) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteWord(word: UserWord) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@DictionaryActivity)
                val response = apiService.deleteWord(word.id)

                if (response.isSuccessful) {
                    Toast.makeText(this@DictionaryActivity, "Eliminada", Toast.LENGTH_SHORT).show()
                    adapter.removeWord(word)
                    
                    fullList = fullList.filter { it.id != word.id }

                    if (adapter.itemCount == 0 && searchView.query.isNullOrEmpty()) {
                        tvEmpty.text = "Diccionario vacío"
                        tvEmpty.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@DictionaryActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DictionaryActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}