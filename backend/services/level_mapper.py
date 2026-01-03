"""
ARCHIVO: services/level_mapper.py
PROPÓSITO: Mapear niveles entre sistemas diferentes (CEFR, JLPT, HSK, TOPIK)
"""

# Sistemas de niveles por idioma (SOPORTADOS POR GEMINI)
LEVEL_SYSTEMS = {
    # ✅ CEFR (Europeos)
    "en": {"system": "CEFR", "levels": ["A1", "A2", "B1", "B2", "C1", "C2"]},
    "es": {"system": "CEFR", "levels": ["A1", "A2", "B1", "B2", "C1", "C2"]},
    "fr": {"system": "CEFR", "levels": ["A1", "A2", "B1", "B2", "C1", "C2"]},
    "de": {"system": "CEFR", "levels": ["A1", "A2", "B1", "B2", "C1", "C2"]},
    "it": {"system": "CEFR", "levels": ["A1", "A2", "B1", "B2", "C1", "C2"]},
    "pt": {"system": "CEFR", "levels": ["A1", "A2", "B1", "B2", "C1", "C2"]},
    
    # ✅ Sistemas Asiáticos
    "ja": {"system": "JLPT", "levels": ["N5", "N4", "N3", "N2", "N1"]},
    "zh": {"system": "HSK", "levels": ["1", "2", "3", "4", "5", "6"]},
    "ko": {"system": "TOPIK", "levels": ["1", "2", "3", "4", "5", "6"]},
}


def get_level_system(language_code: str) -> dict:
    """Obtiene el sistema de niveles para un idioma"""
    return LEVEL_SYSTEMS.get(language_code, LEVEL_SYSTEMS["en"])


def validate_level_for_system(level: str, system: str) -> bool:
    """Valida que un nivel existe en un sistema"""
    valid_levels = {
        "CEFR": ["A1", "A2", "B1", "B2", "C1", "C2"],
        "JLPT": ["N5", "N4", "N3", "N2", "N1"],
        "HSK": ["1", "2", "3", "4", "5", "6"],
        "TOPIK": ["1", "2", "3", "4", "5", "6"],
    }
    
    levels = valid_levels.get(system, [])
    return level in levels


def normalize_to_cefr_scale(level: str, system: str) -> int:
    """Convierte cualquier nivel a escala CEFR (1-6) para análisis interno"""
    equivalence = {
        "CEFR": {"A1": 1, "A2": 2, "B1": 3, "B2": 4, "C1": 5, "C2": 6},
        "JLPT": {"N5": 1, "N4": 2, "N3": 3, "N2": 4, "N1": 5},
        "HSK": {"1": 1, "2": 2, "3": 3, "4": 4, "5": 5, "6": 6},
        "TOPIK": {"1": 1, "2": 2, "3": 3, "4": 4, "5": 5, "6": 6},
    }
    
    system_map = equivalence.get(system, {})
    return system_map.get(level, 3)  # Default B1


def convert_cefr_scale_to_system(cefr_value: int, system: str) -> str:
    """Convierte de escala CEFR (1-6) al sistema específico"""
    reverse_map = {
        "CEFR": {1: "A1", 2: "A2", 3: "B1", 4: "B2", 5: "C1", 6: "C2"},
        "JLPT": {1: "N5", 2: "N4", 3: "N3", 4: "N2", 5: "N1", 6: "N1"},
        "HSK": {1: "1", 2: "2", 3: "3", 4: "4", 5: "5", 6: "6"},
        "TOPIK": {1: "1", 2: "2", 3: "3", 4: "4", 5: "5", 6: "6"},
    }
    
    system_reverse = reverse_map.get(system, {})
    return system_reverse.get(cefr_value, "A1" if system == "CEFR" else "1")