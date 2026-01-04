package com.example.diccionario_hiphop.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * 🔧 Helper para manejar WindowInsets y edge-to-edge en todos los dispositivos
 * Uso: WindowInsetsHelper.applySystemBarInsets(view)
 */
object WindowInsetsHelper {

    /**
     * Aplica padding automático para barras del sistema (status bar + navigation bar)
     * Útil para layouts raíz que deben respetar las barras del sistema
     */
    fun applySystemBarInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            v.updatePadding(
                top = insets.top,
                bottom = insets.bottom,
                left = insets.left,
                right = insets.right
            )
            
            WindowInsetsCompat.CONSUMED
        }
    }

    /**
     * Aplica padding solo para la barra de navegación inferior
     * Útil para botones que deben estar por encima de la barra de navegación
     */
    fun applyBottomInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            v.updatePadding(bottom = insets.bottom)
            
            windowInsets
        }
    }

    /**
     * Aplica padding solo para la status bar superior
     * Útil para toolbars o headers
     */
    fun applyTopInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            
            v.updatePadding(top = insets.top)
            
            windowInsets
        }
    }

    /**
     * Aplica margen en lugar de padding
     * Útil cuando el padding afecta al contenido interno
     */
    fun applyBottomInsetsAsMargin(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            val params = v.layoutParams as? ViewGroup.MarginLayoutParams
            params?.bottomMargin = insets.bottom
            v.layoutParams = params
            
            windowInsets
        }
    }

    /**
     * Obtiene la altura de la barra de navegación inferior
     * Útil para ajustes manuales
     */
    fun getNavigationBarHeight(view: View): Int {
        val insets = ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
        return insets?.bottom ?: 0
    }

    /**
     * Obtiene la altura de la status bar superior
     */
    fun getStatusBarHeight(view: View): Int {
        val insets = ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
        return insets?.top ?: 0
    }

    /**
     * Habilita edge-to-edge en una Activity
     * Llamar en onCreate() antes de setContentView()
     */
    fun enableEdgeToEdge(activity: android.app.Activity) {
        activity.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
}
