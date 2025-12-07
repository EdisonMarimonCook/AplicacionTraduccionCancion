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
        setContentView(R.layout.activity_dictionary)

        initViews()
        setupRecyclerView()
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
        adapter = DictionaryAdapter(mutableListOf()) { wordToDelete ->
            confirmDelete(wordToDelete)
        }
        rvDictionary.layoutManager = LinearLayoutManager(this)
        rvDictionary.adapter = adapter
    }

    private fun loadDictionary() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@DictionaryActivity)
                // ✅ CAMBIO: El backend devuelve List<UserWord> directamente
                val response = apiService.getDictionary(type = null) // Pedimos todo (palabras y expresiones)

                if (response.isSuccessful && response.body() != null) {
                    val words = response.body()!!

                    if (words.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        rvDictionary.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
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
        AlertDialog.Builder(this)
            .setTitle("¿Borrar?")
            .setMessage("¿Eliminar '${word.word}'?")
            .setPositiveButton("Sí") { _, _ -> deleteWord(word) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteWord(word: UserWord) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitService.getInstance(this@DictionaryActivity)
                // ✅ CAMBIO: Usamos 'word.id' (nuevo modelo) en lugar de 'word.wordId'
                val response = apiService.deleteWord(word.id)

                if (response.isSuccessful) {
                    Toast.makeText(this@DictionaryActivity, "Eliminada", Toast.LENGTH_SHORT).show()
                    adapter.removeWord(word)
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