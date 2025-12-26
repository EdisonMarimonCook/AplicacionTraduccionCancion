from fastapi import APIRouter, HTTPException, Depends
from typing import Optional
import logging
import asyncio # Para correr requests síncronos sin bloquear

from models import User
from routers.auth import get_current_user
from routers.schemas import LyricsResponse # 👈 Importamos el nuevo schema
from utils.genius_client import get_song_lyrics
from utils.spotify import enrich_single_song 
from utils.audio_extractor import get_youtube_audio_url # 👈 El nuevo extractor

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/lyrics", tags=["Lyrics"])

@router.get("/", response_model=LyricsResponse) # 👈 Usamos el modelo tipado
async def get_lyrics(
    title: str,
    artist: str,
    current_user: User = Depends(get_current_user)
):
    """
    📄 Obtiene letra, metadatos y busca enlaces de audio (Preview y Full).
    """
    try:
        logger.info(f"📄 Procesando canción: {title} - {artist}")
        
        # 1. Obtener Letras (Genius)
        lyrics_data = await get_song_lyrics(title, artist)
        
        if not lyrics_data:
            raise HTTPException(status_code=404, detail="Lyrics not found")

        # 2. Tareas en Paralelo:
        #    a) Buscar Preview en iTunes/Spotify (Rápido)
        #    b) Buscar Audio Completo en Cobalt (Puede tardar 1-2s)
        
        # Preparamos la búsqueda para YouTube ("Linkin Park Numb Audio")
        youtube_query = f"{artist} - {title} audio"

        # Ejecutamos en paralelo para no sumar tiempos
        # enrich_single_song es async, get_youtube_audio_url es sync (lo envolvemos)
        loop = asyncio.get_event_loop()
        
        task_preview = enrich_single_song(title, artist)
        task_full_audio = loop.run_in_executor(None, get_youtube_audio_url, youtube_query)

        # Esperamos a ambos
        song_meta, audio_data = await asyncio.gather(task_preview, task_full_audio)

        # 3. Construir Respuesta
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "lyrics": lyrics_data.get("lyrics"),
            
            # Imagen: Prioridad Genius > Spotify > Nada
            "image_url": lyrics_data.get("image_url") or song_meta.get("image_url"),
            "genius_url": lyrics_data.get("url"),
            
            # 🎵 LOS DOS AUDIOS
            "preview_url": song_meta.get("preview_url"), # iTunes (Respaldo)
            "full_audio_url": audio_data["stream_url"] if audio_data else None # Cobalt (Principal)
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error getting lyrics/audio: {e}")
        raise HTTPException(status_code=500, detail="Internal server error")