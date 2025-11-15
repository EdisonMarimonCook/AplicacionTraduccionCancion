package com.example.diccionario_hiphop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var songs: List<Song> = emptyList()
    private var onItemClickListener: ((Song) -> Unit)? = null

    fun submitList(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Song) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
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
}