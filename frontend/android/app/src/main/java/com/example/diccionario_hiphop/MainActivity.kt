package com.example.diccionario_hiphop

import android.content.Intent  // 🔥 AÑADIR ESTE
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private var currentPage: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔥 Verificar sesión ANTES de crear la UI
        val tokenManager = TokenManager(this)
        if (!tokenManager.hasActiveSession()) {
            android.util.Log.w("MainActivity", "⚠️ No hay sesión activa, redirigiendo a login")
            val intent = Intent(this, SplashActivity::class.java) // O LoginActivity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)
        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Restaurar página guardada
        currentPage = savedInstanceState?.getInt("current_page", 1) ?: 1

        setupViewPager()
        setupBottomNav()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_page", viewPager.currentItem)
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        
        viewPager.currentItem = currentPage
        bottomNav.selectedItemId = when (currentPage) {
            0 -> R.id.nav_grammys
            1 -> R.id.nav_home
            2 -> R.id.nav_profile
            else -> R.id.nav_home
        }

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