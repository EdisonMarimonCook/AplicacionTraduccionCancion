from fastapi import FastAPI
import asyncio
from spotify import get_top10_playlist
from lyrics import get_lyrics
from cache import top10_cache

app = FastAPI()

# Endpoint raíz para comprobar que el servidor funciona
@app.get("/")
def root():
    return {"message": "Servidor funcionando"}

# Endpoint para obtener letra y traducción de una canción
@app.get("/api/songs/{song}/lyrics")
def lyrics_endpoint(song: str, level: str = "default"):
    return get_lyrics(song, level)

# Endpoint para obtener top10 de canciones
@app.get("/api/songs/top10")
def top10_endpoint(language: str = "en"):
    """
    Devuelve el top10 de canciones por idioma.
    Usa el cache que se llena al iniciar la app y se actualiza cada hora.
    """
    return {"top10": top10_cache}

# Endpoint para obtener top10 de canciones según el idioma
@app.get("/api/songs/top10/{language}")
def top10_by_language(language: str):
    """
    Devuelve el top10 de canciones según el idioma especificado.
    """
    top10_songs = get_top10_playlist(language)
    return {
        "language": language,
        "top10": top10_songs
    }

# Evento al iniciar la app: llenamos cache y lanzamos actualización cada hora
@app.on_event("startup")
async def startup_event():
    # Llenamos el cache inicialmente
    top10_cache.clear()
    top10_cache.extend(get_top10_playlist())  # por defecto "en"

    # Función que actualiza el cache cada hora en segundo plano
    async def actualizar_top10():
        while True:
            top10_cache.clear()
            top10_cache.extend(get_top10_playlist())
            await asyncio.sleep(3600)

    asyncio.create_task(actualizar_top10())
