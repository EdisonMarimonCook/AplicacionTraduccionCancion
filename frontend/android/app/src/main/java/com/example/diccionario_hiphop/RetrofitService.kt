package com.example.diccionario_hiphop

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitService {
    companion object {
        private const val TAG = "RetrofitService"
        
        @Volatile private var INSTANCE: ApiService? = null
        @Volatile private var appContext: Context? = null
        @Volatile private var lastBaseUrl: String? = null

        /**
         * URL base actual (se recalcula dinámicamente según el contexto)
         */
        val BASE_URL: String
            get() = lastBaseUrl ?: "http://192.168.137.1:8000/"

        /**
         * 🧠 DETECCIÓN INTELIGENTE DE ENTORNO
         */
        private fun getBaseUrl(): String {
            // 1️⃣ Verificar si estamos en PRODUCCIÓN (BuildConfig)
            //if (!BuildConfig.DEBUG) {
                Log.i(TAG, "🌐 Modo PRODUCCIÓN → ${BuildConfig.PRODUCTION_URL}")
                return BuildConfig.PRODUCTION_URL
            //}

            // 2️⃣ Modo DESARROLLO: Detectar emulador vs dispositivo físico
            return if (isEmulator()) {
                // Emulador Android Studio
                val url = "http://10.0.2.2:${BuildConfig.LOCALHOST_PORT}/"
                Log.i(TAG, "📱 EMULADOR detectado → $url")
                url
            } else {
                // 🔥 Dispositivo físico: Detectar IP del GATEWAY (router/hotspot) automáticamente
                val gatewayIp = appContext?.let { getGatewayIp(it) }
                val url = if (gatewayIp != null) {
                    "http://$gatewayIp:${BuildConfig.LOCALHOST_PORT}/"
                } else {
                    // Fallback: IP típica de hotspot Windows (solo si falla detección)
                    Log.w(TAG, "⚠️ No se pudo detectar gateway, usando fallback 192.168.137.1")
                    "http://192.168.137.1:${BuildConfig.LOCALHOST_PORT}/"
                }
                Log.i(TAG, "📲 DISPOSITIVO FÍSICO → $url")
                url
            }
        }

        /**
         * 🤖 Detecta si estamos en un emulador de Android
         */
        private fun isEmulator(): Boolean {
            return (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || "google_sdk" == Build.PRODUCT)
        }

        /**
         * 🌐 Obtiene la IP del GATEWAY (router/hotspot) en la red WiFi
         * Esto detecta automáticamente:
         * - Hotspot Windows: 192.168.137.1
         * - Router WiFi casa: 192.168.1.1 (típico)
         * - Cualquier otro router: La IP del gateway DHCP
         */
        private fun getGatewayIp(context: Context): String? {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    ?: return null

                val dhcpInfo = wifiManager.dhcpInfo ?: return null
                val gatewayInt = dhcpInfo.gateway

                if (gatewayInt == 0) {
                    Log.w(TAG, "⚠️ Gateway IP es 0 (sin conexión WiFi?)")
                    return null
                }

                // Convertir int a IP formato string (192.168.137.1)
                val gatewayIp = String.format(
                    "%d.%d.%d.%d",
                    (gatewayInt and 0xff),
                    (gatewayInt shr 8 and 0xff),
                    (gatewayInt shr 16 and 0xff),
                    (gatewayInt shr 24 and 0xff)
                )

                Log.i(TAG, "✅ Gateway detectado: $gatewayIp")
                return gatewayIp
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo gateway: ${e.message}")
                return null
            }
        }

        fun getInstance(context: Context): ApiService {
            // Guardar el contexto de aplicación para usarlo en getBaseUrl()
            if (appContext == null) {
                appContext = context.applicationContext
            }
            
            // 🔄 Detectar si cambió la URL base (cambio de red)
            val currentBaseUrl = getBaseUrl()
            if (lastBaseUrl != null && lastBaseUrl != currentBaseUrl) {
                Log.w(TAG, "🔄 Cambio de red detectado: $lastBaseUrl → $currentBaseUrl")
                INSTANCE = null // Invalidar instancia anterior
            }
            lastBaseUrl = currentBaseUrl
            
            return INSTANCE ?: synchronized(this) {
                val instance = buildRetrofit(context, currentBaseUrl)
                INSTANCE = instance
                instance
            }
        }

        private fun buildRetrofit(context: Context, baseUrl: String): ApiService {
            val tokenManager = TokenManager(context)
            val authInterceptor = AuthInterceptor(tokenManager, context)

            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { 
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                })
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}