package com.example.diccionario_hiphop

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken()

        // Si no hay token, enviamos la petición tal cual (útil para Login/Register)
        if (token == null) {
            return chain.proceed(originalRequest)
        }

        // Si hay token, creamos una nueva petición con el Header Authorization
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}