package com.example.diccionario_hiphop

import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class ConnectionTester {
    private val TAG = "ConnectionTester"

    suspend fun testBackendConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔌 Probando conexión con backend...")
                val response: Response<HealthResponse> = RetrofitService.apiService.healthCheck()

                // ✅ Ahora isSuccessful debería funcionar
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