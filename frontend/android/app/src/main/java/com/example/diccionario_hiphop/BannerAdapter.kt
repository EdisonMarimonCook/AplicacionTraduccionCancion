package com.example.diccionario_hiphop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class BannerAdapter(
    private val songs: List<SongItem>,
    private val onClick: (SongItem) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivBannerImage)
        val title: TextView = view.findViewById(R.id.tvBannerTitle)
        val artist: TextView = view.findViewById(R.id.tvBannerArtist)

        fun bind(song: SongItem) {
            title.text = song.title
            artist.text = song.artist

            // Cargar imagen con Glide
            Glide.with(itemView.context)
                .load(song.imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.album_cover_background)
                .error(R.drawable.album_cover_background)
                .into(image)

            // Click en la tarjeta
            itemView.setOnClickListener { onClick(song) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner_song, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun getItemCount(): Int = songs.size
}
