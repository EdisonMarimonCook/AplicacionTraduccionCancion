package com.example.diccionario_hiphop

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
// Imports necesarios para los efectos
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback

class SongAdapter(
    private var songs: List<SongItem>,
    private val onSongClick: (SongItem) -> Unit,
    private val selectedLanguage: String = "en",
    private val grammySongs: List<SongItem> = emptyList()
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Componentes básicos
        val tvTitle: TextView = view.findViewById(R.id.tvSongTitle)
        val tvArtist: TextView = view.findViewById(R.id.tvSongArtist)
        val ivCover: ImageView = view.findViewById(R.id.ivSongCover)
        val tvGrammyBadge: TextView = view.findViewById(R.id.tvGrammyBadge)
        val cardSong: CardView = view.findViewById(R.id.cardSong)
        
        // Componentes de Efectos Premium (Nuevos IDs)
        val viewFlash: View = view.findViewById(R.id.viewFlashInternal)
        val lottieAura: LottieAnimationView = view.findViewById(R.id.lottieAuraOutside)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        // Importante: attachToRoot debe ser false
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        val context = holder.itemView.context

        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist

        Glide.with(context)
            .load(song.imageUrl)
            .transform(RoundedCorners(16))
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(holder.ivCover)

        holder.itemView.setOnClickListener { onSongClick(song) }

        // --- LÓGICA PREMIUM / GRAMMY ---
        val isGrammy = grammySongs.any { 
            it.title.equals(song.title, ignoreCase = true) && 
            it.artist.equals(song.artist, ignoreCase = true) 
        }

        if (isGrammy) {
            // 🌟 MODO PREMIUM 🌟
            holder.cardSong.setCardBackgroundColor(Color.parseColor("#FFD700")) // Dorado
            holder.tvTitle.setTextColor(Color.BLACK)
            holder.tvArtist.setTextColor(Color.DKGRAY)
            holder.tvGrammyBadge.visibility = View.VISIBLE
            
            // Activar efectos
            playPremiumEffect(holder)

        } else {
            // 🌑 MODO NORMAL 🌑
            val defaultCardColor = try {
                 androidx.core.content.ContextCompat.getColor(context, R.color.bg_card)
            } catch (e: Exception) { Color.WHITE }
            holder.cardSong.setCardBackgroundColor(defaultCardColor)
            
            val colorPrimary = try { androidx.core.content.ContextCompat.getColor(context, R.color.text_primary) } catch (e: Exception) { Color.BLACK }
            val colorSecondary = try { androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary) } catch (e: Exception) { Color.GRAY }
            holder.tvTitle.setTextColor(colorPrimary)
            holder.tvArtist.setTextColor(colorSecondary)

            holder.tvGrammyBadge.visibility = View.GONE
            
            // Apagar efectos
            holder.lottieAura.visibility = View.GONE
            holder.lottieAura.cancelAnimation()
            holder.viewFlash.visibility = View.GONE
            holder.viewFlash.clearAnimation()
        }
    }

  private fun playPremiumEffect(holder: SongViewHolder) {
    holder.viewFlash.visibility = View.VISIBLE
    holder.viewFlash.scaleY = 3f 
    holder.viewFlash.scaleX = 1.2f

    holder.viewFlash.post {
        if (holder.viewFlash.visibility == View.VISIBLE) {
            val width = holder.cardSong.width.toFloat()
            holder.viewFlash.clearAnimation()

            val animation = TranslateAnimation(
                -width - 400f,
                width + 400f,
                0f, 0f
            )
            animation.duration = 1400
            animation.repeatCount = Animation.INFINITE
            animation.repeatMode = Animation.RESTART
            holder.viewFlash.startAnimation(animation)
        }
    }
}

    override fun getItemCount() = songs.size

    fun updateData(newSongs: List<SongItem>) {
        songs = newSongs
        notifyDataSetChanged()
    }
}