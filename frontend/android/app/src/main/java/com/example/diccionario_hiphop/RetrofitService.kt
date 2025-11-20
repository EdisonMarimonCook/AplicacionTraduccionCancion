package com.example.diccionario_hiphop

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitService {

    companion object {
        // ✅ URL ESPECIAL PARA EMULADOR ANDROID
        // 10.0.2.2 es el alias que usa Android para acceder al "localhost" de tu PC.
        // Si usas un móvil físico, tendrás que cambiar esto por tu IP local (ej: 192.168.1.X)
        private const val BASE_URL = "http://10.0.2.2:8000/"

        @Volatile
        private var INSTANCE: ApiService? = null

        fun getInstance(context: Context): ApiService {
            return INSTANCE ?: synchronized(this) {
                val instance = buildRetrofit(context)
                INSTANCE = instance
                instance
            }
        }

        private fun buildRetrofit(context: Context): ApiService {
            // 1. Inicializamos el TokenManager para gestionar la sesión
            val tokenManager = TokenManager(context)

            // 2. Inicializamos el AuthInterceptor que inyectará el token automáticamente
            val authInterceptor = AuthInterceptor(tokenManager)

            // 3. Logging (para ver qué pasa en el Logcat, vital para depurar)
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // 4. Configuración del Cliente HTTP
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) // Logs primero
                .addInterceptor(authInterceptor)    // Auth después
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            // 5. Construir Retrofit
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}