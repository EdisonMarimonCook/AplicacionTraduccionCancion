package com.example.diccionario_hiphop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist

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

    override fun getItemCount() = songs.size

    fun updateData(newSongs: List<SongItem>) {
        songs = newSongs
        notifyDataSetChanged()
    }
}