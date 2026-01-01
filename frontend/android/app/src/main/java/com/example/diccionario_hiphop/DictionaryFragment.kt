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
import kotlinx.coroutines.launch

// ✅ CORRECCIÓN: Apuntamos a 'fragment_dictionary'
class DictionaryFragment : Fragment(R.layout.fragment_dictionary) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: DictionaryAdapter
    private lateinit var repository: DictionaryRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = DictionaryRepository(requireContext())

        initViews(view)
        setupRecyclerView()
        loadDictionary()
    }

    private fun initViews(view: View) {
        // ⚠️ IMPORTANTE: Asegúrate de que tu 'fragment_dictionary.xml' tenga estos IDs:
        recyclerView = view.findViewById(R.id.rvDictionary)
        progressBar = view.findViewById(R.id.progressBar)
        // Si tu XML no tiene un TextView para "Vacío", comenta la línea de abajo:
        tvEmpty = view.findViewById(R.id.tvEmpty) 
    }

    private fun setupRecyclerView() {
        adapter = DictionaryAdapter(mutableListOf()) { word ->
            // Acción al borrar (opcional por ahora)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadDictionary() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val response = repository.getDictionary()

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
}