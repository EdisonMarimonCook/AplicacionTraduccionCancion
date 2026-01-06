package com.example.diccionario_hiphop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {
    
    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.ivOnboardingImage)
        val tvTitle: TextView = view.findViewById(R.id.tvOnboardingTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvOnboardingDescription)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return OnboardingViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        holder.tvEmoji.text = page.emoji
        holder.tvTitle.text = page.title
        holder.tvDescription.text = page.description
    }
    
    override fun getItemCount() = pages.size
}
