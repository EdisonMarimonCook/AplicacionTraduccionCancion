package com.example.diccionario_hiphop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class SongAdapter(
    private var songs: List<SongItem>,
    private val onSongClick: (SongItem) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvSongTitle)
        val tvArtist: TextView = view.findViewById(R.id.tvSongArtist)
        val ivCover: ImageView = view.findViewById(R.id.ivSongCover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]

        // Limpiar caracteres corruptos por encoding
        holder.tvTitle.text = cleanText(song.title)
        holder.tvArtist.text = cleanText(song.artist)

        // Carga optimizada con Glide
        Glide.with(holder.itemView.context)
            .load(song.imageUrl)
            .placeholder(R.drawable.album_cover_background)
            .error(R.drawable.album_cover_background)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions().transform(RoundedCorners(16)))
            .into(holder.ivCover)

        holder.itemView.setOnClickListener {
            onSongClick(song)
        }
    }

    /**
     * Limpia caracteres corruptos por problemas de encoding UTF-8
     */
    private fun cleanText(text: String): String {
        // Remover caracteres de control y caracteres no imprimibles
        return text.replace(Regex("[\\p{C}]"), "")
            .trim()
            .takeIf { it.isNotEmpty() } ?: text
    }

    override fun getItemCount() = songs.size

    // 🔥 OPTIMIZACIÓN: DiffUtil para animaciones suaves (en vez de notifyDataSetChanged)
    fun updateData(newSongs: List<SongItem>) {
        val diffCallback = SongDiffCallback(songs, newSongs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        songs = newSongs
        diffResult.dispatchUpdatesTo(this)
    }
    
    // 🔥 DiffUtil Callback para comparar listas eficientemente
    private class SongDiffCallback(
        private val oldList: List<SongItem>,
        private val newList: List<SongItem>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Comparar por ID único (asumiendo que SongItem tiene un identificador)
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            // Si no hay ID, comparamos por título + artista (único razonable)
            return oldItem.title == newItem.title && oldItem.artist == newItem.artist
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Comparar si el contenido completo es igual
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}