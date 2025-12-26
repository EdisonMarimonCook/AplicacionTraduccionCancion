from fastapi import APIRouter, HTTPException, Depends
from typing import Optional
import logging
import asyncio

from models import User
from routers.auth import get_current_user
from routers.schemas import LyricsResponse
from utils.genius_client import get_song_lyrics
from utils.spotify import enrich_single_song 
from utils.audio_extractor import get_youtube_audio_url

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/lyrics", tags=["Lyrics"])

@router.get("/", response_model=LyricsResponse)
async def get_lyrics(
    title: str,
    artist: str,
    current_user: User = Depends(get_current_user)
):
    """
    📄 Obtiene letra, metadatos y busca enlaces de audio (Preview + Full).
    """
    try:
        logger.info(f"📄 Procesando canción: {title} - {artist}")
        
        # 1. Obtener Letras (Genius/LRCLIB)
        lyrics_data = await get_song_lyrics(title, artist)
        
        if not lyrics_data:
            raise HTTPException(status_code=404, detail="Lyrics not found")

        # 2. Solo buscar Preview en iTunes/Spotify (Rápido)
        song_meta = await enrich_single_song(title, artist)

        # 3. Construir Respuesta SOLO con preview
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "lyrics": lyrics_data.get("lyrics"),
            # Imagen: Prioridad Genius > Spotify
            "image_url": lyrics_data.get("image_url") or song_meta.get("image_url"),
            "genius_url": lyrics_data.get("url"),
            # 🎵 SOLO PREVIEW
            "preview_url": song_meta.get("preview_url")
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error getting lyrics/audio: {e}")
        raise HTTPException(status_code=500, detail="Internal server error")