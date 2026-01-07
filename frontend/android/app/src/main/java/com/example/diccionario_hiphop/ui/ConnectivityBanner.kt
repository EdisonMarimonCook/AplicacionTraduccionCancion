package com.example.diccionario_hiphop.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.diccionario_hiphop.R

/**
 * Banner que muestra el estado de conectividad
 * Se oculta cuando hay conexión, se muestra cuando está offline
 */
class ConnectivityBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val tvMessage: TextView
    private var currentState: BannerState = BannerState.ONLINE
    
    private enum class BannerState {
        ONLINE, OFFLINE, SYNCING
    }
    
    init {
        LayoutInflater.from(context).inflate(R.layout.view_connectivity_banner, this, true)
        tvMessage = findViewById(R.id.tvConnectivityMessage)
        
        orientation = HORIZONTAL
        visibility = GONE
    }
    
    fun setOffline() {
        if (currentState == BannerState.OFFLINE) return  // Ya está offline, no cambiar
        
        android.util.Log.d("ConnectivityBanner", "📴 Mostrando banner offline")
        currentState = BannerState.OFFLINE
        visibility = VISIBLE
        tvMessage.text = "📴 Modo Offline - Viendo datos guardados"
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
    }
    
    fun setOnline() {
        if (currentState == BannerState.ONLINE) return  // Ya está online, no cambiar
        
        android.util.Log.d("ConnectivityBanner", "✅ Ocultando banner (online)")
        currentState = BannerState.ONLINE
        visibility = GONE
    }
    
    fun setSyncing() {
        if (currentState == BannerState.SYNCING) return  // Ya está sincronizando, no cambiar
        
        currentState = BannerState.SYNCING
        visibility = VISIBLE
        tvMessage.text = "🔄 Sincronizando datos..."
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
    }
}
