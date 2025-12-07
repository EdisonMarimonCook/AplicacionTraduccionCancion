package com.example.diccionario_hiphop

<<<<<<< HEAD
=======
import android.content.Context
>>>>>>> feature/lyrics-translation
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

<<<<<<< HEAD
object RetrofitService {
    // ✅ URL de tu backend FastAPI - PARA EMULADOR
    private const val BASE_URL = "http://10.0.2.2:8000"

    // Si usas dispositivo físico, cambia por tu IP local:
    // private const val BASE_URL = "http://192.168.1.XXX:8000"

    // Interceptor para ver logs de las peticiones
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente HTTP configurado
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS) // Timeout de conexión
        .readTimeout(30, TimeUnit.SECONDS)    // Timeout de lectura
        .build()

    // Instancia de Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Servicio de API (se crea una sola vez - lazy initialization)
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
=======
class RetrofitService {
    companion object {
        // ⚠️ CAMBIAR SI USAS MÓVIL FÍSICO (PON TU IP LOCAL)
        // Para emulador usa: "http://10.0.2.2:8000/"
        private const val BASE_URL = "http://10.0.2.2:8000/"

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
>>>>>>> feature/lyrics-translation
    }
}