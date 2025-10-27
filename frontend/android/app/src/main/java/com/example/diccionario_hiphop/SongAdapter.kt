package com.example.diccionario_hiphop  // ⬅️ USA TU PACKAGE REAL

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

// Modelo de datos para canciones - ESTO VA ARRIBA DEL ADAPTER
data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val difficulty: String,
    val lyrics: String
)

class SongAdapter(private val onItemClick: (Song) -> Unit) :
    ListAdapter<Song, SongAdapter.SongViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        // Usamos un layout simple de Android (no necesitamos crear XML)
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song)
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(song: Song) {
            text1.text = "${song.title} - ${song.artist}"
            text2.text = "Nivel: ${song.difficulty.capitalize()}"

            itemView.setOnClickListener {
                onItemClick(song)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}