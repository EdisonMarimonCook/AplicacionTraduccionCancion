"""
MÓDULO: Cache
PROPÓSITO: Almacenar datos en memoria (rápido acceso)

¿QUÉ ES?
└─ Diccionario que guarda las Top 10 canciones por idioma
└─ Se actualiza cada hora automáticamente desde main.py
└─ Evita hacer requests innecesarios a Spotify

ESTRUCTURA:
top10_cache = {
    "en": [canción1, canción2, ...],
    "es": [canción1, canción2, ...],
    "fr": [canción1, canción2, ...],
    ...
}
"""

from typing import List, Dict, Optional

# ===============================================================================
# CACHE GLOBAL
# ===============================================================================

# Diccionario que almacena Top 10 canciones por idioma
# Estructura: {"en": [...], "es": [...], "fr": [...], etc}
top10_cache: Dict[str, List[Dict]] = {}

# ===============================================================================
# FUNCIONES AUXILIARES
# ===============================================================================

def get_cached_songs(language: Optional[str] = None) -> List[Dict]:
    """
    📚 OBTIENE TODAS LAS CANCIONES DEL CACHE
    
    LÓGICA:
    1. Si se especifica idioma, retorna canciones de ese idioma
    2. Si NO se especifica, retorna TODAS las canciones de TODOS los idiomas
    3. Si no hay cache, retorna lista vacía
    
    PARÁMETROS:
    - language (opcional): Código de idioma (ej: "en", "es", "fr")
    
    RETORNA:
    - Lista de canciones (dicts)
    
    EJEMPLOS:
    get_cached_songs()           # Todas las canciones de todos idiomas
    get_cached_songs("en")       # Solo canciones en inglés
    get_cached_songs("es")       # Solo canciones en español
    """
    
    # Si se especifica idioma, retorna solo de ese idioma
    if language:
        return top10_cache.get(language, [])
    
    # Si NO se especifica, retorna TODAS las canciones
    all_songs = []
    for songs_list in top10_cache.values():
        all_songs.extend(songs_list)
    
    return all_songs

# ===============================================================================

def get_song_from_cache(song_id: str) -> Optional[Dict]:
    """
    🔍 BUSCA UNA CANCIÓN ESPECÍFICA EN EL CACHE
    
    LÓGICA:
    1. Recorre todos los idiomas en el cache
    2. Busca por ID (spotify_id o id)
    3. Si la encuentra, retorna la canción
    4. Si no la encuentra, retorna None
    
    PARÁMETROS:
    - song_id: ID de la canción a buscar
    
    RETORNA:
    - Dict con info de la canción O None si no existe
    
    EJEMPLO:
    song = get_song_from_cache("spotify-123")
    if song:
        print(f"Encontrada: {song['name']}")
    else:
        print("No encontrada")
    """
    
    # Recorrer todos los idiomas
    for songs_list in top10_cache.values():
        # Buscar en cada canción
        for song in songs_list:
            # Comparar por ID o spotify_id
            if song.get("id") == song_id or song.get("spotify_id") == song_id:
                return song
    
    # No encontrada
    return None

# ===============================================================================

def get_cached_songs_by_language(language: str) -> List[Dict]:
    """
    🌍 OBTIENE CANCIONES DE UN IDIOMA ESPECÍFICO
    
    LÓGICA:
    1. Recibe código de idioma
    2. Busca en el cache
    3. Retorna canciones de ese idioma
    
    PARÁMETROS:
    - language: Código ISO (ej: "en", "es", "fr")
    
    RETORNA:
    - Lista de canciones del idioma (o lista vacía si no existe)
    
    EJEMPLO:
    spanish_songs = get_cached_songs_by_language("es")
    """
    
    return top10_cache.get(language.lower(), [])

# ===============================================================================

def clear_cache():
    """
    🗑️ LIMPIA TODO EL CACHE
    
    ¿PARA QUÉ?
    └─ Forzar actualización completa
    └─ Liberar memoria si es necesario
    
    EJEMPLO:
    clear_cache()
    # Ahora top10_cache está vacío
    """
    
    global top10_cache
    top10_cache.clear()

# ===============================================================================

def get_cache_status() -> Dict:
    """
    📊 OBTIENE ESTADÍSTICAS DEL CACHE
    
    RETORNA:
    - Dict con información del cache:
        * total_languages: Número de idiomas
        * total_songs: Número total de canciones
        * languages: Lista de idiomas
        * songs_per_language: Número de canciones por idioma
    
    EJEMPLO:
    status = get_cache_status()
    print(f"Cache tiene {status['total_songs']} canciones")
    """
    
    total_languages = len(top10_cache)
    total_songs = sum(len(songs) for songs in top10_cache.values())
    
    songs_per_language = {
        lang: len(songs)
        for lang, songs in top10_cache.items()
    }
    
    return {
        "total_languages": total_languages,
        "total_songs": total_songs,
        "languages": list(top10_cache.keys()),
        "songs_per_language": songs_per_language
    }
