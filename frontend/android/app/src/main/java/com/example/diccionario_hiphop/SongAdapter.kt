package com.example.diccionario_hiphop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

<<<<<<< HEAD
class SongAdapter : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var songs: List<Song> = emptyList()
    private var onItemClickListener: ((Song) -> Unit)? = null

    fun submitList(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Song) -> Unit) {
        onItemClickListener = listener
=======
class SongAdapter(
    private var songs: List<SongItem>,
    private val onSongClick: (SongItem) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvSongTitle)
        val tvArtist: TextView = view.findViewById(R.id.tvSongArtist)
        val ivCover: ImageView = view.findViewById(R.id.ivSongCover)
>>>>>>> feature/lyrics-translation
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
<<<<<<< HEAD
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.songTitle)
        private val artist: TextView = itemView.findViewById(R.id.songArtist)
        private val rank: TextView = itemView.findViewById(R.id.songRank)
        private val albumCover: ImageView = itemView.findViewById(R.id.albumCover)

        fun bind(song: Song) {
            title.text = song.title
            artist.text = song.artist
            rank.text = "#${song.spotifyRank}"

            // Cargar imagen (usar Glide/Picasso en producción)
            // Glide.with(itemView).load(song.albumCover).into(albumCover)

            itemView.setOnClickListener {
                onItemClickListener?.invoke(song)
            }
        }
    }
=======
        val song = songs[position]

        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist

        // Carga segura de imagen con Glide
        if (!song.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(song.imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(holder.ivCover)
        } else {
            // Imagen por defecto si no hay URL
            holder.ivCover.setImageResource(R.drawable.ic_launcher_foreground)
        }

        holder.itemView.setOnClickListener {
            onSongClick(song)
        }
    }

    override fun getItemCount() = songs.size

    fun updateData(newSongs: List<SongItem>) {
        songs = newSongs
        notifyDataSetChanged()
    }
>>>>>>> feature/lyrics-translation
}