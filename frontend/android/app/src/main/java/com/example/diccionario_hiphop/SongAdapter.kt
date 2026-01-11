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

         val ivSparkle1: ImageView = view.findViewById(R.id.ivSparkle1)
    val ivSparkle2: ImageView = view.findViewById(R.id.ivSparkle2)
    val ivSparkle3: ImageView = view.findViewById(R.id.ivSparkle3)
    val ivSparkle4: ImageView = view.findViewById(R.id.ivSparkle4)
    val ivSparkle5: ImageView = view.findViewById(R.id.ivSparkle5)  
    val ivSparkle6: ImageView = view.findViewById(R.id.ivSparkle6)
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
            holder.viewFlash.visibility = View.GONE
            holder.viewFlash.clearAnimation()

           // ✨ Apagar TODOS los destellos
    listOf(
        holder.ivSparkle1, holder.ivSparkle2, holder.ivSparkle3,
        holder.ivSparkle4, holder.ivSparkle5, holder.ivSparkle6
    ).forEach {
        it.visibility = View.GONE
        it.clearAnimation()
        it.animate().cancel()
    }
        }
    }

 private fun playPremiumEffect(holder: SongViewHolder) {
    // 1️⃣ FLASH
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
    
    // 2️⃣ ✨ DESTELLOS ALEATORIOS
    val sparkles = listOf(
        holder.ivSparkle1,
        holder.ivSparkle2,
        holder.ivSparkle3,
        holder.ivSparkle4,
        holder.ivSparkle5,
        holder.ivSparkle6
    )
    
    sparkles.forEach { sparkle ->
        sparkle.visibility = View.VISIBLE
        animateSparkleLoop(sparkle, holder.cardSong)
    }
}

private fun animateSparkleLoop(sparkle: ImageView, card: CardView) {
    // Posición aleatoria dentro de la tarjeta
    card.post {
        val cardWidth = card.width
        val cardHeight = card.height
        
        if (cardWidth <= 0 || cardHeight <= 0) return@post
        
        val randomX = (Math.random() * (cardWidth - sparkle.width)).toFloat()
        val randomY = (Math.random() * (cardHeight - sparkle.height)).toFloat()
        
        sparkle.x = randomX
        sparkle.y = randomY
        
        // Animación: aparecer -> brillar -> desaparecer -> repetir
        sparkle.alpha = 0f
        sparkle.scaleX = 0.5f
        sparkle.scaleY = 0.5f
        
        sparkle.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(400)
            .withEndAction {
                // Mantener visible un momento
                sparkle.postDelayed({
                    // Desaparecer
                    sparkle.animate()
                        .alpha(0f)
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(300)
                        .withEndAction {
                            // 🔥 LOOP: Volver a empezar con nueva posición aleatoria
                            if (sparkle.visibility == View.VISIBLE) {
                                sparkle.postDelayed({
                                    animateSparkleLoop(sparkle, card)
                                }, (Math.random() * 500).toLong())  // Delay aleatorio
                            }
                        }
                        .start()
                }, 200)
            }
            .start()
    }
}

    override fun getItemCount() = songs.size

    fun updateData(newSongs: List<SongItem>) {
        songs = newSongs
        notifyDataSetChanged()
    }
}