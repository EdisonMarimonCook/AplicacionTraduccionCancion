from fastapi import FastAPI
import asyncio
from spotify import get_top10_playlist
from lyrics import get_lyrics
from cache import top10_cache
import logging

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
    
    Args:
        language (str): Código del idioma ('en', 'es', 'fr', etc.)
        
    Returns:
        dict: Top 10 de canciones del idioma especificado
    """
    # Obtener canciones del cache, retornar lista vacía si no existen
    songs = top10_cache.get(language, [])
    
    if not songs:
        logging.warning(f"No hay canciones en cache para idioma: {language}")
    
    return {"language": language, "top10": songs}

# Endpoint para obtener top10 de canciones según el idioma
@app.get("/api/songs/top10/{language}")
def top10_by_language(language: str):
    """
    Devuelve el top10 de canciones según el idioma especificado.
    """
    songs = top10_cache.get(language)
    if not songs:
        # Opcional: devolver un error 404
        return {"error": "Language not supported or cache not ready"}, 404
    return {
        "language": language,
        "top10": songs
    }

# Evento al iniciar la app: llenamos cache y lanzamos actualización cada hora
@app.on_event("startup")
async def startup_event():
    # Lista de idiomas que soportas
    SUPPORTED_LANGUAGES = ["en", "es", "fr", "de", "it", "pt", "jp"]

    def update_full_cache():
        logging.info("Actualizando todo el cache de Top 10...")
        for lang in SUPPORTED_LANGUAGES:
            top10_cache[lang] = get_top10_playlist(lang)

    # Llenamos el cache inicialmente
    update_full_cache()

    # Función que actualiza el cache cada hora en segundo plano
    async def actualizar_top10():
        while True:
            await asyncio.sleep(3600)
            update_full_cache()

    asyncio.create_task(actualizar_top10())
