package com.example.diccionario_hiphop.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitor de conectividad de red
 * Proporciona un Flow para observar cambios en la conectividad
 */
class NetworkMonitor(context: Context) {
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableStateFlow(checkInitialConnection())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "🌐 Red disponible")
            // Verificar el estado general de conectividad
            checkCurrentConnection()
        }
        
        override fun onLost(network: Network) {
            Log.d(TAG, "📴 Red perdida")
            // Verificar el estado general de conectividad
            checkCurrentConnection()
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            // Verificar el estado general de conectividad
            checkCurrentConnection()
        }
    }
    
    private fun checkCurrentConnection() {
        val isConnected = checkInitialConnection()
        updateConnectionState(isConnected)
    }
    
    private fun updateConnectionState(newState: Boolean) {
        if (_isConnected.value != newState) {
            _isConnected.value = newState
            Log.d(TAG, "🔄 Estado de conexión actualizado: ${if (newState) "ONLINE" else "OFFLINE"}")
        }
    }
    
    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    private fun checkInitialConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        // Solo verificar que tenga capacidad de internet, sin esperar validación
        // La validación puede tardar y causar que onLost no actualice el estado inmediatamente
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar network callback: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "NetworkMonitor"
        
        @Volatile
        private var instance: NetworkMonitor? = null
        
        fun getInstance(context: Context): NetworkMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkMonitor(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
