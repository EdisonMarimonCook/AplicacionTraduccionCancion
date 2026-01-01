package com.example.diccionario_hiphop

import android.content.Context
import android.content.Intent
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val context: Context
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken()

        // 1. Construir petición con token actual
        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        var response = chain.proceed(requestBuilder.build())

        // Si no es 401, retornar normal
        if (response.code != 401) {
            return response
        }

        // 🚨 401 DETECTADO - Iniciar refresh
        synchronized(this) {
            response.close() // Cerrar respuesta original

            // DOUBLE-CHECK: ¿Alguien ya refrescó?
            val currentToken = tokenManager.getToken()
            val tokenFromRequest = originalRequest.header("Authorization")?.replace("Bearer ", "")

            if (currentToken != null && currentToken != tokenFromRequest) {
                // Token ya actualizado por otro hilo
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
                return chain.proceed(newRequest)
            }

            // Soy el primero → Refrescar token
            val refreshToken = tokenManager.getRefreshToken()
            
            if (refreshToken.isNullOrEmpty()) {
                android.util.Log.e("AuthInterceptor", "❌ No hay refresh token, forzando logout")
                tokenManager.forceLogout()
                throw IOException("Sesión expirada")
            }

            try {
                // 🔥 FIX: Usar BASE_URL dinámica, NO localhost hardcoded
                val baseUrl = RetrofitService.BASE_URL
                
                android.util.Log.d("AuthInterceptor", "🔄 Refrescando token en: $baseUrl")

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl) // ✅ Usa la URL correcta
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ApiService::class.java)
                val call = api.refreshToken(RefreshTokenRequest(refreshToken))
                val refreshResponse = call.execute()

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val newAccessToken = refreshResponse.body()!!.accessToken
                    val newRefreshToken = refreshResponse.body()?.refreshToken ?: refreshToken
                    
                    // 🔥 FIX: Guardar AMBOS tokens
                    tokenManager.saveTokens(newAccessToken, newRefreshToken)
                    
                    android.util.Log.d("AuthInterceptor", "✅ Token refrescado exitosamente")

                    // Reintentar request original con nuevo token
                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                    
                    return chain.proceed(newRequest)
                } else {
                    // Refresh falló → Logout
                    android.util.Log.e("AuthInterceptor", "❌ Refresh falló: ${refreshResponse.code()}")
                    tokenManager.forceLogout()
                    throw IOException("Refresh token inválido")
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthInterceptor", "💥 Error crítico en refresh", e)
                tokenManager.forceLogout()
                throw IOException("Error de autenticación: ${e.message}")
            }
        }
    }
}