package com.example.diccionario_hiphop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)

        setupViewPager()
        setupBottomNav()
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        
        // Empezar en la página del CENTRO (Descubrir/Home)
        viewPager.currentItem = 1 
        bottomNav.selectedItemId = R.id.nav_home

        // Sincronizar Swipe -> Menú
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
        // Sincronizar Menú -> Swipe
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_grammys -> viewPager.currentItem = 0
                R.id.nav_home -> viewPager.currentItem = 1
                R.id.nav_profile -> viewPager.currentItem = 2
            }
            true
        }
    }

    // Adaptador para las 3 pantallas
    private inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GrammysFragment() // Izquierda
                1 -> HomeFragment()    // Centro
                2 -> ProfileFragment() // Derecha
                else -> HomeFragment()
            }
        }
    }
}