package com.example.diccionario_hiphop

<<<<<<< HEAD
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class ConnectionTester {
=======
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConnectionTester(private val context: Context) { // ✅ Recibimos Context
>>>>>>> feature/lyrics-translation
    private val TAG = "ConnectionTester"

    suspend fun testBackendConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔌 Probando conexión con backend...")
<<<<<<< HEAD
                val response: Response<HealthResponse> = RetrofitService.apiService.healthCheck()

                // ✅ Ahora isSuccessful debería funcionar
=======
                
                // ✅ Usamos getInstance(context) correctamente
                val api = RetrofitService.getInstance(context)
                val response = api.healthCheck()

>>>>>>> feature/lyrics-translation
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Backend conectado: ${response.body()?.status}")
                    true
                } else {
                    Log.e(TAG, "❌ Backend error: ${response.code()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error de conexión: ${e.message}")
                false
            }
        }
    }
}