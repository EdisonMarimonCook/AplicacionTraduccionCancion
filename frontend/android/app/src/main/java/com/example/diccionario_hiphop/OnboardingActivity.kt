package com.example.diccionario_hiphop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.diccionario_hiphop.utils.WindowInsetsHelper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView
    private lateinit var btnBack: ImageButton
    
    private val pages = listOf(
        OnboardingPage(
            "🎵",
            "Aprende idiomas con música",
            "Descubre palabras y expresiones reales mientras disfrutas de tus canciones favoritas"
        ),
        OnboardingPage(
            "📖",
            "Diccionario personalizado",
            "Guarda palabras interesantes con su contexto musical y vuelve a ellas cuando quieras"
        ),
        OnboardingPage(
            "🎴",
            "Flashcards inteligentes",
            "Practica vocabulario con tarjetas que se adaptan a tu nivel de aprendizaje"
        ),
        OnboardingPage(
            "🎧",
            "Audio nativo de canciones",
            "Escucha la pronunciación correcta extraída directamente de la canción original"
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        initViews()
        setupViewPager()
        setupButtons()
    }
    
    private fun initViews() {
        viewPager = findViewById(R.id.viewPagerOnboarding)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
        btnBack = findViewById(R.id.btnBack)
        
        // ✅ Aplicar insets para ajuste dinámico de botones
        val layoutButtons = findViewById<View>(R.id.layoutButtons)
        WindowInsetsHelper.applyBottomInsets(layoutButtons)
        WindowInsetsHelper.applyTopInsets(btnSkip)
    }
    
    private fun setupViewPager() {
        val adapter = OnboardingAdapter(pages)
        viewPager.adapter = adapter
        
        // Vincular TabLayout con ViewPager
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
        
        // Listener para cambiar botones según la página
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
                updateProgressBar(position)
            }
        })
    }
    
    private fun setupButtons() {
        btnNext.setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }
        
        btnSkip.setOnClickListener {
            finishOnboarding()
        }
        
        btnBack.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }
    }
    
    private fun updateButtons(position: Int) {
        // Botón Back
        btnBack.isEnabled = position > 0
        btnBack.alpha = if (position > 0) 1f else 0.3f
        
        // Botón Next/Empezar
        if (position == pages.size - 1) {
            btnNext.text = "¡Empezar!"
            btnSkip.visibility = View.GONE
        } else {
            btnNext.text = "Siguiente"
            btnSkip.visibility = View.VISIBLE
        }
    }
    
    private fun updateProgressBar(position: Int) {
        val progress1 = findViewById<View>(R.id.progress1)
        val progress2 = findViewById<View>(R.id.progress2)
        val progress3 = findViewById<View>(R.id.progress3)
        val progress4 = findViewById<View>(R.id.progress4)
        
        val activeColor = android.graphics.Color.WHITE
        val inactiveColor = android.graphics.Color.parseColor("#4DFFFFFF")
        
        progress1.setBackgroundColor(if (position >= 0) activeColor else inactiveColor)
        progress2.setBackgroundColor(if (position >= 1) activeColor else inactiveColor)
        progress3.setBackgroundColor(if (position >= 2) activeColor else inactiveColor)
        progress4.setBackgroundColor(if (position >= 3) activeColor else inactiveColor)
    }
    
    private fun finishOnboarding() {
        // Marcar como completado en el servidor
        lifecycleScope.launch {
            try {
                val api = RetrofitService.getInstance(this@OnboardingActivity)
                val response = api.completeOnboarding()
                
                if (response.isSuccessful) {
                    android.util.Log.d("OnboardingActivity", "✅ Onboarding marcado como completado")
                } else {
                    android.util.Log.w("OnboardingActivity", "⚠️ Error al marcar onboarding: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("OnboardingActivity", "❌ Error: ${e.message}")
            }
            
            // Ir a MainActivity de todos modos
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            finish()
        }
    }
}

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)
