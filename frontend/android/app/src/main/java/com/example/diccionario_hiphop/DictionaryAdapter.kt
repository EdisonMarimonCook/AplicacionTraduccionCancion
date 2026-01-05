package com.example.diccionario_hiphop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DictionaryAdapter(
    private var words: MutableList<UserWord>,
    private val onDeleteClick: (UserWord) -> Unit,
    private val onAudioClick: (UserWord) -> Unit  // 🎵 Nuevo callback para audio
) : RecyclerView.Adapter<DictionaryAdapter.WordViewHolder>() {

    class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWord: TextView = view.findViewById(R.id.tvWord)
        val tvTranslation: TextView = view.findViewById(R.id.tvTranslation)
        val tvContext: TextView = view.findViewById(R.id.tvContext)
        val tvSource: TextView = view.findViewById(R.id.tvSource) // Reutilizamos este ID para mostrar el TIPO
        val btnAudio: ImageButton = view.findViewById(R.id.btnAudio)  // 🎵 Nuevo botón
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val item = words[position]

        holder.tvWord.text = item.word.replaceFirstChar { it.uppercase() }
        holder.tvTranslation.text = item.translation
        holder.tvContext.text = "\"${item.context ?: ""}\""
        
        // ✅ CORRECCIÓN: Mostrar TIPO en lugar de canción
        if (item.type == "expression") {
            holder.tvSource.text = "📝 Expresión"
            holder.tvSource.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.teal_700))
        } else {
            holder.tvSource.text = "📖 Palabra"
            holder.tvSource.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.purple_700))
        }

        // 🎵 Configurar botón de audio
        holder.btnAudio.setOnClickListener {
            onAudioClick(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = words.size

    fun updateData(newWords: List<UserWord>) {
        words.clear()
        words.addAll(newWords)
        notifyDataSetChanged()
    }

    fun removeWord(word: UserWord) {
        val index = words.indexOf(word)
        if (index != -1) {
            words.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}