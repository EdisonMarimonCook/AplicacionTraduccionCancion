package com.example.diccionario_hiphop

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val context: Context // Necesario para crear instancia temporal de Retrofit
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken()

        // 1. Construir petición con token actual (si existe)
        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        var response = chain.proceed(requestBuilder.build())

        if (response.code != 401) {
            return response
        }

        // 🚨 ALERTA 401: Aquí empieza la magia Thread-Safe
        synchronized(this) {
            // Cierre preventivo: Antes de hacer nada, cerramos la respuesta fallida 
            response.close()

            // 🕵️ DOUBLE-CHECK LOCKING
            val currentToken = tokenManager.getToken()
            val tokenFromRequest = originalRequest.header("Authorization")?.replace("Bearer ", "")

            if (currentToken != null && currentToken != tokenFromRequest) {
                // ¡Alguien ya hizo el trabajo sucio! Reintentamos con el token nuevo.
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
                return chain.proceed(newRequest)
            }

            // SI ES IGUAL: Soy el primero en entrar (o el token sigue caducado). Toca refrescar.
            val newToken = refreshToken()

            if (newToken != null) {
                // Éxito: Guardamos y reintentamos
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(newRequest)
            } else {
                // ❌ FRACASO: El refresh también caducó o es inválido -> Logout forzoso
                tokenManager.forceLogout()
            }
        }

        // Si llegamos aquí, es que no se pudo refrescar (Login caducado del todo).
        // Devolvemos una nueva respuesta 401 limpia o redirigimos a LoginActivity.
        // Como cerramos la 'response' original arriba, no podemos devolverla. 
        // Normalmente OkHttp necesita que devuelvas algo.
        return chain.proceed(originalRequest)
    }

    // Lógica síncrona para refrescar el token
    private fun refreshToken(): String? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null

        try {
            // Creamos un Retrofit LIMPIO (sin interceptores) para evitar bucles infinitos.
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)
            val call = api.refreshToken(RefreshTokenRequest(refreshToken))
            val response = call.execute()

            if (response.isSuccessful && response.body() != null) {
                val newAccessToken = response.body()!!.accessToken
                val newRefreshToken = response.body()?.refreshToken ?: ""
                val finalRefreshToken = if (newRefreshToken.isNotEmpty()) newRefreshToken else refreshToken
                // Guardar ambos tokens (access y refresh)
                tokenManager.saveTokens(newAccessToken, finalRefreshToken)
                return newAccessToken
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}