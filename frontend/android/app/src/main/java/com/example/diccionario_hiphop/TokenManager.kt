package com.example.diccionario_hiphop

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class TokenManager(private val context: Context) {

    // SharedPreferences PERSISTENTE (sobrevive al cierre de la app)
    private var prefs: SharedPreferences = context.getSharedPreferences("prefs_diccionario_hiphop", Context.MODE_PRIVATE)
    
    // 🔥 COMPANION OBJECT: Variables estáticas compartidas entre TODAS las instancias
    companion object {
        // Tokens temporales en memoria (compartidos globalmente)
        @Volatile private var tempAccessToken: String? = null
        @Volatile private var tempRefreshToken: String? = null
        @Volatile private var isUsingTempSession: Boolean = false
    }

    /**
     * Guarda tokens de forma PERSISTENTE (con "Recuérdame" activo).
     */
    @Synchronized
    fun saveTokens(accessToken: String, refreshToken: String) {
        val editor = prefs.edit()
        editor.putString("USER_TOKEN", accessToken)
        if (refreshToken.isNotEmpty()) {
            editor.putString("REFRESH_TOKEN", refreshToken)
        }
        editor.apply()
        // Limpiar sesión temporal si existía
        tempAccessToken = null
        tempRefreshToken = null
        isUsingTempSession = false
    }
    
    /**
     * 🆕 Guarda tokens de forma TEMPORAL (sin "Recuérdame").
     * Los tokens se pierden al cerrar la app.
     */
    @Synchronized
    fun saveTemporaryTokens(accessToken: String, refreshToken: String) {
        tempAccessToken = accessToken
        tempRefreshToken = refreshToken
        isUsingTempSession = true
        // Asegurarse de que NO haya tokens persistentes
        clearPersistedTokens()
    }

    /**
     * Guarda solo el Access Token (útil tras un refresco).
     */
    @Synchronized
    fun saveAccessToken(token: String) {
        if (isUsingTempSession) {
            tempAccessToken = token
        } else {
            prefs.edit().putString("USER_TOKEN", token).apply()
        }
    }

    /**
     * Recupera el token de acceso (persistente o temporal).
     */
    @Synchronized
    fun getToken(): String? {
        return if (isUsingTempSession) {
            tempAccessToken
        } else {
            prefs.getString("USER_TOKEN", null)
        }
    }

    /**
     * Recupera el token de refresco (persistente o temporal).
     */
    @Synchronized
    fun getRefreshToken(): String? {
        return if (isUsingTempSession) {
            tempRefreshToken
        } else {
            prefs.getString("REFRESH_TOKEN", null)
        }
    }
    
    /**
     * Verifica si hay una sesión activa (persistente o temporal).
     */
    @Synchronized
    fun hasActiveSession(): Boolean {
        return getToken() != null
    }

    /**
     * Borra SOLO los tokens persistentes de SharedPreferences.
     */
    @Synchronized
    private fun clearPersistedTokens() {
        val editor = prefs.edit()
        editor.remove("USER_TOKEN")
        editor.remove("REFRESH_TOKEN")
        editor.apply()
    }
    
    /**
     * Borra SOLO los tokens temporales de memoria.
     */
    @Synchronized
    private fun clearTemporaryTokens() {
        tempAccessToken = null
        tempRefreshToken = null
        isUsingTempSession = false
    }

    /**
     * Borra TODA la sesión (persistente y temporal).
     */
    @Synchronized
    fun clearSession() {
        clearPersistedTokens()
        clearTemporaryTokens()
        
        // ✅ Borrar también SharedPreferences de onboarding (por si acaso)
        try {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("onboarding_completed")
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("TokenManager", "Error limpiando app_prefs", e)
        }
    }
/**
 * 🔥 FORCE LOGOUT: Borra TODO y redirige al Login.
 */
@Synchronized
fun forceLogout() {
    android.util.Log.d("TokenManager", "🔄 Forzando logout...")
    
    // 1. Borrar tokens
    clearSession()  // ✅ Usa clearSession() que ya existe en tu código
    
    // 2. Borrar datos temporales si existen
    try {
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    } catch (e: Exception) {
        android.util.Log.e("TokenManager", "Error limpiando user_prefs", e)
    }
    
    // 3. Redirigir a Login (NO a MainActivity)
    val intent = Intent(context, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
    }
}