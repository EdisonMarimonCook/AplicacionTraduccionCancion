package com.example.diccionario_hiphop.utils

import android.graphics.Color

/**
 * Utilidad para mapear niveles de idioma a colores e información visual
 */
object LevelIndicator {
    
    data class LevelInfo(
        val color: Int,
        val description: String,
        val difficulty: String,
        val progress: Int  // De 1 a 6 para barra de progreso
    )
    
    /**
     * Obtiene información visual del nivel según el sistema (CEFR, JLPT, HSK, TOPIK)
     */
    fun getLevelInfo(level: String, languageCode: String): LevelInfo {
        return when (languageCode) {
            "ja" -> getJLPTInfo(level)
            "zh" -> getHSKInfo(level)
            "ko" -> getTOPIKInfo(level)
            else -> getCEFRInfo(level)
        }
    }
    
    private fun getCEFRInfo(level: String): LevelInfo {
        return when (level) {
            "A1" -> LevelInfo(
                color = Color.parseColor("#4CAF50"),  // Verde
                description = "Principiante",
                difficulty = "Muy fácil",
                progress = 1
            )
            "A2" -> LevelInfo(
                color = Color.parseColor("#8BC34A"),  // Verde claro
                description = "Elemental",
                difficulty = "Fácil",
                progress = 2
            )
            "B1" -> LevelInfo(
                color = Color.parseColor("#FDD835"),  // Amarillo
                description = "Intermedio",
                difficulty = "Medio",
                progress = 3
            )
            "B2" -> LevelInfo(
                color = Color.parseColor("#FFB300"),  // Naranja claro
                description = "Intermedio-Alto",
                difficulty = "Medio-Alto",
                progress = 4
            )
            "C1" -> LevelInfo(
                color = Color.parseColor("#FF6F00"),  // Naranja
                description = "Avanzado",
                difficulty = "Difícil",
                progress = 5
            )
            "C2" -> LevelInfo(
                color = Color.parseColor("#D32F2F"),  // Rojo
                description = "Maestría",
                difficulty = "Muy difícil",
                progress = 6
            )
            else -> LevelInfo(
                color = Color.parseColor("#9E9E9E"),
                description = "Desconocido",
                difficulty = "",
                progress = 0
            )
        }
    }
    
    private fun getJLPTInfo(level: String): LevelInfo {
        return when (level) {
            "N5" -> LevelInfo(
                color = Color.parseColor("#4CAF50"),
                description = "Principiante",
                difficulty = "最も易しい (Más fácil)",
                progress = 1
            )
            "N4" -> LevelInfo(
                color = Color.parseColor("#8BC34A"),
                description = "Básico",
                difficulty = "易しい (Fácil)",
                progress = 2
            )
            "N3" -> LevelInfo(
                color = Color.parseColor("#FDD835"),
                description = "Intermedio",
                difficulty = "普通 (Normal)",
                progress = 3
            )
            "N2" -> LevelInfo(
                color = Color.parseColor("#FF6F00"),
                description = "Avanzado",
                difficulty = "難しい (Difícil)",
                progress = 5
            )
            "N1" -> LevelInfo(
                color = Color.parseColor("#D32F2F"),
                description = "Experto",
                difficulty = "最も難しい (Más difícil)",
                progress = 6
            )
            else -> LevelInfo(
                color = Color.parseColor("#9E9E9E"),
                description = "Desconocido",
                difficulty = "",
                progress = 0
            )
        }
    }
    
    private fun getHSKInfo(level: String): LevelInfo {
        val levelInt = level.toIntOrNull() ?: 0
        return when (levelInt) {
            1 -> LevelInfo(
                color = Color.parseColor("#4CAF50"),
                description = "Principiante",
                difficulty = "初级 (Básico)",
                progress = 1
            )
            2 -> LevelInfo(
                color = Color.parseColor("#8BC34A"),
                description = "Básico",
                difficulty = "基础 (Fundamental)",
                progress = 2
            )
            3 -> LevelInfo(
                color = Color.parseColor("#FDD835"),
                description = "Intermedio",
                difficulty = "中级 (Intermedio)",
                progress = 3
            )
            4 -> LevelInfo(
                color = Color.parseColor("#FFB300"),
                description = "Intermedio-Alto",
                difficulty = "中高级 (Intermedio-Alto)",
                progress = 4
            )
            5 -> LevelInfo(
                color = Color.parseColor("#FF6F00"),
                description = "Avanzado",
                difficulty = "高级 (Avanzado)",
                progress = 5
            )
            6 -> LevelInfo(
                color = Color.parseColor("#D32F2F"),
                description = "Experto",
                difficulty = "精通 (Maestría)",
                progress = 6
            )
            else -> LevelInfo(
                color = Color.parseColor("#9E9E9E"),
                description = "Desconocido",
                difficulty = "",
                progress = 0
            )
        }
    }
    
    private fun getTOPIKInfo(level: String): LevelInfo {
        val levelInt = level.toIntOrNull() ?: 0
        return when (levelInt) {
            1 -> LevelInfo(
                color = Color.parseColor("#4CAF50"),
                description = "Principiante",
                difficulty = "초급 (Principiante)",
                progress = 1
            )
            2 -> LevelInfo(
                color = Color.parseColor("#8BC34A"),
                description = "Básico",
                difficulty = "초급 상 (Básico Alto)",
                progress = 2
            )
            3 -> LevelInfo(
                color = Color.parseColor("#FDD835"),
                description = "Intermedio",
                difficulty = "중급 (Intermedio)",
                progress = 3
            )
            4 -> LevelInfo(
                color = Color.parseColor("#FFB300"),
                description = "Intermedio-Alto",
                difficulty = "중급 상 (Intermedio Alto)",
                progress = 4
            )
            5 -> LevelInfo(
                color = Color.parseColor("#FF6F00"),
                description = "Avanzado",
                difficulty = "고급 (Avanzado)",
                progress = 5
            )
            6 -> LevelInfo(
                color = Color.parseColor("#D32F2F"),
                description = "Experto",
                difficulty = "고급 상 (Experto)",
                progress = 6
            )
            else -> LevelInfo(
                color = Color.parseColor("#9E9E9E"),
                description = "Desconocido",
                difficulty = "",
                progress = 0
            )
        }
    }
}
