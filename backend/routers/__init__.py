"""
📦 Paquete: Routers

Contiene todos los endpoints (rutas) organizados por funcionalidad:

- auth.py       → Autenticación (login, registro)
- songs.py      → Canciones (listar, detalles)
- lyrics.py     → Letras de canciones
- dictionary.py → Diccionario personal del usuario
- progress.py   → Progreso y estadísticas del usuario
- users.py      → Perfil del usuario

VENTAJA: Centraliza todos los routers para importarlos fácilmente en main.py
"""
"""
📦 Paquete: Routers
Centraliza las importaciones.
"""

# Importar todos los routers
from routers.auth import router as auth_router
from routers.songs import router as songs_router
from routers.lyrics import router as lyrics_router
from routers.ai_analysis import router as ai_router

# ✅ DESCOMENTADOS Y ACTIVADOS:
from routers.dictionary import router as dictionary_router
from routers.users import router as users_router
from routers.progress import router as progress_router

__all__ = [
    "auth_router",
    "songs_router",
    "lyrics_router",
    "ai_router",
    "dictionary_router",
    "users_router",
    "progress_router"
]
