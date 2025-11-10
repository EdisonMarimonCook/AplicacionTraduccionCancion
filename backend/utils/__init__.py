"""
🛠️ Paquete: Utilidades

Contiene herramientas y clientes reutilizables:

- genius_client.py   → Cliente para extraer letras de Genius API
- spotify_client.py  → Cliente Spotify (en el futuro)
- ai_analyzer.py     → Análisis con OpenAI (en el futuro)

VENTAJA: Importar desde utils en cualquier router
"""

# Importar funciones principales de genius_client
from utils.genius_client import (
    get_song_lyrics,
    get_song_verses,
    get_line_lyrics,
    is_genius_configured
)

# Exportar para que otros archivos puedan hacer:
# from utils import get_song_lyrics, get_line_lyrics, etc.
__all__ = [
    "get_song_lyrics",
    "get_song_verses", 
    "get_line_lyrics",
    "is_genius_configured"
]