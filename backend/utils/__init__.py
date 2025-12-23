"""
🛠️ Paquete: Utilidades

Contiene herramientas y clientes reutilizables:

- genius_client.py   → Cliente para extraer letras de Genius API
- spotify.py  → Cliente Spotify (en el futuro)
- ai_analyzer.py     → Análisis con OpenAI (en el futuro)

VENTAJA: Importar desde utils en cualquier router
"""

# 1. GENIUS (Lyrics Híbridas)
from utils.genius_client import (
    get_song_lyrics,
    search_genius_songs,
    is_genius_configured
)

from utils.spotify import (
        search_songs_spotify,
        get_grammy_songs,
        enrich_single_song,
        get_top_tracks_by_language
    )
# Exportar para que otros archivos puedan hacer:
# from utils import get_song_lyrics, get_line_lyrics, etc.
__all__ = [
    "get_song_lyrics",
    "search_genius_songs",
    "is_genius_configured",
    "search_songs_spotify",
    "get_grammy_songs",
    "enrich_single_song",
    "get_top_tracks_by_language"
]