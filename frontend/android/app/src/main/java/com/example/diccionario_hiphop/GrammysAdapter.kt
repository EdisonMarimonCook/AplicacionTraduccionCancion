package com.example.diccionario_hiphop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class GrammysAdapter(
    private var songs: List<SongItem>, // Usamos SongItem para conectar con tu API
    private val onSongClick: (SongItem) -> Unit
) : RecyclerView.Adapter<GrammysAdapter.GrammyViewHolder>() {

    // Método para actualizar la lista sin recrear el adaptador
    fun updateList(newSongs: List<SongItem>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrammyViewHolder {
        // 🔥 Aquí inflamos el diseño ESPECIAL de trofeo
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grammy_trophy, parent, false)
        return GrammyViewHolder(view)
    }

    override fun onBindViewHolder(holder: GrammyViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, onSongClick)
    }

    override fun getItemCount(): Int = songs.size

    // Clase interna para manejar las vistas
    class GrammyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSongTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvArtistName)
        private val ivCover: ImageView = itemView.findViewById(R.id.ivAlbumCover)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvCategoryBadge)

        fun bind(song: SongItem, onClick: (SongItem) -> Unit) {
            tvTitle.text = song.title
            tvArtist.text = song.artist

            // Etiqueta dorada (Como la API no trae categoría, ponemos una elegante)
            tvBadge.text = "GRAMMY SELECTION"

            // Cargar imagen con Glide
            Glide.with(itemView.context)
                .load(song.imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .centerCrop()
                .into(ivCover)

            // Click listener
            itemView.setOnClickListener { onClick(song) }
        }
    }
}