package com.example.diccionario_hiphop

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class TokenManager(private val context: Context) {

    // Usamos SharedPreferences para guardar datos simples de forma segura
    private var prefs: SharedPreferences = context.getSharedPreferences("prefs_diccionario_hiphop", Context.MODE_PRIVATE)

    /**
     * Guarda AMBOS tokens al hacer Login o Registro exitoso.
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        val editor = prefs.edit()
        editor.putString("USER_TOKEN", accessToken)
        // Si el backend devuelve un refresh token, lo guardamos.
        // Si viene vacío (algunas APIs no lo devuelven siempre), mantenemos el anterior o guardamos vacío según lógica.
        if (refreshToken.isNotEmpty()) {
            editor.putString("REFRESH_TOKEN", refreshToken)
        }
        editor.apply()
    }

    /**
     * Guarda solo el Access Token (útil tras un refresco exitoso donde el refresh token no cambia).
     */
    fun saveAccessToken(token: String) {
        prefs.edit().putString("USER_TOKEN", token).apply()
    }

    /**
     * Recupera el token de acceso actual.
     */
    fun getToken(): String? {
        return prefs.getString("USER_TOKEN", null)
    }

    /**
     * Recupera el token de refresco para solicitar una nueva sesión.
     */
    fun getRefreshToken(): String? {
        return prefs.getString("REFRESH_TOKEN", null)
    }

    /**
     * Borra los tokens de sesión (pero no datos de usuario como nivel).
     */
    fun clearSession() {
        val editor = prefs.edit()
        editor.remove("USER_TOKEN")
        editor.remove("REFRESH_TOKEN")
        editor.apply()
    }

    /**
     * 🔥 FORCE LOGOUT: Borra TODO y redirige al Login.
     * Se usa cuando el token expira y el refresco falla (401 final).
     */
    fun forceLogout() {
        // 1. Borrar tokens de seguridad
        clearSession()

        // 2. Borrar datos de perfil (nombre, nivel, etc.) guardados en otro archivo
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().clear().apply()

        // 3. Redirigir a la pantalla de inicio (MainActivity / Login)
        // FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK borran la pila de actividades
        // para que el usuario no pueda volver atrás con el botón "Back".
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}