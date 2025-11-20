package com.example.diccionario_hiphop

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class DictionaryActivity : AppCompatActivity() {

    private lateinit var rvDictionary: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var adapter: DictionaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionary) // Carga el diseño general

        initViews()
        setupRecyclerView()

        // Al abrir la pantalla, pedimos los datos
        loadDictionary()

        btnBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        rvDictionary = findViewById(R.id.rvDictionary)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupRecyclerView() {
        // Inicializamos el adaptador (el camarero) vacío
        adapter = DictionaryAdapter(mutableListOf()) { wordToDelete ->
            // Qué hacer cuando se pulsa borrar en un item
            confirmDelete(wordToDelete)
        }
        rvDictionary.layoutManager = LinearLayoutManager(this)
        rvDictionary.adapter = adapter // Conectamos el camarero con la lista
    }

    private fun loadDictionary() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                // LLAMADA AL SERVIDOR
                val apiService = RetrofitService.getInstance(this@DictionaryActivity)
                val response = apiService.getDictionary()

                if (response.isSuccessful && response.body() != null) {
                    val dictionary = response.body()!!

                    if (dictionary.words.isEmpty()) {
                        // Si no hay palabras, mostramos mensaje de vacío
                        tvEmptyState.visibility = View.VISIBLE
                        rvDictionary.visibility = View.GONE
                    } else {
                        // Si hay palabras, se las damos al adaptador
                        tvEmptyState.visibility = View.GONE
                        rvDictionary.visibility = View.VISIBLE
                        adapter.updateData(dictionary.words)
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
        AlertDialog.Builder(this)
            .setTitle("¿Borrar palabra?")
            .setMessage("¿Quieres eliminar '${word.word}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteWord(word)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteWord(word: UserWord) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@DictionaryActivity)
                val response = apiService.deleteWord(word.wordId)

                if (response.isSuccessful) {
                    Toast.makeText(this@DictionaryActivity, "Eliminada", Toast.LENGTH_SHORT).show()
                    adapter.removeWord(word) // La quitamos de la lista visualmente

                    if (adapter.itemCount == 0) tvEmptyState.visibility = View.VISIBLE
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