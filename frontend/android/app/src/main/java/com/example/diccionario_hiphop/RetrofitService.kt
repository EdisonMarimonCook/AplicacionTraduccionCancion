package com.example.diccionario_hiphop

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitService {
    companion object {
        // 🔥 CAMBIO: Quitar 'private' para que se pueda usar en ProfileActivity
        const val BASE_URL = "http://192.168.137.1:8000/" // Reemplaza X con tu IP real

        @Volatile private var INSTANCE: ApiService? = null

        fun getInstance(context: Context): ApiService {
            return INSTANCE ?: synchronized(this) {
                // 🔥 CORRECCIÓN: Pasamos 'context' a la función buildRetrofit
                val instance = buildRetrofit(context)
                INSTANCE = instance
                instance
            }
        }

        // Esta función recibe el contexto para inicializar TokenManager y AuthInterceptor
        private fun buildRetrofit(context: Context): ApiService {
            val tokenManager = TokenManager(context)

            // AuthInterceptor necesita contexto para poder forzar el logout si el token falla
            val authInterceptor = AuthInterceptor(tokenManager, context)

            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}