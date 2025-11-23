"""
📦 Paquete: Routers

Contiene todos los endpoints (rutas) organizados por funcionalidad:

- auth.py       → Autenticación (login, registro)
- songs.py      → Canciones (listar, detalles)
- lyrics.py     → Letras de canciones
- dictionary.py → Diccionario personal del usuario
- progress.py   → Progreso y estadísticas del usuario
- users.py      → Perfil del usuario
- openai.py     → Interacción con OpenAI

VENTAJA: Centraliza todos los routers para importarlos fácilmente en main.py
"""

# Importar todos los routers
from routers.auth import router as auth_router
from routers.songs import router as songs_router
from routers.lyrics import router as lyrics_router 
from routers.openai import router as openai_router 

# Nota: Importaremos los demás conforme los creemos
# from routers.dictionary import router as dictionary_router
# from routers.progress import router as progress_router
# from routers.users import router as users_router

# Exportar para que main.py pueda hacer:
# from routers import auth_router, songs_router, lyrics_router, etc.
__all__ = [
    "auth_router",
    "songs_router",
    "lyrics_router",
    "openai_router",
    # "dictionary_router",
    # "progress_router",
    # "users_router",
]