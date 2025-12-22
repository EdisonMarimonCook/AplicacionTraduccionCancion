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

        val response = chain.proceed(requestBuilder.build())

        // 2. 🔥 VIGILANCIA: Si falla con 401 (Token Caducado) -> Intentar Refrescar
        if (response.code == 401) {
            response.close() // Cerrar respuesta fallida para liberar recursos

            synchronized(this) {
                // Intentar obtener nuevo token
                val newToken = refreshToken()

                if (newToken != null) {
                    // ✅ ÉXITO: Reintentar la petición original con el nuevo token
                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    return chain.proceed(newRequest)
                } else {
                    // ❌ FRACASO: El refresh también caducó o es inválido -> Logout forzoso
                    tokenManager.forceLogout()
                }
            }
        }

        return response
    }

    // Lógica síncrona para refrescar el token
    private fun refreshToken(): String? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null

        try {
            // Creamos un Retrofit LIMPIO (sin interceptores) para evitar bucles infinitos.
            // ⚠️ IMPORTANTE: Asegúrate de que esta URL coincide con la de tu RetrofitService.
            // Si usas emulador: "http://10.0.2.2:8000/"
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)

            // Llamada síncrona (.execute)
            val call = api.refreshToken(RefreshTokenRequest(refreshToken))
            val response = call.execute()

            if (response.isSuccessful && response.body() != null) {
                val newAccessToken = response.body()!!.accessToken
                
                // NOTA: El backend v4.0 devuelve 'access_token' nuevo pero quizás no 'refresh_token'.
                // Mantenemos el antiguo si el nuevo viene vacío o nulo.
                val newRefreshToken = response.body()?.refreshToken ?: ""
                val finalRefreshToken = if (newRefreshToken.isNotEmpty()) newRefreshToken else refreshToken

                // 🔥 IMPORTANTE: Guardar SOLO el access token (preserva el tipo de sesión)
                // Si era temporal, sigue siendo temporal. Si era persistente, sigue persistente.
                tokenManager.saveAccessToken(newAccessToken)
                return newAccessToken
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}