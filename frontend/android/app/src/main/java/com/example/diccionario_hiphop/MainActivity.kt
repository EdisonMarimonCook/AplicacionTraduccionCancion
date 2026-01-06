package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.diccionario_hiphop.ui.ConnectivityBanner
import com.example.diccionario_hiphop.utils.NetworkMonitor
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.diccionario_hiphop.utils.WindowInsetsHelper
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var connectivityBanner: ConnectivityBanner
    private lateinit var networkMonitor: NetworkMonitor
    
    // 🚀 Repository para precarga de perfil
    private val profileRepository by lazy { ProfileRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1️⃣ Verificar sesión ANTES de crear la UI
        val tokenManager = TokenManager(this)
        if (!tokenManager.hasActiveSession()) {
            android.util.Log.w("MainActivity", "⚠️ No hay sesión activa, redirigiendo a login")
            val intent = Intent(this, SplashActivity::class.java) // O LoginActivity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        // 2️⃣ Inicializar UI inmediatamente
        initializeMainActivity()
        
        // 3️⃣ Verificar onboarding en background (sin bloquear UI)
        checkOnboardingStatus()
    }
    
    private fun setupNetworkMonitoring() {
        networkMonitor = NetworkMonitor.getInstance(this)
        
        lifecycleScope.launch {
            networkMonitor.isConnected
                .collect { isConnected ->
                    android.util.Log.d("MainActivity", "🌐 Conectividad cambió: ${if (isConnected) "ONLINE" else "OFFLINE"}")
                    
                    // Actualizar banner directamente sin runOnUiThread (ya estamos en Main)
                    if (isConnected) {
                        connectivityBanner.setOnline()
                    } else {
                        connectivityBanner.setOffline()
                    }
                }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::networkMonitor.isInitialized) {
            networkMonitor.unregister()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::viewPager.isInitialized) {
            outState.putInt("current_page", viewPager.currentItem)
        }
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        
        // Siempre iniciar en HomeFragment (posición 1)
        // Siempre iniciar en HomeFragment (posición 1)
        viewPager.currentItem = 1
        bottomNav.selectedItemId = R.id.nav_home

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> bottomNav.selectedItemId = R.id.nav_grammys
                    1 -> bottomNav.selectedItemId = R.id.nav_home
                    2 -> bottomNav.selectedItemId = R.id.nav_profile
                }
            }
        })
    }
    
    private fun checkOnboardingStatus() {
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@MainActivity)
                val response = api.getCurrentUser()
                
                if (response.isSuccessful) {
                    val user = response.body()
                    // Verificar si el onboarding NO está completado
                    val needsOnboarding = user?.onboardingCompleted == false
                    if (needsOnboarding) {
                        // Usuario nuevo, mostrar onboarding
                        android.util.Log.d("MainActivity", "🎓 Primera vez del usuario, mostrando onboarding")
                        startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                        finish()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error verificando onboarding: ${e.message}")
                // En caso de error, continuar normal (ya se inicializó la UI)
            }
        }
    }
    
    private fun initializeMainActivity() {
        setContentView(R.layout.activity_main)

        // Initialize yt-dlp
        try {
            com.yausername.youtubedl_android.YoutubeDL.getInstance().init(applicationContext)
            android.util.Log.d("MainActivity", "✅ yt-dlp inicializado correctamente")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error inicializando yt-dlp: ${e.message}")
        }

        // Initialize views
        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)
        connectivityBanner = findViewById(R.id.connectivityBanner)

        // 🔧 Aplicar insets solo a la barra inferior
        WindowInsetsHelper.applyBottomInsets(bottomNav)

        // Setup ViewPager
        viewPager.adapter = MainPagerAdapter(this)
        viewPager.isUserInputEnabled = true  // ✅ Permitir deslizar entre fragmentos
        viewPager.offscreenPageLimit = 3

        // Sincronizar ViewPager con BottomNav
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNav.menu.getItem(position).isChecked = true
            }
        })

        setupBottomNav()

        // Ir a home por defecto - DESPUÉS de configurar todo
        viewPager.post {
            viewPager.setCurrentItem(1, false)
            bottomNav.selectedItemId = R.id.nav_home
        }
        
        // 🚀 Precarga paralela de datos críticos (Profile + Home)
        preloadFragmentData()

        // NetworkMonitor con estado inicial
        networkMonitor = NetworkMonitor.getInstance(this)
        setupNetworkMonitoring()
        
        // Establecer estado inicial del banner
        if (networkMonitor.isConnected.value) {
            connectivityBanner.setOnline()
        } else {
            connectivityBanner.setOffline()
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_grammys -> viewPager.currentItem = 0
                R.id.nav_home -> viewPager.currentItem = 1
                R.id.nav_profile -> viewPager.currentItem = 2
            }
            true
        }
    }
    
    /**
     * 🚀 Precarga de datos del perfil
     * Carga el perfil al iniciar la app para que esté disponible en caché
     * Esto mejora la velocidad percibida cuando el usuario navega al perfil o home
     */
    private fun preloadFragmentData() {
        lifecycleScope.launch {
            try {
                // Precargar perfil en background
                profileRepository.getProfile()
                android.util.Log.d("MainActivity", "✅ Precarga de perfil completada")
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "⚠️ Error en precarga: ${e.message}")
                // No mostrar error al usuario, los fragments manejarán sus propias cargas
            }
        }
    }

    private inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GrammysFragment()
                1 -> HomeFragment()
                2 -> ProfileFragment()
                else -> HomeFragment()
            }
        }
    }
}